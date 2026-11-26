package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.presentation.feed.VideoDetail
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowSubscribeButton
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_copy_link
import io.github.aedev.flow.resources.action_dislike
import io.github.aedev.flow.resources.action_download
import io.github.aedev.flow.resources.action_downloaded
import io.github.aedev.flow.resources.action_downloading
import io.github.aedev.flow.resources.action_like
import io.github.aedev.flow.resources.action_save
import io.github.aedev.flow.resources.action_saved
import io.github.aedev.flow.resources.action_share
import io.github.aedev.flow.resources.desc_see_less
import io.github.aedev.flow.resources.desc_see_more
import io.github.aedev.flow.resources.notes_add
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/** The reference player's 12 dp gutter for every block under the surface. */
internal val PlayerInfoPadding = 12.dp

private val ActionSpacing = 8.dp
private val ActionIconSize = 18.dp
private val ActionIconSpacing = 6.dp
private val ActionContentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
private const val DESCRIPTION_COLLAPSED_LINES = 6
private val DescriptionToggleSpacing = 8.dp

/** The player area: black, 16:9, showing the video's own artwork; fullscreen gives it the window. */
@Composable
internal fun PlayerSurface(
    posterUrl: String,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun PlayerHeader(
    detail: VideoDetail,
    fallbackTitle: String,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    subscribed: Boolean,
    onSubscriptionChange: (Subscription) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = PlayerInfoPadding),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerInfoPadding)) {
            Text(
                text = detail.title.ifBlank { fallbackTitle },
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                    ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Views and the upload date are two elements on one line, as the app draws them.
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                detail.views?.takeIf { it.isNotBlank() }?.let { views ->
                    Text(
                        text = views,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!detail.published.isNullOrBlank()) {
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                detail.published?.takeIf { it.isNotBlank() }?.let { published ->
                    Text(
                        text = published,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        detail.channel?.let { channel ->
            val channelId = detail.channelId
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerInfoPadding)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(enabled = !channelId.isNullOrBlank()) {
                                channelId?.let { onOpenChannel(it, channel) }
                            },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelAvatar(
                        name = channel,
                        imageUrl = detail.channelAvatarUrl,
                        diameter = 44,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // The name carries the subscriber count under it, as the app's channel row does.
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel,
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        detail.subscribers?.takeIf { it.isNotBlank() }?.let { subscribers ->
                            Text(
                                text = subscribers,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // Without an id there is nothing to subscribe to, so the control stays off the row.
                if (!channelId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FlowSubscribeButton(
                        subscribed = subscribed,
                        onSubscribe = {
                            onSubscriptionChange(
                                Subscription(channelId, channel, detail.channelAvatarUrl),
                            )
                        },
                        onUnsubscribe = {
                            onSubscriptionChange(
                                Subscription(channelId, channel, detail.channelAvatarUrl),
                            )
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(8.dp))
    }
}

/**
 * The row under the channel: the like/dislike pair as one connected button group, then share and
 * copy link, and under it the pair that writes to the on-device library: save and download.
 */
@Composable
internal fun VideoActionRow(
    likes: String?,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onSave: () -> Unit,
    onDownload: () -> Unit,
    /** How far a download running now has got, 0..1; null when none is running. */
    downloadProgress: Float? = null,
    /** Whether this video already has a note, and the way to write another at the playing moment. */
    hasNote: Boolean = false,
    onNote: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ActionSpacing),
        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerInfoPadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ActionSpacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    shapes = connectedShapes(ButtonGroupDefaults.connectedLeadingButtonShape),
                    colors = actionToggleColors(),
                    contentPadding = ActionContentPadding,
                    enabled = false,
                ) {
                    Icon(
                        imageVector = FlowIcons.ThumbUp,
                        contentDescription = stringResource(Res.string.action_like),
                        modifier = Modifier.size(ActionIconSize),
                    )
                    Spacer(modifier = Modifier.width(ActionIconSpacing))
                    Text(
                        text = likes?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.action_like),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    shapes = connectedShapes(ButtonGroupDefaults.connectedTrailingButtonShape),
                    colors = actionToggleColors(),
                    contentPadding = ActionContentPadding,
                    enabled = false,
                ) {
                    Icon(
                        imageVector = FlowIcons.ThumbDown,
                        contentDescription = stringResource(Res.string.action_dislike),
                        modifier = Modifier.size(ActionIconSize),
                    )
                }
            }

            FilledTonalButton(
                onClick = onShare,
                shapes = ButtonDefaults.shapes(),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                contentPadding = ActionContentPadding,
            ) {
                Icon(
                    imageVector = FlowIcons.Share,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
                Spacer(modifier = Modifier.width(ActionIconSpacing))
                Text(text = stringResource(Res.string.action_share), style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalButton(
                onClick = onCopyLink,
                shapes = ButtonDefaults.shapes(),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                contentPadding = ActionContentPadding,
            ) {
                Icon(
                    imageVector = FlowIcons.Link,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
                Spacer(modifier = Modifier.width(ActionIconSpacing))
                Text(
                    text = stringResource(Res.string.action_copy_link),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ActionSpacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ToggleButton(
                checked = isSaved,
                onCheckedChange = { onSave() },
                shapes = connectedShapes(MaterialTheme.shapes.large),
                colors = actionToggleColors(),
                contentPadding = ActionContentPadding,
            ) {
                Icon(
                    imageVector = FlowIcons.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
                Spacer(modifier = Modifier.width(ActionIconSpacing))
                Text(
                    text =
                        stringResource(
                            if (isSaved) Res.string.action_saved else Res.string.action_save,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            ToggleButton(
                checked = isDownloaded,
                onCheckedChange = { onDownload() },
                // A running download owns the button until it lands, so a second tap cannot start
                // the same fetch twice.
                enabled = downloadProgress == null,
                shapes = connectedShapes(MaterialTheme.shapes.large),
                colors = actionToggleColors(),
                contentPadding = ActionContentPadding,
            ) {
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(ActionIconSize),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = FlowIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(ActionIconSize),
                    )
                }
                Spacer(modifier = Modifier.width(ActionIconSpacing))
                Text(
                    text =
                        stringResource(
                            when {
                                downloadProgress != null -> Res.string.action_downloading
                                isDownloaded -> Res.string.action_downloaded
                                else -> Res.string.action_download
                            },
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            ToggleButton(
                checked = hasNote,
                onCheckedChange = { onNote() },
                shapes = connectedShapes(MaterialTheme.shapes.large),
                colors = actionToggleColors(),
                contentPadding = ActionContentPadding,
            ) {
                Icon(
                    imageVector = FlowIcons.Note,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
                Spacer(modifier = Modifier.width(ActionIconSpacing))
                Text(
                    text = stringResource(Res.string.notes_add),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The group's seam shape, worn in every state until the port can actually toggle a vote. */
@Composable
private fun connectedShapes(shape: androidx.compose.ui.graphics.Shape) = ToggleButtonShapes(shape, shape, shape)

@Composable
private fun actionToggleColors() =
    ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = MaterialTheme.colorScheme.onSurface,
    )

/**
 * The description in the app's rounded secondary surface, clamped until the reader asks for the
 * rest. The control appears only once the text has actually overflowed, so a short description
 * never gets a button that expands nothing.
 */
@Composable
internal fun DescriptionCard(text: String) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflowed by remember(text) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerInfoPadding)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Dimensions.ContentPaddingHorizontal),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (result.hasVisualOverflow) overflowed = true },
        )
        if (overflowed) {
            OutlinedButton(
                onClick = { expanded = !expanded },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(top = DescriptionToggleSpacing),
            ) {
                Text(
                    text =
                        stringResource(
                            if (expanded) Res.string.desc_see_less else Res.string.desc_see_more,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
