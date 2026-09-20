package com.knot.app.model

/**
 * Represents one private circle/group that the signed-in user is a member of.
 * The fields here match the data stored in Firestore's "groups" collection.
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val memberCount: Int = 0,
    val memberAvatars: List<String> = emptyList(), // initials or image URLs, placeholder for now
    val lastActivitySummary: String = "",
    val unreadCount: Int = 0,
    val ownerId: String = "",
    val inviteCode: String = ""
)