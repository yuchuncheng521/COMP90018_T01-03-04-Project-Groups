package com.knot.app.model

/** The kind of content this memory contains. */
enum class MemoryType {
    PHOTO,
    TEXT,
    AUDIO,
    VIDEO
}

/**
 * A single shared-timeline entry: one member's contribution to a group,
 * either a free post or a response to an activity/quest.
 * Mirrors a document in the future "memories" Firestore collection.
 */
data class Memory(
    val id: String = "",
    val groupId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val activityId: String = "",       // empty if not tied to a quest/prompt
    val type: MemoryType = MemoryType.TEXT,
    val contentUrl: String = "",       // link to file in storage (photo/audio/video), empty for text
    val thumbnailUrl: String = "",     // small preview, used in monthly/weekly covers
    val textContent: String = "",      // written text, or transcription if audio
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val createdAt: Long = 0L           // epoch millis -- week/month grouping is derived from this
)
