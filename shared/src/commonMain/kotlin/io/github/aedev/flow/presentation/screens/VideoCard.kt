package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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

/** The card's own side inset; the grid pads itself by the window margin minus this value. */
private val CardInset = 12.dp
private val CardAvatarSize = 40.dp
private val CardAvatarTarget = 48.dp
private val CardDetailSpacing = 8.dp

/**
 * A video card in the app's stacked shape: a 16:9 thumbnail with the duration pill, then the
 * channel, a two-line title and the metadata line, with the card menu on the right.
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
                .padding(horizontal = CardInset)
                .clickable(enabled = opensPlaylist || videoId != null) {
                    if (opensPlaylist) {
                        onOpenPlaylist(playlistId, entry.title, entry.thumbnailUrl)
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (channelName != null) CardDetailSpacing else CardInset,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
            verticalAlignment = Alignment.Top,
        ) {
            if (channelName != null) {
                // The avatar's tap target is 48 dp; the 40 dp image centres inside it, as it does
                // on the reference card, so the title lines up on both.
                Box(modifier = Modifier.size(CardAvatarTarget)) {
                    MediaArtwork(
                        title = channelName,
                        imageUrl = channelAvatarUrl,
                        shape = CircleShape,
                        modifier = Modifier.size(CardAvatarSize).align(Alignment.Center),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = if (channelName != null) CardDetailSpacing else 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                cardMetaLine(channelName, entry.subtitle)?.let { meta ->
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (hasMenu) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.offset(y = (-12).dp),
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
    }
}

/** The line under a card's title: the channel, then whatever else the surface knows about it. */
private fun cardMetaLine(
    channelName: String?,
    subtitle: String?,
): String? =
    listOfNotNull(
        channelName?.takeIf { it.isNotBlank() },
        subtitle?.takeIf { it.isNotBlank() },
    ).joinToString(" • ").takeIf { it.isNotBlank() }
