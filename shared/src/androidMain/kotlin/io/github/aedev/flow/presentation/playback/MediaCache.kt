package io.github.aedev.flow.presentation.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import io.github.aedev.flow.innertube.models.YouTubeClient
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The queue's media, read through a disk cache so the entry after the playing one can be filled
 * ahead of the boundary. ExoPlayer starts the next track from the cache instead of its first
 * network bytes, which is what keeps the handoff out of a buffering state.
 */
internal object MediaCache {
    private var shared: SimpleCache? = null
    private var prefetcher: ExecutorService? = null
    private val started = ConcurrentHashMap.newKeySet<String>()

    /** Keeps the app context so settings can free the cache without one being in reach. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private var appContext: Context? = null

    /** The host's context, for the downloader that writes into this cache. */
    fun context(): Context? = appContext

    /** The thread a download runs on: one at a time, so two never share the link. */
    fun io(): ExecutorService = executor()

    /** The bytes a resource occupies in the cache right now. */
    fun cachedBytesOf(url: String): Long =
        runCatching {
            val open = shared ?: return 0L
            val length = ContentMetadata.getContentLength(open.getContentMetadata(url))
            open.getCachedBytes(url, 0, if (length > 0) length else C.LENGTH_UNSET.toLong())
        }.getOrDefault(0L)

    /** Drops one resource's spans, which is how a removed download gives its space back. */
    fun removeResource(url: String) {
        runCatching { shared?.removeResource(url) }
        started.remove(url)
    }

    /**
     * Drops every cache entry the player is not using right now. An entry a data source is reading
     * from is locked, so it is skipped rather than throwing away the bytes under a playing track.
     */
    fun clear() {
        val open = shared ?: return
        open.keys.toList().forEach { key -> runCatching { open.removeResource(key) } }
        started.clear()
    }

    private fun cache(context: Context): SimpleCache =
        synchronized(this) {
            shared
                ?: SimpleCache(File(context.filesDir, CACHE_DIR), LeastRecentlyUsedCacheEvictor(CACHE_BYTES)).also {
                    shared = it
                }
        }

    private fun executor(): ExecutorService =
        synchronized(this) {
            prefetcher ?: Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "flow-prefetch") }.also { prefetcher = it }
        }

    /** The upstream for one stream: GVS binds the URL to the client whose response minted it. */
    private fun upstream(stream: PlayableStream): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource
            .Factory()
            .setUserAgent(stream.userAgent.takeIf { it.isNotBlank() } ?: YouTubeClient.ANDROID_VR_NO_AUTH.userAgent)
            .setAllowCrossProtocolRedirects(true)

    fun dataSourceFactory(
        context: Context,
        stream: PlayableStream,
    ): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(upstream(stream))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    /**
     * The datasource for an imported file. It is already on this device, so copying it into the
     * cache would write the same bytes twice and hold a second copy nobody asked for; the platform's
     * own resolver opens `content://` and `file://` directly, and the cache only ever holds URLs.
     */
    fun localDataSourceFactory(context: Context): DataSource.Factory = DefaultDataSource.Factory(context.applicationContext)

    /** True when [stream] is one of the device's own files rather than a URL to fetch. */
    fun isLocalStream(stream: PlayableStream): Boolean = !stream.isNetworkUri()

    /** True when [this] is a URL fetched over the network rather than one of the device's files. */
    private fun PlayableStream.isNetworkUri(): Boolean = Uri.parse(videoUrl).scheme?.startsWith("http") == true

    /**
     * Fills the cache with the start of [stream], enough for its media period to be prepared and
     * played without waiting on the network. Runs once per entry, on its own thread, so it never
     * competes with the track that is playing.
     */
    fun prefetch(
        context: Context,
        videoId: String,
        stream: PlayableStream,
    ) {
        // Only a remote stream is worth warming; a local file is read from disk as it plays.
        if (!stream.isNetworkUri()) return
        if (!started.add(videoId)) return
        val appContext = context.applicationContext
        executor().execute {
            listOfNotNull(stream.videoUrl, stream.audioUrl).forEach { url ->
                runCatching {
                    val source = dataSourceFactory(appContext, stream).createDataSource()
                    source.open(DataSpec(Uri.parse(url), 0, PREFETCH_BYTES))
                    val buffer = ByteArray(PREFETCH_CHUNK)
                    var total = 0L
                    while (total < PREFETCH_BYTES) {
                        val read = source.read(buffer, 0, buffer.size)
                        if (read == C.RESULT_END_OF_INPUT) break
                        total += read
                    }
                    source.close()
                    Log.d("FlowPlayback", "prefetch $videoId cached=${total}B")
                }.onFailure { error -> Log.w("FlowPlayback", "prefetch failed $videoId", error) }
            }
        }
    }
}

/** Points the cache at the app's files directory; called once from the host. */
fun initializeMediaCache(context: Context) {
    MediaCache.initialize(context)
}

actual fun clearPlaybackCache() {
    MediaCache.clear()
}

private const val CACHE_DIR = "media_cache"

/** The whole cache's ceiling on disk; the queue's next entries are what it is for. */
private const val CACHE_BYTES = 2L * 1024 * 1024 * 1024

/** Enough of a track's video and audio to start it without touching the network. */
private const val PREFETCH_BYTES = 6L * 1024 * 1024
private const val PREFETCH_CHUNK = 64 * 1024
