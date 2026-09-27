package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.model.Group
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Fetches the groups the signed-in user belongs to, and lets them create a new group or join an existing one using an invite code.
 */
data class MemberAvatarInfo(val displayName: String = "", val avatarColor: String = "#C97C5D")

class GroupsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun getGroups(): List<Group> {
        val uid = auth.currentUser?.uid ?: return sampleGroups
        return runCatching {
            val snapshot = firestore.collection("groups")
                .whereArrayContains("memberIds", uid)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toGroup() }.ifEmpty { sampleGroups }
        }.getOrElse { sampleGroups }
    }

    /** Fetches a single group by id -- used by GroupDetailScreen to read its createdAt (for the month list) and other fields. */
    suspend fun getGroup(groupId: String): Group? {
        return runCatching {
            firestore.collection("groups").document(groupId).get().await().toGroup()
        }.getOrNull()
    }

    /** Creates a new private circle owned by the current user and returns it, invite code included. */
    suspend fun createGroup(name: String): Result<Group> = runCatching {
        val uid = auth.currentUser?.uid ?: error("You need to be signed in to create a group.")
        val inviteCode = generateInviteCode()
        val docRef = firestore.collection("groups").document()
        val data = mapOf(
            "name" to name.trim(),
            "ownerId" to uid,
            "inviteCode" to inviteCode,
            "memberIds" to listOf(uid),
            "memberCount" to 1,
            "lastActivitySummary" to "",
            "unreadCount" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        addGroupToUserProfile(uid, docRef.id)

        Group(id = docRef.id, name = name.trim(), memberCount = 1, memberIds = listOf(uid), ownerId = uid, inviteCode = inviteCode)
    }

    /** Display name + avatar color for a list of member ids, used to render real colored avatars in the Groups list. */
    suspend fun getMemberAvatarInfo(memberIds: List<String>): Map<String, MemberAvatarInfo> {
        if (memberIds.isEmpty()) return emptyMap()
        return runCatching {
            // Firestore's whereIn supports at most 10 values -- fine for small groups,
            // would need batching in chunks of 10 for larger ones.
            val snapshot = firestore.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), memberIds.take(10))
                .get()
                .await()
            snapshot.documents.associate {
                it.id to MemberAvatarInfo(
                    displayName = it.getString("displayName") ?: "",
                    avatarColor = it.getString("avatarColor") ?: "#C97C5D"
                )
            }
        }.getOrElse { emptyMap() }
    }

    /** Joins an existing group by its invite code. Fails if the code doesn't match any group. */
    suspend fun joinGroupByCode(code: String): Result<Group> = runCatching {
        val uid = auth.currentUser?.uid ?: error("You need to be signed in to join a group.")
        val normalizedCode = code.trim().uppercase()

        val snapshot = firestore.collection("groups")
            .whereEqualTo("inviteCode", normalizedCode)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: error("No group found with that invite code.")
        val memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        if (uid in memberIds) {
            return@runCatching doc.toGroup() ?: error("Something went wrong loading that group.")
        }

        doc.reference.update(
            mapOf(
                "memberIds" to FieldValue.arrayUnion(uid),
                "memberCount" to FieldValue.increment(1L)
            )
        ).await()

        addGroupToUserProfile(uid, doc.id)

        doc.toGroup()?.copy(memberCount = (doc.getLong("memberCount") ?: 0L).toInt() + 1)
            ?: error("Something went wrong loading that group.")
    }

    /**
     * Removes a member from a group. Anyone can remove themselves (leaving the
     * group); only the owner can remove someone else. The owner can't remove
     * themselves this way
     */
    suspend fun removeMember(groupId: String, memberIdToRemove: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("You need to be signed in to do that.")
        val docRef = firestore.collection("groups").document(groupId)
        val doc = docRef.get().await()
        val ownerId = doc.getString("ownerId") ?: ""

        val isRemovingSelf = uid == memberIdToRemove
        val isOwnerRemovingSomeoneElse = uid == ownerId && memberIdToRemove != ownerId
        if (!isRemovingSelf && !isOwnerRemovingSomeoneElse) {
            error("You don't have permission to remove this member.")
        }

        docRef.update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(memberIdToRemove),
                "memberCount" to FieldValue.increment(-1L)
            )
        ).await()

        runCatching {
            firestore.collection("users").document(memberIdToRemove)
                .update("groupIds", FieldValue.arrayRemove(groupId))
                .await()
        }
    }

    /** Deletes the whole group. Only the owner can do this. Note: this does NOT
     *  cascade-delete the group's memories/activities -- known MVP limitation. */
    suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("You need to be signed in to do that.")
        val docRef = firestore.collection("groups").document(groupId)
        val doc = docRef.get().await()
        val ownerId = doc.getString("ownerId") ?: ""
        if (uid != ownerId) error("Only the group owner can delete this group.")

        docRef.delete().await()
    }

    /** Looks up display names for a list of member ids, for the "remove a member" picker. */
    suspend fun getMemberNames(memberIds: List<String>): Map<String, String> {
        if (memberIds.isEmpty()) return emptyMap()
        return runCatching {
            // Firestore's whereIn supports at most 10 values -- fine for small groups,
            // would need batching in chunks of 10 for larger ones.
            val snapshot = firestore.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), memberIds.take(10))
                .get()
                .await()
            snapshot.documents.associate { it.id to (it.getString("displayName") ?: "Member") }
        }.getOrElse { emptyMap() }
    }

    private suspend fun addGroupToUserProfile(uid: String, groupId: String) {
        runCatching {
            firestore.collection("users").document(uid)
                .update("groupIds", FieldValue.arrayUnion(groupId))
                .await()
        }
    }

    /** Generates a short, human-friendly invite code (no ambiguous characters like 0/O or 1/I). */
    private fun generateInviteCode(length: Int = 6): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        return Group(
            id = id,
            name = getString("name") ?: return null,
            memberCount = (getLong("memberCount") ?: 0L).toInt(),
            memberIds = (get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            memberAvatars = (get("memberAvatars") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            lastActivitySummary = getString("lastActivitySummary") ?: "",
            unreadCount = (getLong("unreadCount") ?: 0L).toInt(),
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            ownerId = getString("ownerId") ?: "",
            inviteCode = getString("inviteCode") ?: ""
        )
    }

    companion object {
        /** Placeholder data shown when there's no Firebase project configured yet, or no groups exist. */
        val sampleGroups = listOf(
            Group(id = "sample-1", name = "The Reyes-Cheng Family", memberCount = 5, lastActivitySummary = "Mariana added a photo to \"18th birthday memories\"", unreadCount = 2),
            Group(id = "sample-2", name = "Melbourne Uni Squad", memberCount = 4, lastActivitySummary = "New weekly prompt: \"What made you smile today?\"", unreadCount = 0),
            Group(id = "sample-3", name = "Sarah & Qin Yu", memberCount = 2, lastActivitySummary = "You were both near Union House earlier today", unreadCount = 1)
        )
    }
}
