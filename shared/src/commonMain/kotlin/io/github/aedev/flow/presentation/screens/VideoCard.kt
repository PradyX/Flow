package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.ui.DurationBadge
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.home_action_dont_recommend_channel
import io.github.aedev.flow.resources.home_action_more_like_this
import io.github.aedev.flow.resources.home_action_not_interested
import io.github.aedev.flow.resources.home_action_undo_dont_recommend_channel
import io.github.aedev.flow.resources.home_action_undo_more_like_this
import io.github.aedev.flow.resources.home_action_undo_not_interested
import io.github.aedev.flow.resources.home_card_more_actions
import org.jetbrains.compose.resources.stringResource

/**
 * A video card in the app's stacked shape: 16:9 thumbnail with the duration pill, a two-line title
 * and the channel/views/date line beneath.
 */
@Composable
internal fun VideoCard(
    entry: FeedEntry,
    onOpen: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    /** Provided by a grid that can open a playlist; without it a playlist card is not clickable. */
    onOpenPlaylist: ((playlistId: String, title: String, thumbnailUrl: String?) -> Unit)? = null,
    channelName: String? = null,
    channelAvatarUrl: String? = null,
    liked: Boolean = false,
    notInterested: Boolean = false,
    channelBlocked: Boolean = false,
    onMoreLikeThis: (() -> Unit)? = null,
    onNotInterested: (() -> Unit)? = null,
    onDontRecommendChannel: (() -> Unit)? = null,
) {
    val videoId = entry.videoId
    val playlistId = entry.playlistId
    val opensPlaylist = playlistId != null && onOpenPlaylist != null
    val hasMenu = onMoreLikeThis != null || onNotInterested != null || onDontRecommendChannel != null
    var menuOpen by remember { mutableStateOf(false) }
    val moreLabel = stringResource(Res.string.home_card_more_actions)
    val moreLikeThisLabel =
        stringResource(
            if (liked) Res.string.home_action_undo_more_like_this else Res.string.home_action_more_like_this,
        )
    val notInterestedLabel =
        stringResource(
            if (notInterested) Res.string.home_action_undo_not_interested else Res.string.home_action_not_interested,
        )
    val dontRecommendLabel =
        stringResource(
            if (channelBlocked) {
                Res.string.home_action_undo_dont_recommend_channel
            } else {
                Res.string.home_action_dont_recommend_channel
            },
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = opensPlaylist || videoId != null) {
                    if (opensPlaylist) {
                        onOpenPlaylist?.invoke(playlistId, entry.title, entry.thumbnailUrl)
                    } else {
                        videoId?.let { onOpen(it, entry.title, entry.thumbnailUrl) }
                    }
                },
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            MediaArtwork(
                title = entry.title,
                imageUrl = entry.thumbnailUrl,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            (entry.overlayText ?: entry.durationSeconds?.let(::durationText))?.let { pill ->
                DurationBadge(
                    text = pill,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasMenu) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = FlowIcons.MoreVert,
                            contentDescription = moreLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        onMoreLikeThis?.let { moreLikeThis ->
                            DropdownMenuItem(
                                text = { Text(moreLikeThisLabel) },
                                leadingIcon = { Icon(FlowIcons.ThumbUp, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    moreLikeThis()
                                },
                            )
                        }
                        onNotInterested?.let { notInterestedCallback ->
                            DropdownMenuItem(
                                text = { Text(notInterestedLabel) },
                                leadingIcon = { Icon(FlowIcons.ThumbDown, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    notInterestedCallback()
                                },
                            )
                        }
                        onDontRecommendChannel?.let { dontRecommend ->
                            DropdownMenuItem(
                                text = { Text(dontRecommendLabel) },
                                leadingIcon = { Icon(FlowIcons.Block, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    dontRecommend()
                                },
                            )
                        }
                    }
                }
            }
        }
        if (channelName != null) {
            ChannelLine(name = channelName, avatarUrl = channelAvatarUrl)
        }
        entry.subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The card's channel row: a small portrait when YouTube sent one, then the channel's name. */
@Composable
private fun ChannelLine(
    name: String,
    avatarUrl: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        MediaArtwork(
            title = name,
            imageUrl = avatarUrl,
            shape = CircleShape,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
