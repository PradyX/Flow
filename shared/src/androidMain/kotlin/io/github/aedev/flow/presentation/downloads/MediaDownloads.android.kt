package io.github.aedev.flow.presentation.downloads

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import io.github.aedev.flow.presentation.playback.MediaCache

actual val isDownloadSupported: Boolean = true

private val mainHandler = Handler(Looper.getMainLooper())

/**
 * Downloads through Media3's own `CacheWriter`, into the cache ExoPlayer reads from. That is the
 * whole point: the bytes a download stores are exactly the bytes the player would have fetched, so
 * a downloaded video plays with the radio off.
 */
actual fun downloadMedia(
    videoId: String,
    stream: PlayableStream,
    onProgress: (DownloadProgress) -> Unit,
    onOutcome: (DownloadOutcome) -> Unit,
) {
    val context = MediaCache.context()
    if (context == null) {
        mainHandler.post { onOutcome(DownloadOutcome.Failed("Storage is not ready")) }
        return
    }
    val urls = listOfNotNull(stream.videoUrl, stream.audioUrl?.takeIf { it.isNotBlank() })
    MediaCache.io().execute {
        val stored = LongArray(urls.size)
        val lengths = LongArray(urls.size)
        var failure: String? = null
        var position = 0
        while (position < urls.size && failure == null) {
            val url = urls[position]
            val track = position
            try {
                val source = MediaCache.dataSourceFactory(context, stream).createDataSourceForDownloading()
                val writer =
                    CacheWriter(
                        source,
                        DataSpec(Uri.parse(url)),
                        null,
                        CacheWriter.ProgressListener { requestLength, bytesCached, _ ->
                            if (requestLength > 0) lengths[track] = requestLength
                            stored[track] = bytesCached
                            var fraction = 0f
                            for (i in urls.indices) {
                                if (lengths[i] > 0) fraction += (stored[i].toFloat() / lengths[i]).coerceIn(0f, 1f)
                            }
                            val progress = DownloadProgress(bytes = stored.sum(), fraction = fraction / urls.size)
                            mainHandler.post { onProgress(progress) }
                        },
                    )
                writer.cache()
                Log.d("FlowDownload", "track $track of $videoId stored ${stored[track]}B")
            } catch (error: Exception) {
                failure = error.message?.takeIf { it.isNotBlank() } ?: "Download failed"
                Log.w("FlowDownload", "download $videoId failed on track $track", error)
            }
            position++
        }
        if (failure != null) {
            // A failed download leaves nothing behind: the partial spans go with it.
            urls.forEach { url -> MediaCache.removeResource(url) }
            val reason = failure
            mainHandler.post { onOutcome(DownloadOutcome.Failed(reason.orEmpty())) }
        } else {
            val bytes = urls.sumOf { url -> MediaCache.cachedBytesOf(url) }
            mainHandler.post { onOutcome(DownloadOutcome.Done(bytes)) }
        }
    }
}

actual fun deleteDownloadedMedia(entry: DownloadEntry) {
    listOfNotNull(entry.videoUrl, entry.audioUrl).forEach { url -> MediaCache.removeResource(url) }
    Log.d("FlowDownload", "removed ${entry.videoId} from the cache")
}
