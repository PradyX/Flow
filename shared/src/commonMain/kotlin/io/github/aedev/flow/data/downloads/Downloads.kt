package io.github.aedev.flow.data.downloads

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One video this device downloaded: the metadata a Library row draws, the streams the bytes were
 * fetched from, and what the fetch turned out to be. The URLs double as the disk cache's keys, so
 * this record is also how the player finds a download's bytes again after a relaunch.
 */
@Serializable
data class DownloadEntry(
    val videoId: String,
    val title: String,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
    /** The streams the bytes were fetched from; [audioUrl] is null for a muxed download. */
    val videoUrl: String,
    val audioUrl: String? = null,
    val userAgent: String = "",
    val mimeType: String = "",
    val audioMimeType: String? = null,
    val qualityLabel: String? = null,
    val height: Int? = null,
    /** Bytes stored on disk for every track of this download together. */
    val sizeBytes: Long = 0,
    val completedAtMillis: Long = 0,
) {
    /** A download is one file when the stream was muxed, two when audio came separately. */
    val trackCount: Int get() = if (audioUrl == null) 1 else 2
}

/** Every video downloaded on this device, newest first, one entry per video id. */
@Serializable
data class Downloads(
    val entries: List<DownloadEntry> = emptyList(),
) {
    /** [entry] as the newest row; a second download of the same video replaces the first. */
    fun with(entry: DownloadEntry): Downloads {
        if (entry.videoId.isBlank()) return this
        val stored = entry.copy(sizeBytes = entry.sizeBytes.coerceAtLeast(0))
        return Downloads(listOf(stored) + entries.filterNot { it.videoId == stored.videoId })
    }

    fun without(videoId: String): Downloads = Downloads(entries.filterNot { it.videoId == videoId })

    fun entry(videoId: String): DownloadEntry? = entries.firstOrNull { it.videoId == videoId }

    fun contains(videoId: String): Boolean = entry(videoId) != null

    fun totalBytes(): Long = entries.sumOf { it.sizeBytes }

    /** One row per video id, and never a negative size: what a hand-edited file may still say. */
    fun sanitized(): Downloads =
        Downloads(
            entries
                .filter { it.videoId.isNotBlank() }
                .distinctBy { it.videoId }
                .map { it.copy(sizeBytes = it.sizeBytes.coerceAtLeast(0)) },
        )

    fun encode(): String = DownloadsJson.encodeToString(serializer(), this)

    companion object {
        fun decode(json: String?): Downloads {
            val text = json?.takeIf { it.isNotBlank() } ?: return Downloads()
            val parsed = runCatching { DownloadsJson.decodeFromString(serializer(), text) }.getOrNull()
            return parsed?.sanitized() ?: Downloads()
        }
    }
}

private val DownloadsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
