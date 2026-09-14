package com.knot.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.knot.app.model.Memory
import com.knot.app.model.MemoryType
import kotlinx.coroutines.tasks.await

/**
 * Loads and aggregates every member's contributions for a group into one
 * chronologically ordered shared timeline.
 *
 * STUB: reads from a "memories" Firestore collection filtered by groupId.
 * Falls back to [sampleMemories] so the Timeline screen is demoable before
 * the collection is populated with real data.
 */
class TimelineRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Full timeline for a group, newest first. */
    suspend fun getTimelineForGroup(groupId: String): List<Memory> {
        return runCatching {
            val snapshot = firestore.collection("memories")
                .whereEqualTo("groupId", groupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.map { it.toMemory() }.ifEmpty { sampleMemories }
        }.getOrElse { sampleMemories }
    }

    /** Just the memories inside one week, oldest first (for the weekly spread view). */
    suspend fun getMemoriesForWeek(groupId: String, weekStartMillis: Long, weekEndMillis: Long): List<Memory> {
        return runCatching {
            val snapshot = firestore.collection("memories")
                .whereEqualTo("groupId", groupId)
                .whereGreaterThanOrEqualTo("createdAt", weekStartMillis)
                .whereLessThanOrEqualTo("createdAt", weekEndMillis)
                .orderBy("createdAt")
                .get()
                .await()

            snapshot.documents.map { it.toMemory() }
        }.getOrElse { emptyList() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMemory() = Memory(
        id = id,
        groupId = getString("groupId") ?: "",
        authorId = getString("authorId") ?: "",
        authorName = getString("authorName") ?: "",
        activityId = getString("activityId") ?: "",
        type = runCatching { MemoryType.valueOf(getString("type") ?: "") }.getOrDefault(MemoryType.TEXT),
        contentUrl = getString("contentUrl") ?: "",
        thumbnailUrl = getString("thumbnailUrl") ?: "",
        textContent = getString("textContent") ?: "",
        locationLat = getDouble("locationLat"),
        locationLng = getDouble("locationLng"),
        createdAt = getLong("createdAt") ?: 0L
    )

    companion object {
        /** Placeholder data shown when there's no Firebase project configured yet, or no memories exist. */
        val sampleMemories = listOf(
            Memory(
                id = "sample-1",
                groupId = "sample-1",
                authorId = "u1",
                authorName = "Sarah",
                type = MemoryType.PHOTO,
                thumbnailUrl = "",
                textContent = "",
                createdAt = System.currentTimeMillis() - 3_600_000
            ),
            Memory(
                id = "sample-2",
                groupId = "sample-1",
                authorId = "u2",
                authorName = "Mariana",
                type = MemoryType.TEXT,
                textContent = "Grandma's arroz con pollo, every Sunday without fail.",
                createdAt = System.currentTimeMillis() - 90_000_000
            )
        )
    }
}
