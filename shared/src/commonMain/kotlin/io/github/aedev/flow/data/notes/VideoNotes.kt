package io.github.aedev.flow.data.notes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A note the viewer wrote on a video at a moment in it, the way the reference app's notes keep the
 * spot they are about so tapping one resumes there.
 */
@Serializable
data class VideoNote(
    val id: String,
    val videoId: String,
    val videoTitle: String,
    val timestampMs: Long = 0,
    val text: String,
    val createdAtMillis: Long = 0,
)

/** Every note on the device, newest first. Notes are keyed by their own id, not by video. */
@Serializable
data class VideoNotes(
    val notes: List<VideoNote> = emptyList(),
) {
    /** Adds [text] as a note on [videoId] at [timestampMs]; blank text adds nothing. */
    fun add(
        videoId: String,
        videoTitle: String,
        timestampMs: Long,
        text: String,
        nowMillis: Long,
    ): VideoNotes {
        val body = text.trim()
        if (videoId.isBlank() || body.isEmpty()) return this
        val note =
            VideoNote(
                id = "$videoId@$nowMillis",
                videoId = videoId,
                videoTitle = videoTitle,
                timestampMs = timestampMs.coerceAtLeast(0),
                text = body,
                createdAtMillis = nowMillis,
            )
        return VideoNotes((listOf(note) + notes).take(MAX_NOTES))
    }

    fun without(id: String): VideoNotes = VideoNotes(notes.filterNot { it.id == id })

    fun forVideo(videoId: String): List<VideoNote> = notes.filter { it.videoId == videoId }.sortedBy { it.timestampMs }

    fun encode(): String = NotesJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_NOTES = 500

        fun decode(json: String?): VideoNotes {
            val text = json?.takeIf { it.isNotBlank() } ?: return VideoNotes()
            val parsed = runCatching { NotesJson.decodeFromString(serializer(), text) }.getOrNull()
            return parsed ?: VideoNotes()
        }
    }
}

private val NotesJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
