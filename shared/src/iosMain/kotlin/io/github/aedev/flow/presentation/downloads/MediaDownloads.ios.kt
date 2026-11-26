package io.github.aedev.flow.presentation.downloads

import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.innertube.pages.player.PlayableStream

actual val isDownloadSupported: Boolean = false

/** The iOS build has no video player, so there is nothing a downloaded file could play into. */
actual fun downloadMedia(
    videoId: String,
    stream: PlayableStream,
    onProgress: (DownloadProgress) -> Unit,
    onOutcome: (DownloadOutcome) -> Unit,
) {
    onOutcome(DownloadOutcome.Failed("Downloads need the video player, which this build does not include"))
}

actual fun deleteDownloadedMedia(entry: DownloadEntry) = Unit
