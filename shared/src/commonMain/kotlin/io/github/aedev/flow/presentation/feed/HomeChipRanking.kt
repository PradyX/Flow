package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Video

/** Only channels the viewer has never watched, followed or rejected, one video each. */
internal fun fromUnknownChannels(
    videos: List<Video>,
    knownChannels: Set<String>,
): List<Video> =
    videos
        .filter { it.channelId.isNotBlank() && it.channelId !in knownChannels }
        .distinctBy { it.channelId }

/** Long-form uploads from the last seven days with a known date. */
internal fun uploadedThisWeek(
    videos: List<Video>,
    now: Long,
): List<Video> =
    videos.filter {
        !it.isShort && !it.isLive && !it.isUpcoming && it.timestamp > 0L &&
            now - it.timestamp in 0..HomeChipParams.RECENT_WINDOW_MS
    }

/**
 * Taste order with a mild lean towards the newest: a video a week old keeps its rank, one from
 * today moves up by up to a fifth of its position.
 */
internal fun freshnessOrder(
    ranked: List<Video>,
    now: Long,
): List<Video> =
    ranked
        .withIndex()
        .sortedBy { (index, video) ->
            val age = ((now - video.timestamp).toDouble() / HomeChipParams.RECENT_WINDOW_MS).coerceIn(0.0, 1.0)
            index * (0.8 + 0.2 * age)
        }.map { it.value }
