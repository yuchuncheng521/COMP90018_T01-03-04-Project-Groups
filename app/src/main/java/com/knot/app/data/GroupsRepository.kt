package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.model.Group
import kotlinx.coroutines.tasks.await

/**
 * Loads the groups the current user is a member of.
 *
 * STUB: reads from a "groups" Firestore collection filtered by memberIds array-contains uid.
 * Until a real Firebase project is configured, [getGroups] falls back to [sampleGroups] so
 * the Groups screen is populated and demoable out of the box.
 */
class GroupsRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend fun getGroups(): List<Group> {
        val uid = auth.currentUser?.uid ?: return sampleGroups
        return runCatching {
            val snapshot = firestore.collection("groups")
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            snapshot.documents.map { doc ->
                Group(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    memberCount = (doc.getLong("memberCount") ?: 0L).toInt(),
                    memberAvatars = (doc.get("memberAvatars") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    lastActivitySummary = doc.getString("lastActivitySummary") ?: "",
                    unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt()
                )
            }.ifEmpty { sampleGroups }
        }.getOrElse { sampleGroups }
    }

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
