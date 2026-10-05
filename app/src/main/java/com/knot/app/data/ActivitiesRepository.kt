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
import java.io.File
import java.util.UUID

/**
 * Loads the weekly prompts / activities assigned to the current user across all their groups.
 *
 * STUB: reads from an "activities" Firestore collection. The real P2P (BLE proximity) alert
 * described in the project plan is a separate on-device signal (Nearby Connections API /
 * a foreground BLE scan service) -- it is NOT fetched from Firestore. For this base build,
 * [getP2pAlert] is a placeholder you can wire up once the BLE scanning service exists.
 * Falls back to [sampleActivities] / [samplePlaceholderAlert] when there's no backend yet.
 */
class ActivitiesRepository(
    private val nearbyManager: NearbyManager? = null
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    suspend fun getActivities(): List<ActivityItem> {
        val uid = auth.currentUser?.uid ?: return sampleActivities
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
                // Newest first. Sorted here in Kotlin rather than with a Firestore
                // .orderBy("createdAt") -- combined with the existing
                // .whereEqualTo("assignedTo", uid) that would need a new composite
                // index, and this app doesn't have any defined yet.
            }.sortedByDescending { it.createdAt }
        }.getOrElse { emptyList() }
    }

    /**
     * TODO(sensors): replace with a real check against the BLE proximity service once it's
     * implemented (see project plan: "Nearby Connections API" + foreground scan service).
     * Returning a non-null value here is what makes the placeholder banner show up.
     */
    suspend fun getP2pAlert(): ActivityItem? {
        val connectedMember =
            nearbyManager?.connectedMembers?.value?.firstOrNull()
                ?: return null

        return ActivityItem(
            id = "p2p-${connectedMember.endpointId}",
            groupId = connectedMember.sharedGroupId.orEmpty(),
            groupName = "Shared group",
            title = "${connectedMember.endpointName} is nearby. Capture a memory together?",
            description = "You're both here right now. Take a photo to save this moment.",
            type = ActivityType.P2P_ALERT,
            status = ActivityStatus.PENDING,
            dueLabel = "Detected just now · Nearby"
        )
    }

    suspend fun updateActivityStatus(activityId: String, status: ActivityStatus): Result<Unit> = runCatching {
        firestore.collection("activities").document(activityId).update("status", status.name).await()
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

        val batch = firestore.batch()
        memberIds.forEach { memberId ->
            val docRef = firestore.collection("activities").document()
            batch.set(
                docRef,
                mapOf(
                    "groupId" to groupId,
                    "groupName" to groupName,
                    "title" to title,
                    "description" to description,
                    "type" to ActivityType.WEEKLY_PROMPT.name,
                    "status" to ActivityStatus.PENDING.name,
                    "dueLabel" to "New",
                    "assignedTo" to memberId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    /**
     * Saves a member's response to an activity. photoPath/videoPath/audioPath are LOCAL
     * device file paths (from CameraScreen/AudioRecorderScreen) -- each one, if present,
     * gets uploaded to Storage here and replaced with its real download URL before
     * anything is written to Firestore. Previously this wrote the raw local path
     * directly into Firestore, which is meaningless on any other device.
     *
     * groupId is required now (not previously passed) specifically so the Storage
     * security rule can check group membership directly on this path, without an
     * extra lookup through the activity document.
     */
    suspend fun saveActivityResponse(
        context: Context,
        activityId: String,
        groupId: String,
        text: String,
        photoPath: String?,
        videoPath: String?,
        audioPath: String?,
        location: String?
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")

        val photoUrl = photoPath?.let { uploadFile(it, groupId, activityId, "photo") }
        val videoUrl = videoPath?.let { uploadFile(it, groupId, activityId, "video") }
        val audioUrl = audioPath?.let { uploadFile(it, groupId, activityId, "audio") }

        // Encrypt the Storage download URLs the same way the text response is
        // encrypted (GroupKeyManager.encryptText only handles String, which a
        // URL is). Note this encrypts the URL, not the file bytes behind it --
        // the actual photo/video/audio content in Storage is still only
        // protected by the Storage security rules, not end-to-end encrypted.
        val encryptedPhotoUrl = photoUrl?.let { GroupKeyManager.encryptText(context, groupId, uid, it) ?: it }
        val encryptedVideoUrl = videoUrl?.let { GroupKeyManager.encryptText(context, groupId, uid, it) ?: it }
        val encryptedAudioUrl = audioUrl?.let { GroupKeyManager.encryptText(context, groupId, uid, it) ?: it }

        val response = mapOf(
            "activityId" to activityId,
            "groupId" to groupId,
            "userId" to uid,
            "text" to text,
            "photoUrl" to encryptedPhotoUrl,
            "videoUrl" to encryptedVideoUrl,
            "audioUrl" to encryptedAudioUrl,
            "location" to location,
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("activity_responses").add(response).await()

        // After saving response, mark activity as completed
        updateActivityStatus(activityId, ActivityStatus.COMPLETED).getOrThrow()
    }

    private suspend fun uploadFile(localPath: String, groupId: String, activityId: String, kind: String): String {
        val file = File(localPath)
        val storageRef = storage.reference
            .child("activity_responses")
            .child(groupId)
            .child(activityId)
            .child("${kind}_${UUID.randomUUID()}.${file.extension}")

        storageRef.putFile(Uri.fromFile(file)).await()
        return storageRef.downloadUrl.await().toString()
    }

    companion object {
        val samplePlaceholderAlert = ActivityItem(
            id = "p2p-placeholder",
            groupName = "Melbourne Uni Squad",
            title = "You're near Sarah right now!",
            description = "Capture this moment together before it's gone.",
            type = ActivityType.P2P_ALERT,
            status = ActivityStatus.PENDING,
            dueLabel = "Detected just now · BLE"
        )

        val sampleActivities = listOf(
            ActivityItem(
                id = "sample-1",
                groupName = "The Reyes-Cheng Family",
                title = "What was your 18th birthday like?",
                description = "Answer with a photo, text, audio, or video.",
                type = ActivityType.WEEKLY_PROMPT,
                status = ActivityStatus.PENDING,
                dueLabel = "Due in 3 days"
            ),
            ActivityItem(
                id = "sample-2",
                groupName = "Melbourne Uni Squad",
                title = "What made you smile today?",
                description = "A quick present-day check-in for the group.",
                type = ActivityType.WEEKLY_PROMPT,
                status = ActivityStatus.PENDING,
                dueLabel = "Due in 5 days"
            ),
            ActivityItem(
                id = "sample-3",
                groupName = "Sarah & Qin Yu",
                title = "Design your cover page for this month",
                description = "Draw or decorate this month's memory page.",
                type = ActivityType.ACTIVITY,
                status = ActivityStatus.COMPLETED,
                dueLabel = "Completed"
            )
        )
    }
}