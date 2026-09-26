package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import kotlinx.coroutines.tasks.await
import com.knot.app.nearby.NearbyManager

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
                )
            }.ifEmpty { sampleActivities }
        }.getOrElse { sampleActivities }
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
                    "assignedTo" to memberId
                )
            )
        }
        batch.commit().await()
    }

    suspend fun saveActivityResponse(
        activityId: String,
        text: String,
        photoPath: String?,
        videoPath: String?,
        audioPath: String?,
        location: String?
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val response = mapOf(
            "activityId" to activityId,
            "userId" to uid,
            "text" to text,
            "photoPath" to photoPath,
            "videoPath" to videoPath,
            "audioPath" to audioPath,
            "location" to location,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        firestore.collection("activity_responses").add(response).await()
        
        // After saving response, mark activity as completed
        updateActivityStatus(activityId, ActivityStatus.COMPLETED).getOrThrow()
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
