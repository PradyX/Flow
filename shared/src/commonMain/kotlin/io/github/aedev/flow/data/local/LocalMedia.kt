package io.github.aedev.flow.data.local

import io.github.aedev.flow.innertube.pages.player.PlayableStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A media file the viewer imported from this device. Its URI is its identity: picking the same file
 * twice keeps one row, and the read permission the picker takes is what keeps it playable after a
 * restart. The row holds no bytes — the file stays where the viewer keeps it, which is what makes an
 * import free and instant instead of a copy.
 */
@Serializable
data class LocalMediaItem(
    val uri: String,
    val title: String,
    val mimeType: String? = null,
    val sizeBytes: Long = 0,
    val addedAtMillis: Long = 0,
)

/** The imported files, newest first, capped so the record cannot grow without bound. */
@Serializable
data class LocalMediaLibrary(
    val items: List<LocalMediaItem> = emptyList(),
) {
    fun with(item: LocalMediaItem): LocalMediaLibrary {
        if (item.uri.isBlank()) return this
        val kept = items.filterNot { it.uri == item.uri }
        return LocalMediaLibrary((listOf(item) + kept).take(MAX_ITEMS))
    }

    /** What one picker dialog returned: each file is a row, a repeat of one already here moves up. */
    fun withAll(picked: List<LocalMediaItem>): LocalMediaLibrary = picked.fold(this) { acc, item -> acc.with(item) }

    fun without(uri: String): LocalMediaLibrary = LocalMediaLibrary(items.filterNot { it.uri == uri })

    fun item(uri: String): LocalMediaItem? = items.firstOrNull { it.uri == uri }

    /** The stream the queue engine plays for [uri], or null when this library does not hold it. */
    fun streamFor(uri: String): PlayableStream? = item(uri)?.toPlayableStream()

    fun encode(): String = LocalMediaJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_ITEMS = 200

        fun decode(json: String?): LocalMediaLibrary {
            val text = json?.takeIf { it.isNotBlank() } ?: return LocalMediaLibrary()
            val parsed = runCatching { LocalMediaJson.decodeFromString(serializer(), text) }.getOrNull()
            return parsed ?: LocalMediaLibrary()
        }
    }
}

/**
 * The file as a playable stream: one track, already on this device, so the engine reads it through
 * the platform's own resolver and nothing is fetched.
 */
fun LocalMediaItem.toPlayableStream(): PlayableStream =
    PlayableStream(
        videoUrl = uri,
        videoMimeType = mimeType?.takeIf { it.isNotBlank() } ?: DEFAULT_MIME_TYPE,
        qualityLabel = null,
        width = null,
        height = null,
    )

private const val DEFAULT_MIME_TYPE = "video/*"

private val LocalMediaJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
