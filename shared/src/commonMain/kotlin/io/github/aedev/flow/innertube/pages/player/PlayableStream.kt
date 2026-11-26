package io.github.aedev.flow.innertube.pages.player

import io.github.aedev.flow.innertube.models.response.PlayerResponse

/**
 * A resolved stream: a video track and, when the video is not served as one progressive file, the
 * separate audio track that belongs with it. The player merges the pair; it never plays either
 * alone.
 */
data class PlayableStream(
    val videoUrl: String,
    val videoMimeType: String,
    val qualityLabel: String?,
    val width: Int?,
    val height: Int?,
    val audioUrl: String? = null,
    val audioMimeType: String? = null,
    val expiresInSeconds: Int = 0,
    /**
     * The user agent of the client whose player response minted these URLs. GVS binds a URL to the
     * client that asked for it and answers another agent with 403, so the player must present it.
     */
    val userAgent: String = "",
) {
    /** True when video and audio arrive as one file and need no merging. */
    val isMuxed: Boolean get() = audioUrl == null
}

/** Highest picture the player asks for: 1080p is served everywhere and decodes on every device. */
private const val MAX_PLAYBACK_HEIGHT = 1080

/**
 * H.264 first because it decodes in hardware on every Android device; VP9 next; AV1 last, since
 * many devices (and emulators) cannot decode it at all.
 */
private fun PlayerResponse.StreamingData.Format.codecRank(): Int =
    when {
        mimeType.contains("avc1") -> 3
        mimeType.contains("vp9") || mimeType.contains("vp09") -> 2
        mimeType.contains("av01") -> 1
        else -> 0
    }

private val videoPreference =
    compareBy<PlayerResponse.StreamingData.Format> { it.codecRank() }
        .thenBy { it.height ?: 0 }
        .thenBy { it.bitrate }

/** The tracks up to [MAX_PLAYBACK_HEIGHT]; a response with only taller ones keeps all of them. */
private fun List<PlayerResponse.StreamingData.Format>.underPlaybackHeightCap(): List<PlayerResponse.StreamingData.Format> =
    filter { (it.height ?: 0) <= MAX_PLAYBACK_HEIGHT }.ifEmpty { this }

/**
 * The best stream YouTube returned with a direct URL.
 *
 * A muxed progressive format is preferred because one file is the whole playback. Music videos and
 * many others are served only as separate adaptive tracks, so the fallback pairs the tallest video
 * track with the best audio track; both are handed to the player, which merges them. Ciphered
 * formats without a URL are never candidates: the port has no signature deciphering.
 *
 * H.264 is preferred for the video track because it decodes in hardware on every Android device,
 * including emulators without a VP9/AV1 decoder.
 */
fun PlayerResponse.playableStream(): PlayableStream? {
    val streaming = streamingData ?: return null
    val muxed =
        streaming.formats
            .orEmpty()
            .filter { it.url != null && !it.isText && it.width != null && it.audioChannels != null }
            .underPlaybackHeightCap()
            .maxWithOrNull(videoPreference)
    if (muxed != null) {
        return PlayableStream(
            videoUrl = muxed.url!!,
            videoMimeType = muxed.mimeType,
            qualityLabel = muxed.qualityLabel,
            width = muxed.width,
            height = muxed.height,
            expiresInSeconds = streaming.expiresInSeconds,
        )
    }

    val video =
        streaming.adaptiveFormats
            .filter { it.url != null && it.width != null && it.mimeType.startsWith("video/") }
            .underPlaybackHeightCap()
            .maxWithOrNull(videoPreference) ?: return null
    val audio =
        streaming.adaptiveFormats
            .filter { it.url != null && it.mimeType.startsWith("audio/") }
            .maxWithOrNull(
                compareBy<PlayerResponse.StreamingData.Format> { if (it.mimeType.contains("mp4a")) 1 else 0 }
                    .thenBy { it.bitrate },
            ) ?: return null
    return PlayableStream(
        videoUrl = video.url!!,
        videoMimeType = video.mimeType,
        qualityLabel = video.qualityLabel,
        width = video.width,
        height = video.height,
        audioUrl = audio.url,
        audioMimeType = audio.mimeType,
        expiresInSeconds = streaming.expiresInSeconds,
    )
}
