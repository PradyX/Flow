package io.github.aedev.flow.data.history

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One video the viewer watched, with the metadata a history row draws and where it stopped. */
@Serializable
data class WatchHistoryEntry(
    val videoId: String,
    val title: String,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
    val positionMs: Long = 0,
    val timeWatchedMs: Long = 0,
    val watchedAtMillis: Long = 0,
)

/**
 * The watch history: newest first, one entry per video so rewatching moves a video up instead of
 * adding a second row, and capped so the record cannot grow without bound.
 */
@Serializable
data class WatchHistory(
    val entries: List<WatchHistoryEntry> = emptyList(),
) {
    /** [entry] as the newest row: an existing row for the same video is replaced. */
    fun with(entry: WatchHistoryEntry): WatchHistory {
        if (entry.videoId.isBlank()) return this
        val kept = entries.filterNot { it.videoId == entry.videoId }
        return WatchHistory((listOf(entry.sanitized()) + kept).take(MAX_ENTRIES))
    }

    fun without(videoId: String): WatchHistory = WatchHistory(entries.filterNot { it.videoId == videoId })

    fun cleared(): WatchHistory = WatchHistory()

    fun entry(videoId: String): WatchHistoryEntry? = entries.firstOrNull { it.videoId == videoId }

    fun encode(): String = HistoryJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_ENTRIES = 200

        fun decode(json: String?): WatchHistory {
            val text = json?.takeIf { it.isNotBlank() } ?: return WatchHistory()
            val parsed = runCatching { HistoryJson.decodeFromString(serializer(), text) }.getOrNull()
            return parsed ?: WatchHistory()
        }
    }
}

private fun WatchHistoryEntry.sanitized(): WatchHistoryEntry =
    copy(positionMs = positionMs.coerceAtLeast(0), timeWatchedMs = timeWatchedMs.coerceAtLeast(0))

private val HistoryJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
