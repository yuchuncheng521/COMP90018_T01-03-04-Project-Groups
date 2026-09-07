package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import kotlinx.coroutines.tasks.await

/**
 * Loads the weekly prompts / quests assigned to the current user across all their groups.
 *
 * STUB: reads from an "activities" Firestore collection. The real P2P (BLE proximity) alert
 * described in the project plan is a separate on-device signal (Nearby Connections API /
 * a foreground BLE scan service) -- it is NOT fetched from Firestore. For this base build,
 * [getP2pAlert] is a placeholder you can wire up once the BLE scanning service exists.
 * Falls back to [sampleActivities] / [samplePlaceholderAlert] when there's no backend yet.
 */
class ActivitiesRepository {

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
                    dueLabel = doc.getString("dueLabel") ?: ""
                )
            }.ifEmpty { sampleActivities }
        }.getOrElse { sampleActivities }
    }

    /**
     * TODO(sensors): replace with a real check against the BLE proximity service once it's
     * implemented (see project plan: "Nearby Connections API" + foreground scan service).
     * Returning a non-null value here is what makes the placeholder banner show up.
     */
    suspend fun getP2pAlert(): ActivityItem? = samplePlaceholderAlert

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
                type = ActivityType.QUEST,
                status = ActivityStatus.COMPLETED,
                dueLabel = "Completed"
            )
        )
    }
}
