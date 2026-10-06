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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

/**
 * Loads and aggregates every member's contributions for a group into one
 * chronologically ordered shared timeline.
 *
 * Responses are read from activity_responses and their encrypted
 * text/media fields are decrypted with the group's shared key.
 */

    private data class ActivityInfo(
        val sharedActivityId: String,
        val title: String,
        val createdAt: Long
    )
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

            val activityInfo = activitySnapshot.documents.associate { document ->
                document.id to ActivityInfo(
                    sharedActivityId = document.getString("sharedActivityId") ?: document.id,
                    title = document.getString("title")
                        ?: document.getString("question")
                        ?: document.getString("prompt")
                        ?: "",
                    createdAt = document.getTimestamp("createdAt")
                        ?.toDate()
                        ?.time
                        ?: 0L
                )
            }

            // 2. Get every response for this group, queried directly by groupId
            // (stored on the response doc itself) rather than by joining
            // through the activities collection's document IDs. A member's
            // own activity doc is deleted when they leave/are removed from
            // the group (or the whole group is deleted), but their past
            // responses should still show up in everyone's shared timeline
            // -- querying by activityId would silently drop those.
            val responseSnapshot = firestore.collection("activity_responses")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            val responseDocuments = responseSnapshot.documents

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

                val activityInfoForResponse = activityInfo[activityId]
                    ?: return@forEach

                val authorId = response.getString("userId")
                    ?: return@forEach

                val authorName = authorNames[authorId] ?: "Member"

                val activityTitle = activityInfoForResponse.title
                val sharedActivityId = activityInfoForResponse.sharedActivityId
                val activityCreatedAt = activityInfoForResponse.createdAt

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
                            activityId = sharedActivityId,
                            activityTitle = activityTitle,
                            activityCreatedAt = activityCreatedAt,
                            type = MemoryType.TEXT,
                            textContent = decryptedText,
                            locationText = locationText,
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
                            activityId = sharedActivityId,
                            activityTitle = activityTitle,
                            activityCreatedAt = activityCreatedAt,
                            type = MemoryType.PHOTO,
                            contentUrl = decryptedPhotoUrl,
                            thumbnailUrl = decryptedPhotoUrl,
                            locationText = locationText,
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
                            activityId = sharedActivityId,
                            activityTitle = activityTitle,
                            activityCreatedAt = activityCreatedAt,
                            type = MemoryType.VIDEO,
                            contentUrl = decryptedVideoUrl,
                            thumbnailUrl = decryptedVideoUrl,
                            locationText = locationText,
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
                            activityId = sharedActivityId,
                            activityTitle = activityTitle,
                            activityCreatedAt = activityCreatedAt,
                            type = MemoryType.AUDIO,
                            contentUrl = decryptedAudioUrl,
                            thumbnailUrl = decryptedAudioUrl,
                            locationText = locationText,
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
    /**
     * Emits a freshly built timeline every time this group's responses change, including
     * your own edits/deletes and the background upload attaching a photo to a response.
     * The listener only acts as a trigger; the existing getTimelineForGroup() still does
     * the decrypting and building, so that logic isn't duplicated.
     */
    fun observeTimelineForGroup(groupId: String): Flow<List<Memory>> = callbackFlow {
        val registration = firestore.collection("activity_responses")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { _, error ->
                if (error != null) {
                    android.util.Log.e("TimelineRepository", "Listener failed", error)
                }
                trySend(Unit)
            }
        awaitClose { registration.remove() }
    }
        .conflate()
        .map { getTimelineForGroup(groupId) }
}
