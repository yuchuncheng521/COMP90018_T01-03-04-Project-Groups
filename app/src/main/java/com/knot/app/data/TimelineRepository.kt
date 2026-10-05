package com.knot.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.crypto.GroupKeyManager
import com.google.firebase.firestore.FirebaseFirestore
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
 * Responses are read from activity_responses and their encrypted
 * text/media fields are decrypted with the group's shared key.
 */
    class TimelineRepository(
        private val context: Context,
        private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    ) {
    /** Full timeline for a group, newest first. */
    suspend fun getTimelineForGroup(groupId: String): List<Memory> {
        val myUid = auth.currentUser?.uid ?: return emptyList()

        return runCatching {
            // 1. Get every activity belonging to this group
            val activitySnapshot = firestore.collection("activities")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            val activityTitles = activitySnapshot.documents.associate { document ->
                document.id to (
                        document.getString("title")
                            ?: document.getString("question")
                            ?: document.getString("prompt")
                            ?: ""
                        )
            }

            val activityIds = activitySnapshot.documents.map { it.id }

            if (activityIds.isEmpty()) {
                return@runCatching emptyList<Memory>()
            }

            // 2. Get all responses belonging to those activities
            // Firestore whereIn queries are limited, so use batches of 30
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
                return@runCatching emptyList<Memory>()
            }

            // 3. Load author names
            val authorIds = responseDocuments
                .mapNotNull { it.getString("userId") }
                .distinct()

            val authorNames = loadAuthorNames(authorIds)

            // 4. Each response can contain multiple types of content
            val memories = mutableListOf<Memory>()

            responseDocuments.forEach { response ->
                val activityId = response.getString("activityId")
                    ?: return@forEach

                val activity = activitySnapshot.documents
                    .firstOrNull { it.id == activityId }
                    ?: return@forEach

                val authorId = response.getString("userId")
                    ?: return@forEach

                val authorName = authorNames[authorId] ?: "Member"

                val activityTitle = activityTitles[activityId].orEmpty()

                val timestamp = response.getTimestamp("timestamp")
                    ?.toDate()
                    ?.time
                    ?: return@forEach

                val locationText = response.getString("location").orEmpty()

                // TEXT
                val textCiphertext = response.getString("text").orEmpty()

                if (textCiphertext.isNotBlank()) {
                    val decryptedText = GroupKeyManager.decryptText(
                        context = context,
                        groupId = groupId,
                        myUid = myUid,
                        ciphertextBase64 = textCiphertext
                    )

                    if (!decryptedText.isNullOrBlank()) {
                        memories += Memory(
                            id = "${response.id}_text",
                            groupId = groupId,
                            authorId = authorId,
                            authorName = authorName,
                            activityId = activityId,
                            activityTitle = activityTitle,
                            type = MemoryType.TEXT,
                            textContent = decryptedText,
                            createdAt = timestamp
                        )
                    }
                }

                // PHOTO
                val photoCiphertext = response.getString("photoUrl").orEmpty()

                if (photoCiphertext.isNotBlank()) {
                    val decryptedPhotoUrl = GroupKeyManager.decryptText(
                        context = context,
                        groupId = groupId,
                        myUid = myUid,
                        ciphertextBase64 = photoCiphertext
                    )

                    if (!decryptedPhotoUrl.isNullOrBlank()) {
                        memories += Memory(
                            id = "${response.id}_photo",
                            groupId = groupId,
                            authorId = authorId,
                            authorName = authorName,
                            activityId = activityId,
                            activityTitle = activityTitle,
                            type = MemoryType.PHOTO,
                            contentUrl = decryptedPhotoUrl,
                            thumbnailUrl = decryptedPhotoUrl,
                            createdAt = timestamp
                        )
                    }
                }

                // VIDEO
                val videoCiphertext = response.getString("videoUrl").orEmpty()

                if (videoCiphertext.isNotBlank()) {
                    val decryptedVideoUrl = GroupKeyManager.decryptText(
                        context = context,
                        groupId = groupId,
                        myUid = myUid,
                        ciphertextBase64 = videoCiphertext
                    )

                    if (!decryptedVideoUrl.isNullOrBlank()) {
                        memories += Memory(
                            id = "${response.id}_video",
                            groupId = groupId,
                            authorId = authorId,
                            authorName = authorName,
                            activityId = activityId,
                            activityTitle = activityTitle,
                            type = MemoryType.VIDEO,
                            contentUrl = decryptedVideoUrl,
                            thumbnailUrl = decryptedVideoUrl,
                            createdAt = timestamp
                        )
                    }
                }

                // AUDIO
                val audioCiphertext = response.getString("audioUrl").orEmpty()

                if (audioCiphertext.isNotBlank()) {
                    val decryptedAudioUrl = GroupKeyManager.decryptText(
                        context = context,
                        groupId = groupId,
                        myUid = myUid,
                        ciphertextBase64 = audioCiphertext
                    )

                    if (!decryptedAudioUrl.isNullOrBlank()) {
                        memories += Memory(
                            id = "${response.id}_audio",
                            groupId = groupId,
                            authorId = authorId,
                            authorName = authorName,
                            activityId = activityId,
                            activityTitle = activityTitle,
                            type = MemoryType.AUDIO,
                            contentUrl = decryptedAudioUrl,
                            thumbnailUrl = decryptedAudioUrl,
                            createdAt = timestamp
                        )
                    }
                }
            }

            memories.sortedByDescending { it.createdAt }

        }.getOrElse { error ->
            android.util.Log.e("TimelineRepository", "Failed to load timeline", error)
            emptyList()
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

                        uid to (
                                document.getString("displayName")
                                    ?: document.getString("name")
                                    ?: uid
                                )
                    }.getOrDefault(uid to uid)
                }
            }
                .awaitAll()
                .toMap()
        }
    }
}
