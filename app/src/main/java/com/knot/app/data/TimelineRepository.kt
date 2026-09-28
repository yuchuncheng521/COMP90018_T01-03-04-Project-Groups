package com.knot.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.crypto.GroupKeyManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.knot.app.model.Memory
import com.knot.app.model.MemoryType
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Loads and aggregates every member's contributions for a group into one
 * chronologically ordered shared timeline.
 *
 * STUB: reads from a "memories" Firestore collection filtered by groupId.
 * Falls back to [sampleMemories] so the Timeline screen is demoable before
 * the collection is populated with real data.
 */
    class TimelineRepository(
        private val context: Context? = null,
        private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    ) {
    /** Full timeline for a group, newest first. */
    suspend fun getTimelineForGroup(groupId: String): List<Memory> {
        return runCatching {
            // 1. Get every activity belonging to this group.
            val activitySnapshot = firestore.collection("activities")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            val activityIds = activitySnapshot.documents.map { it.id }

            if (activityIds.isEmpty()) {
                return@runCatching sampleMemories
            }

            // Keep the activity documents so we can verify that
            // each response belongs to this group.
            val activityById = activitySnapshot.documents.associateBy { it.id }

            // 2. Get responses belonging to those activities.
            //
            // Firestore limits "whereIn" queries, so split large groups
            // of activity IDs into batches.
            val responseSnapshots = coroutineScope {
                activityIds.chunked(30).map { chunk ->
                    async {
                        firestore.collection("activity_responses")
                            .whereIn("activityId", chunk)
                            .get()
                            .await()
                    }
                }.awaitAll()
            }

            val responseDocuments = responseSnapshots.flatMap { it.documents }

            if (responseDocuments.isEmpty()) {
                return@runCatching sampleMemories
            }

            // 3. Get the display names of all users who contributed.
            val authorIds = responseDocuments
                .mapNotNull { it.getString("userId") }
                .distinct()

            val authorNames = loadAuthorNames(authorIds)

            val myUid = auth.currentUser?.uid

            // 4. Convert every response into a Memory.
            responseDocuments.mapNotNull { response ->
                val activityId =
                    response.getString("activityId") ?: return@mapNotNull null

                // Make sure the activity actually belongs to this group.
                if (activityById[activityId] == null) {
                    return@mapNotNull null
                }

                response.toMemory(
                    groupId = groupId,
                    activityId = activityId,
                    activityTitle = activityById[activityId]
                        ?.getString("title")
                        ?: "",
                    authorName = authorNames[
                        response.getString("userId")
                    ] ?: "Member",
                    myUid = myUid
                )
            }
                .sortedByDescending { it.createdAt }
                .ifEmpty { sampleMemories }

        }.getOrElse {
            sampleMemories
        }
    }

    private suspend fun loadAuthorNames(
        authorIds: List<String>
    ): Map<String, String> {
        if (authorIds.isEmpty()) return emptyMap()

        return coroutineScope {
            authorIds.map { uid ->
                async {
                    runCatching {
                        val document = firestore
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()

                        val displayName = document.getString("displayName")

                        if (!displayName.isNullOrBlank()) {
                            uid to displayName
                        } else {
                            null
                        }
                    }.getOrNull()
                }
            }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }
    }

    private suspend fun com.google.firebase.firestore.DocumentSnapshot.toMemory(
        groupId: String,
        activityId: String,
        activityTitle: String,
        authorName: String,
        myUid: String?
    ): Memory? {

        val authorId = getString("userId") ?: return null

        val rawText = getString("text") ?: ""

        val photoPath = getString("photoPath") ?: ""
        val videoPath = getString("videoPath") ?: ""
        val audioPath = getString("audioPath") ?: ""

        val timestamp =
            getTimestamp("timestamp")?.toDate()?.time
                ?: return null

        val decryptedText =
            if (rawText.isNotBlank() && context != null && myUid != null) {

                GroupKeyManager.decryptText(
                    context = context,
                    groupId = groupId,
                    myUid = myUid,
                    ciphertextBase64 = rawText
                ) ?: rawText

            } else {
                rawText
            }

        val type: MemoryType
        val contentUrl: String

        when {
            photoPath.isNotBlank() -> {
                type = MemoryType.PHOTO
                contentUrl = photoPath
            }

            videoPath.isNotBlank() -> {
                type = MemoryType.VIDEO
                contentUrl = videoPath
            }

            audioPath.isNotBlank() -> {
                type = MemoryType.AUDIO
                contentUrl = audioPath
            }

            else -> {
                type = MemoryType.TEXT
                contentUrl = ""
            }
        }

        return Memory(
            id = id,
            groupId = groupId,
            authorId = authorId,
            authorName = authorName,
            activityId = activityId,
            activityTitle = activityTitle,
            type = type,
            contentUrl = contentUrl,
            textContent = decryptedText,
            createdAt = timestamp
        )
    }

    /**
     * Creates a new memory (text/photo/audio/video answer) for a group.
     * Photo/audio/video types only store a contentUrl if you already have one
     * (e.g. from Firebase Storage) -- actual file upload isn't wired yet, so
     * for now this is easiest to test with MemoryType.TEXT.
     */
    suspend fun createMemory(memory: Memory): Result<Memory> = runCatching {
        val docRef = firestore.collection("memories").document()
        val data = mapOf(
            "groupId" to memory.groupId,
            "authorId" to memory.authorId,
            "authorName" to memory.authorName,
            "activityId" to memory.activityId,
            "type" to memory.type.name,
            "contentUrl" to memory.contentUrl,
            "thumbnailUrl" to memory.thumbnailUrl,
            "textContent" to memory.textContent,
            "locationLat" to memory.locationLat,
            "locationLng" to memory.locationLng,
            "createdAt" to System.currentTimeMillis()
        )
        docRef.set(data).await()
        memory.copy(id = docRef.id)
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