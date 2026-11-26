package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.YouTube
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Avatars already looked up, so the second Home load asks about no channel twice. */
private val avatarCache = mutableMapOf<String, String>()

private const val AVATAR_LOOKUP_LIMIT = 12
private const val AVATAR_LOOKUP_PARALLELISM = 4

/**
 * Fills in the channel avatar a row arrived without.
 *
 * YouTube's modern Home rows name the channel in their metadata but do not always carry its
 * picture, which is why the app looks the channel up for the cards that need it. This is the same
 * one bounded round, capped and deduplicated by channel, and the answer is kept for later loads.
 * A row that already has an avatar is never asked about, and a lookup that fails leaves the card
 * on its monogram.
 */
internal suspend fun List<Video>.withChannelAvatars(limit: Int = AVATAR_LOOKUP_LIMIT): List<Video> {
    val missing =
        asSequence()
            .filter { it.channelThumbnailUrl.isBlank() && it.channelId.isNotBlank() }
            .map { it.channelId }
            .distinct()
            .take(limit)
            .toList()
    if (missing.isEmpty()) return this
    val permits = Semaphore(AVATAR_LOOKUP_PARALLELISM)
    val resolved =
        coroutineScope {
            missing
                .map { channelId ->
                    async {
                        val cached = avatarCache[channelId]
                        val avatar = cached ?: permits.withPermit { YouTube.channelAvatar(channelId) }
                        if (cached == null && !avatar.isNullOrBlank()) avatarCache[channelId] = avatar
                        channelId to avatar
                    }
                }.map { it.await() }
                .filter { (_, avatar) -> !avatar.isNullOrBlank() }
                .associate { (channelId, avatar) -> channelId to avatar!! }
        }
    if (resolved.isEmpty()) return this
    return map { video ->
        val avatar = resolved[video.channelId]
        if (video.channelThumbnailUrl.isBlank() && avatar != null) {
            video.copy(channelThumbnailUrl = avatar, channelThumbnailUrls = video.channelThumbnailUrls.ifEmpty { listOf(avatar) })
        } else {
            video
        }
    }
}
