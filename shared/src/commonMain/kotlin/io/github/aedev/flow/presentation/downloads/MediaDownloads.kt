package io.github.aedev.flow.presentation.downloads

import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.innertube.pages.player.PlayableStream

/** How far a download has got, as a fraction of every track it has to fetch. */
data class DownloadProgress(
    /** Bytes stored so far, across the tracks fetched so far. */
    val bytes: Long,
    /** 0..1 across the whole download, or 0 while no track's length is known yet. */
    val fraction: Float,
)

/** What a download ended as: the bytes it stored, or why it stored none. */
sealed interface DownloadOutcome {
    data class Done(
        val bytes: Long,
    ) : DownloadOutcome

    data class Failed(
        val reason: String,
    ) : DownloadOutcome
}

/**
 * Fetches [stream]'s tracks into the player's disk cache: the same cache playback reads through, so
 * a downloaded video plays with no network at all. Reports [onProgress] while it runs and
 * [onOutcome] exactly once, both on the main thread. A failed download removes whatever it wrote,
 * so a half-file never outlives it.
 */
expect fun downloadMedia(
    videoId: String,
    stream: PlayableStream,
    onProgress: (DownloadProgress) -> Unit,
    onOutcome: (DownloadOutcome) -> Unit,
)

/**
 * The stream a completed download was fetched from. Its URLs are the cache keys the bytes were
 * stored under, so the player finds them again without asking YouTube for anything.
 */
fun DownloadEntry.toPlayableStream(): PlayableStream =
    PlayableStream(
        videoUrl = videoUrl,
        videoMimeType = mimeType,
        qualityLabel = qualityLabel,
        width = null,
        height = height,
        audioUrl = audioUrl,
        audioMimeType = audioMimeType,
        userAgent = userAgent,
    )

/** Deletes the bytes [entry] stands for, leaving the rest of the cache alone. */
expect fun deleteDownloadedMedia(entry: DownloadEntry)

/** True when this target can fetch media to disk at all (it plays video). */
expect val isDownloadSupported: Boolean
