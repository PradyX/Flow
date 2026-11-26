package io.github.aedev.flow.data.taste

import io.github.aedev.flow.data.recommendation.HomeMixState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The viewer's card-menu feedback on Home: videos and channels marked "more like this", plus the
 * negative marks that hide them. It is stored beside the setup's taste and merged into
 * [HomeMixState] before ranking, so the mixer's own exclusions and boosts apply rather than a filter
 * over an already-ranked list. Every mark can be taken back from the same menu.
 */
@Serializable
data class HomeFeedback(
    val likedVideoIds: List<String> = emptyList(),
    val likedChannels: List<TasteChannel> = emptyList(),
    val videoIds: List<String> = emptyList(),
    val channels: List<TasteChannel> = emptyList(),
) {
    val isEmpty: Boolean
        get() = likedVideoIds.isEmpty() && likedChannels.isEmpty() && videoIds.isEmpty() && channels.isEmpty()

    fun isLikedVideo(videoId: String): Boolean = videoId in likedVideoIds

    fun isLikedChannel(channelId: String): Boolean = likedChannels.any { it.id == channelId }

    fun isSuppressedVideo(videoId: String): Boolean = videoId in videoIds

    fun isSuppressedChannel(channelId: String): Boolean = channels.any { it.id == channelId }

    /** "More like this" on a video: the video and its channel both rise, and any negative mark on them is cleared. */
    fun withVideoLike(
        videoId: String,
        channel: TasteChannel?,
    ): HomeFeedback =
        HomeFeedback(
            likedVideoIds = likedVideoIds.with(videoId),
            likedChannels = channel?.let { likedChannels.with(it) } ?: likedChannels,
            videoIds = videoIds - videoId,
            channels = if (channel == null) channels else channels.filterNot { it.id == channel.id },
        )

    /** Undo of "More like this": the video and the channel it added. */
    fun withoutVideoLike(
        videoId: String,
        channelId: String,
    ): HomeFeedback =
        copy(
            likedVideoIds = likedVideoIds - videoId,
            likedChannels = likedChannels.filterNot { it.id == channelId },
        )

    /** Undo of a negative mark, so the video or the channel returns to the feed. */
    fun restoreVideo(videoId: String): HomeFeedback = copy(videoIds = videoIds - videoId)

    fun restoreChannel(channelId: String): HomeFeedback = copy(channels = channels.filterNot { it.id == channelId })

    /** Marks one video out of Home's chips; marking it twice keeps one entry. */
    fun withoutVideo(videoId: String): HomeFeedback =
        copy(
            videoIds = if (videoId.isBlank() || videoId in videoIds) videoIds else videoIds + videoId,
            likedVideoIds = likedVideoIds - videoId,
        )

    /** Marks every video from one channel out of Home's chips, and drops any positive mark on it. */
    fun withoutChannel(channel: TasteChannel): HomeFeedback =
        copy(
            channels = if (channel.id.isBlank() || channels.any { it.id == channel.id }) channels else channels + channel,
            likedChannels = likedChannels.filterNot { it.id == channel.id },
        )

    /** The mixer state with these marks applied, keeping whatever [base] already held. */
    fun toMixState(base: HomeMixState = HomeMixState()): HomeMixState =
        base.copy(
            boostedVideoIds = base.boostedVideoIds + likedVideoIds,
            boostedChannels = base.boostedChannels + likedChannels.map { it.id },
            suppressedVideoIds = base.suppressedVideoIds + videoIds,
            suppressedChannels = base.suppressedChannels + channels.map { it.id },
        )

    private fun List<String>.with(id: String): List<String> = if (id.isBlank() || id in this) this else this + id

    private fun List<TasteChannel>.with(channel: TasteChannel): List<TasteChannel> =
        if (channel.id.isBlank() || any { it.id == channel.id }) this else this + channel

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): HomeFeedback? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString<HomeFeedback>(it) }.getOrNull() }

        fun toJson(feedback: HomeFeedback): String = json.encodeToString(feedback)
    }
}
