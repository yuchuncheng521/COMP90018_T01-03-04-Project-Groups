package com.knot.app.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.knot.app.crypto.GroupKeyManager
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import com.knot.app.nearby.NearbyManager
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

/**
 * Loads the weekly prompts / activities assigned to the current user across all their groups.
 */
class ActivitiesRepository(
    private val nearbyManager: NearbyManager? = null
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    suspend fun getActivities(): List<ActivityItem> {
        val uid = auth.currentUser?.uid
        return runCatching {
            val snapshot = firestore.collection("activities")
                .whereEqualTo("assignedTo", uid)
                .get()
                .await()

            snapshot.documents.map { doc ->
                ActivityItem(
                    id = doc.id,
                    groupId = doc.getString("groupId") ?: "",
                    groupName = doc.getString("groupName") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    type = runCatching { ActivityType.valueOf(doc.getString("type") ?: "") }.getOrDefault(ActivityType.WEEKLY_PROMPT),
                    status = runCatching { ActivityStatus.valueOf(doc.getString("status") ?: "") }.getOrDefault(ActivityStatus.PENDING),
                    dueLabel = doc.getString("dueLabel") ?: "",
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                )
            }.sortedByDescending { it.createdAt }
        }.getOrElse { emptyList() }
    }

    suspend fun updateActivityStatus(activityId: String, status: ActivityStatus): Result<Unit> = runCatching {
        firestore.collection("activities").document(activityId).update("status", status.name).await()
    }

    suspend fun getGroupNames(groupIds: List<String>): Map<String, String> {
        val names = linkedMapOf<String, String>()

        groupIds.distinct().forEach { groupId ->
            val snapshot = runCatching {
                firestore.collection("groups")
                    .document(groupId)
                    .get()
                    .await()
            }.getOrNull()

            val name = snapshot
                ?.getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: "Shared group"

            names[groupId] = name
        }

        return names
    }

    /**
     * Persists a locally-triggered P2P activity before its response is saved.
     *
     * Nearby detection itself is local, but once the user chooses to respond we need a
     * real Firestore activity document so the normal response/upload/timeline flow can
     * be reused. This document is assigned to the current user only; the response is
     * still shared to the group via activity_responses.groupId.
     */
    suspend fun createP2pActivity(activity: ActivityItem): Result<ActivityItem> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        if (activity.groupId.isBlank()) error("Missing shared group for P2P activity.")

        val groupSnapshot = firestore.collection("groups")
            .document(activity.groupId)
            .get()
            .await()

        val resolvedGroupName =
            groupSnapshot.getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: activity.groupName.ifBlank { "Shared group" }

        val docRef = firestore.collection("activities").document()

        docRef.set(
            mapOf(
                "groupId" to activity.groupId,
                "groupName" to resolvedGroupName,
                "title" to activity.title,
                "description" to activity.description,
                "type" to ActivityType.P2P_ALERT.name,
                "status" to ActivityStatus.PENDING.name,
                "dueLabel" to "Created just now · Nearby",
                "assignedTo" to uid,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        activity.copy(
            id = docRef.id,
            groupName = resolvedGroupName
        )
    }

    /**
     * Creates a new activity/prompt and assigns it to every member of the group.
     *
     * [getActivities] filters "activities" by a single-valued "assignedTo" field
     * (whereEqualTo, not an array-contains), so a single shared document can't be
     * "assigned to everyone" -- instead this fans out into one document per member,
     * each with its own assignedTo/status, so each person's completion state stays
     * independent (matches how [updateActivityStatus] already works, one doc = one status).
     */
    suspend fun createActivity(
        groupId: String,
        groupName: String,
        title: String,
        description: String,
        memberIds: List<String>
    ): Result<Unit> = runCatching {
        if (memberIds.isEmpty()) error("This group has no members to assign the activity to.")

        val creatorUid = auth.currentUser?.uid
            ?: error("You need to be signed in to create an activity.")

        val batch = firestore.batch()
        val sharedActivityId = UUID.randomUUID().toString()
        memberIds.forEach { memberId ->
            val docRef = firestore.collection("activities").document()
            batch.set(
                docRef,
                mapOf(
                    "groupId" to groupId,
                    "sharedActivityId" to sharedActivityId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "groupName" to groupName,
                    "title" to title,
                    "description" to description,
                    "type" to ActivityType.WEEKLY_PROMPT.name,
                    "status" to ActivityStatus.PENDING.name,
                    "dueLabel" to "New",
                    "assignedTo" to memberId,
                    "createdBy" to creatorUid,
                )
            )
        }
        batch.commit().await()
    }

    /**
     * STEP 1 of 2 -- text first. Writes the response document immediately with just the
     * text (already encrypted by the caller, same as before) and marks the activity
     * completed. Nothing here waits on a photo/video/audio upload, so a big file can no
     * longer hold the text hostage. Returns the new response document's id so the media
     * upload (step 2) can attach to it afterwards.
     *
     * mediaStatus tells readers where the media is: "none", "uploading", "done" or "failed".
     *
     * The write is capped at 5s so a missing connection can't leave the screen spinning.
     * As far as I know Firestore keeps an unconfirmed write queued on the device and sends
     * it later, so a timeout here means "not confirmed yet", not "lost". Real errors
     * (e.g. permission denied) still throw.
     */
    suspend fun saveActivityResponse(
        activityId: String,
        groupId: String,
        text: String,
        hasPhoto: Boolean,
        hasVideo: Boolean,
        hasAudio: Boolean,
        location: String?
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val hasMedia = hasPhoto || hasVideo || hasAudio

        val docRef = firestore.collection("activity_responses").document()

        withTimeoutOrNull(5_000) {
            docRef.set(
                mapOf(
                    "activityId" to activityId,
                    "groupId" to groupId,
                    "userId" to uid,
                    "text" to text,
                    "location" to location,
                    "mediaStatus" to if (hasMedia) "uploading" else "none",
                    // Recorded up front so the Timeline can show an "uploading" placeholder
                    // for the right kind(s) while UploadResponseMediaWorker is still working
                    // -- without this, there'd be no way to tell "a video is coming" from
                    // "nothing was submitted" until the URL actually lands.
                    "hasPhoto" to hasPhoto,
                    "hasVideo" to hasVideo,
                    "hasAudio" to hasAudio,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            // After saving response, mark activity as completed
            updateActivityStatus(activityId, ActivityStatus.COMPLETED).getOrThrow()
        }

        docRef.id
    }

    /**
     * STEP 2 of 2 -- media afterwards, smallest file first, one at a time. Each file's URL
     * is patched onto the response as soon as it finishes, so a small photo shows up
     * without waiting for a big video.
     *
     * Encryption is kept exactly as before: GroupKeyManager.encryptText only handles
     * String, so it's the Storage download URLs that get encrypted, not the file bytes
     * behind them. The actual photo/video/audio content in Storage is still only protected
     * by the Storage security rules, not end-to-end encrypted. If encryption returns null
     * the plain URL is stored, same fallback as the old code.
     *
     * Meant to be called from UploadResponseMediaWorker, not directly from the UI.
     */
    suspend fun uploadResponseMedia(
        context: Context,
        responseId: String,
        groupId: String,
        activityId: String,
        photoPath: String?,
        videoPath: String?,
        audioPath: String?
    ) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")

        val files = listOf("photo" to photoPath, "video" to videoPath, "audio" to audioPath)
            .mapNotNull { (kind, path) -> path?.let { Triple(kind, it, File(it).length()) } }
            .sortedBy { it.third }

        val docRef = firestore.collection("activity_responses").document(responseId)
        for ((kind, path, _) in files) {
            val url = uploadFile(path, groupId, activityId, kind, responseId)
            val stored = GroupKeyManager.encryptText(context, groupId, uid, url) ?: url
            docRef.update("${kind}Url", stored).await()
//            android.util.Log.d("PriorityTest", "$kind ATTACHED ${System.currentTimeMillis()}")

        }
        docRef.update("mediaStatus", "done").await()
    }

    suspend fun markMediaFailed(responseId: String) {
        firestore.collection("activity_responses").document(responseId)
            .update("mediaStatus", "failed")
            .await()
    }

    /**
     * Edits ONLY the text of a response. Re-encrypts it the same way submit does.
     * Media can't be changed after posting -- the Firestore rules enforce that too.
     */
    suspend fun editResponseText(
        context: Context,
        responseId: String,
        groupId: String,
        newText: String
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val trimmed = newText.trim()
        val stored =
            if (trimmed.isNotBlank()) GroupKeyManager.encryptText(context, groupId, uid, trimmed) ?: trimmed
            else trimmed

        firestore.collection("activity_responses").document(responseId)
            .update("text", stored)
            .await()
    }

    /**
     * Deletes a response and its uploaded photo/video/audio. The Firestore document goes
     * first (only the author is allowed to), then the files are removed best-effort.
     * Files are found by name -- uploads are named "<kind>_<responseId>.<ext>" -- so
     * responses uploaded before this change leave their files behind in Storage.
     */
    suspend fun deleteResponse(responseId: String): Result<Unit> = runCatching {
        val docRef = firestore.collection("activity_responses").document(responseId)
        val doc = docRef.get().await()
        val groupId = doc.getString("groupId").orEmpty()
        val activityId = doc.getString("activityId").orEmpty()

        docRef.delete().await()

        if (groupId.isNotBlank() && activityId.isNotBlank()) {
            runCatching {
                storage.reference
                    .child("activity_responses")
                    .child(groupId)
                    .child(activityId)
                    .listAll()
                    .await()
                    .items
                    .filter { it.name.contains(responseId) }
                    .forEach { it.delete().await() }
            }
        }
    }

    private suspend fun uploadFile(
        localPath: String,
        groupId: String,
        activityId: String,
        kind: String,
        responseId: String
    ): String {
        val file = File(localPath)
        val storageRef = storage.reference
            .child("activity_responses")
            .child(groupId)
            .child(activityId)
            .child("${kind}_${responseId}.${file.extension}")

        storageRef.putFile(Uri.fromFile(file)).await()
        return storageRef.downloadUrl.await().toString()
    }
}