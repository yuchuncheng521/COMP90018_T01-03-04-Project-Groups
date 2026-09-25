package com.knot.app.model

/**
 * A private circle/group the current user belongs to.
 * Mirrors a document in the future "groups" Firestore collection.
 */

data class Group(
    val id: String = "",
    val name: String = "",
    val memberCount: Int = 0,
    val memberIds: List<String> = emptyList(), // needed to list/remove specific members
    val memberAvatars: List<String> = emptyList(), // initials or image URLs, placeholder for now
    val lastActivitySummary: String = "",
    val unreadCount: Int = 0,
    val createdAt: Long = 0L, // epoch millis -- when the group was created; drives the month list on GroupDetailScreen
    val ownerId: String = "",
    val inviteCode: String = ""
)
