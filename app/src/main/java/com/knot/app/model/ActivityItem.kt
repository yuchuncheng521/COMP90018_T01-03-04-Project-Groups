package com.knot.app.model

/** The kind of activity being asked of the user. */
enum class ActivityType {
    WEEKLY_PROMPT,
    QUEST,
    P2P_ALERT
}

/** Whether the user has responded to this activity yet. */
enum class ActivityStatus {
    PENDING,
    COMPLETED,
    EXPIRED
}

/**
 * A single activity/prompt/quest the user needs to act on, shown in the Activities list.
 * Mirrors a document in the future "activities" Firestore collection, scoped per user or group.
 */
data class ActivityItem(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val title: String = "",
    val description: String = "",
    val type: ActivityType = ActivityType.WEEKLY_PROMPT,
    val status: ActivityStatus = ActivityStatus.PENDING,
    val dueLabel: String = "" // e.g. "Due in 3 days" -- kept as a display string for this base build
)
