package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.model.Group
import kotlinx.coroutines.tasks.await

/**
 * Loads the groups the current user is a member of, and implements the "private circle"
 * join/create flow via a short, shareable invite code (no email/SMS delivery needed).
 *
 * STUB fallback: reads from a "groups" Firestore collection filtered by memberIds
 * array-contains uid. Until a real Firebase project is configured, [getGroups] falls back to
 * [sampleGroups] so the Groups screen is populated and demoable out of the box.
 */
class GroupsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val groupsCollection get() = firestore.collection("groups")
    private val usersCollection get() = firestore.collection("users")

    suspend fun getGroups(): List<Group> {
        val uid = auth.currentUser?.uid ?: return sampleGroups
        return runCatching {
            val snapshot = groupsCollection
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            snapshot.documents.map { it.toGroup() }.ifEmpty { sampleGroups }
        }.getOrElse { sampleGroups }
    }

    /**
     * Creates a new private circle owned by the current user and returns it, with a freshly
     * generated [Group.inviteCode] other people can use with [joinGroupByCode].
     */
    suspend fun createGroup(name: String): Result<Group> = runCatching {
        val uid = requireSignedInUid()
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Give your circle a name first." }

        val inviteCode = generateInviteCode()
        val docRef = groupsCollection.document()

        docRef.set(
            mapOf(
                "name" to trimmedName,
                "ownerId" to uid,
                "memberIds" to listOf(uid),
                "memberCount" to 1,
                "inviteCode" to inviteCode,
                "lastActivitySummary" to "",
                "unreadCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        addGroupToUserProfile(uid, docRef.id)

        Group(
            id = docRef.id,
            name = trimmedName,
            ownerId = uid,
            inviteCode = inviteCode,
            memberCount = 1
        )
    }

    /** Joins the circle whose [Group.inviteCode] matches [code] (case-insensitive). */
    suspend fun joinGroupByCode(code: String): Result<Group> = runCatching {
        val uid = requireSignedInUid()
        val normalizedCode = code.trim().uppercase()
        require(normalizedCode.isNotEmpty()) { "Enter an invite code first." }

        val snapshot = groupsCollection
            .whereEqualTo("inviteCode", normalizedCode)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
            ?: throw NoSuchElementException("That invite code doesn't match any circle. Double-check it with whoever sent it.")

        val existingMemberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        if (uid in existingMemberIds) {
            return@runCatching doc.toGroup()
        }

        doc.reference.update(
            mapOf(
                "memberIds" to FieldValue.arrayUnion(uid),
                "memberCount" to FieldValue.increment(1L)
            )
        ).await()

        addGroupToUserProfile(uid, doc.id)

        val joinedGroup = doc.toGroup()
        joinedGroup.copy(memberCount = joinedGroup.memberCount + 1)
    }

    /**
     * Generates a random 6-character code (letters/digits, ambiguous characters like 0/O and 1/I
     * removed) and re-rolls on the rare chance it collides with an existing group's code.
     */
    suspend fun generateInviteCode(length: Int = 6): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        while (true) {
            val candidate = (1..length).map { alphabet.random() }.joinToString("")
            val collision = groupsCollection
                .whereEqualTo("inviteCode", candidate)
                .limit(1)
                .get()
                .await()
            if (collision.isEmpty) return candidate
        }
    }

    private suspend fun addGroupToUserProfile(uid: String, groupId: String) {
        // Best-effort: the group document is already the source of truth for membership
        // (memberIds), this just makes "groups I'm in" cheap to query from the user's own doc.
        runCatching {
            usersCollection.document(uid).update("groupIds", FieldValue.arrayUnion(groupId)).await()
        }
    }

    private fun requireSignedInUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("You need to be signed in to do that.")

    private fun com.google.firebase.firestore.DocumentSnapshot.toGroup() = Group(
        id = id,
        name = getString("name") ?: "",
        ownerId = getString("ownerId") ?: "",
        inviteCode = getString("inviteCode") ?: "",
        memberCount = (getLong("memberCount") ?: 0L).toInt(),
        memberAvatars = (get("memberAvatars") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        lastActivitySummary = getString("lastActivitySummary") ?: "",
        unreadCount = (getLong("unreadCount") ?: 0L).toInt()
    )

    companion object {
        /** Placeholder data shown when there's no Firebase project configured yet, or no groups exist. */
        val sampleGroups = listOf(
            Group(
                id = "sample-1",
                name = "The Reyes-Cheng Family",
                memberCount = 5,
                lastActivitySummary = "Mariana added a photo to \"18th birthday memories\"",
                unreadCount = 2
            ),
            Group(
                id = "sample-2",
                name = "Melbourne Uni Squad",
                memberCount = 4,
                lastActivitySummary = "New weekly prompt: \"What made you smile today?\"",
                unreadCount = 0
            ),
            Group(
                id = "sample-3",
                name = "Sarah & Qin Yu",
                memberCount = 2,
                lastActivitySummary = "You were both near Union House earlier today",
                unreadCount = 1
            )
        )
    }
}
