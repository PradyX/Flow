package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.presentation.feed.Short
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_comments
import io.github.aedev.flow.resources.action_like
import io.github.aedev.flow.resources.action_save
import io.github.aedev.flow.resources.action_saved
import io.github.aedev.flow.resources.action_share
import io.github.aedev.flow.resources.cd_more_options
import io.github.aedev.flow.resources.playlist_more_options
import io.github.aedev.flow.resources.subscribe
import io.github.aedev.flow.resources.subscribed
import io.github.aedev.flow.ui.theme.PlayerScrim
import io.github.aedev.flow.ui.theme.PlayerScrimContent
import io.github.aedev.flow.ui.theme.PlayerScrimPanel
import io.github.aedev.flow.utils.formatViewCount
import org.jetbrains.compose.resources.stringResource

// The reel's chrome at the reference's own metrics: the rail down the right edge, and the channel,
// title, metadata and sound down the left, resting on the scrim tokens rather than a surface.
internal val ShortsChromeStartPadding = 16.dp
internal val ShortsChromeEndPadding = 8.dp
internal val ShortsMetadataEndPadding = 16.dp

// The reference seats the reel's chrome 34dp above its short navigation bar. The shell's bar is
// 16.5dp taller at this density (it is the one chrome piece already matched to the app's own bar),
// so the offset absorbs that difference and the chrome lands on the reference's rows.
internal val ShortsControlsBottomOffset = 17.5.dp

private val RailSpacing = 16.dp
private val RailButtonSize = 44.dp
private val RailIconSize = 24.dp
private val RailLabelSpacing = 2.dp
private val DiscSize = 36.dp
private val DiscRim = 3.dp
private val AvatarSize = 36.dp
private val ChannelRowSpacing = 8.dp
private val TitleSpacing = 8.dp
private val MetadataSpacing = 2.dp
private val SoundSpacing = 6.dp
private val SoundBarWidth = 2.dp
private val SoundBarGap = 2.dp
private val SoundBarMinHeight = 4.dp
private val SoundBarMaxHeight = 12.dp
private val SubscribeHorizontalPadding = 12.dp
private val SubscribeVerticalPadding = 4.dp

private val OverlayTextShadow = Shadow(color = PlayerScrim, blurRadius = 4f)

/** The actions down a reel's right edge, the channel's record last. */
@Composable
internal fun ShortsActionRail(
    isLiked: Boolean,
    likeLabel: String,
    onLikeClick: (() -> Unit)?,
    commentLabel: String,
    onCommentsClick: () -> Unit,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    channelAvatarUrl: String?,
    channelName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RailSpacing),
        modifier = modifier,
    ) {
        ShortsRailAction(
            icon = FlowIcons.ThumbUp,
            label = likeLabel.ifBlank { stringResource(Res.string.action_like) },
            contentDescription = stringResource(Res.string.action_like),
            contentColor = if (isLiked) MaterialTheme.colorScheme.primary else PlayerScrimContent,
            onClick = onLikeClick,
        )
        ShortsRailAction(
            icon = FlowIcons.Comment,
            label = commentLabel.ifBlank { stringResource(Res.string.action_comments) },
            contentDescription = stringResource(Res.string.action_comments),
            onClick = onCommentsClick,
        )
        ShortsRailAction(
            icon = if (isSaved) FlowIcons.Bookmark else FlowIcons.BookmarkBorder,
            // The reference's rail keeps one word here; the filled mark carries the state.
            label = stringResource(Res.string.action_save),
            contentDescription = stringResource(if (isSaved) Res.string.action_saved else Res.string.action_save),
            contentColor = if (isSaved) MaterialTheme.colorScheme.primary else PlayerScrimContent,
            onClick = onSaveClick,
        )
        ShortsRailAction(
            icon = FlowIcons.Share,
            label = stringResource(Res.string.action_share),
            contentDescription = stringResource(Res.string.action_share),
            onClick = onShareClick,
        )
        ShortsRailAction(
            icon = FlowIcons.MoreVert,
            label = stringResource(Res.string.cd_more_options),
            contentDescription = stringResource(Res.string.cd_more_options),
            onClick = onMoreClick,
        )
        ShortsChannelDisc(avatarUrl = channelAvatarUrl, channelName = channelName)
    }
}

@Composable
private fun ShortsRailAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: (() -> Unit)?,
    contentColor: Color = PlayerScrimContent,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { onClick?.invoke() },
            enabled = onClick != null,
            modifier = Modifier.size(RailButtonSize),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(RailIconSize),
            )
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(RailLabelSpacing))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(shadow = OverlayTextShadow),
                color = PlayerScrimContent,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** The channel's avatar as the record the reel plays, ringed the way the reference rings it. */
@Composable
private fun ShortsChannelDisc(
    avatarUrl: String?,
    channelName: String?,
) {
    Box(
        modifier =
            Modifier
                .size(DiscSize)
                .background(PlayerScrimPanel, CircleShape)
                .padding(DiscRim),
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = channelName,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

/** Channel, subscribe control, title, views and age, then the sound the reel plays. */
@Composable
internal fun ShortsMetadataOverlay(
    short: Short,
    isSubscribed: Boolean,
    onChannelClick: () -> Unit,
    onSubscribeToggle: () -> Unit,
    onOpenDescription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onChannelClick),
        ) {
            // The avatar's own box fixes the row's height at the reference's 36dp; the image fills
            // it, so a vendor's odd intrinsic size cannot shrink the row around it.
            Box(
                modifier =
                    Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (!short.channelAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = short.channelAvatarUrl,
                        contentDescription = short.channelName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(ChannelRowSpacing))
            short.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                Text(
                    text = channel,
                    style = MaterialTheme.typography.titleMedium.copy(shadow = OverlayTextShadow),
                    fontWeight = FontWeight.Bold,
                    color = PlayerScrimContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(ChannelRowSpacing))
            }
            ShortsSubscribePill(isSubscribed = isSubscribed, onClick = onSubscribeToggle)
        }

        if (short.title.isNotBlank()) {
            Spacer(modifier = Modifier.height(TitleSpacing))
            Text(
                text = short.title,
                style = MaterialTheme.typography.bodyMedium.copy(shadow = OverlayTextShadow),
                color = PlayerScrimContent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenDescription),
            )
        }

        val metadata = shortsMetadataLine(short)
        if (metadata.isNotBlank()) {
            Spacer(modifier = Modifier.height(MetadataSpacing))
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodySmall.copy(shadow = OverlayTextShadow),
                color = PlayerScrimContent,
            )
        }

        short.soundTitle?.takeIf { it.isNotBlank() }?.let { sound ->
            Spacer(modifier = Modifier.height(SoundSpacing))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShortsSoundBars()
                Spacer(modifier = Modifier.width(SoundSpacing))
                Text(
                    text = sound,
                    style = MaterialTheme.typography.labelMedium.copy(shadow = OverlayTextShadow),
                    color = PlayerScrimContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The watch page's own order: views, then the age beside them. */
private fun shortsMetadataLine(short: Short): String {
    val views = compactViewCount(short.viewCountText)
    val age = short.publishedTimeText?.trim().orEmpty()
    return listOf(views, age).filter { it.isNotEmpty() }.joinToString(" • ")
}

/**
 * "20,027,283 views" as the app prints it beside a video: "20M views". The watch page hands the
 * number over already spelled out, so the digits are taken back out of it and formatted.
 */
private fun compactViewCount(text: String?): String {
    val raw = text?.trim().orEmpty()
    if (raw.isEmpty()) return raw
    val match = Regex("[\\d][\\d,. ]*").find(raw)
    val digits = match?.value?.filter(Char::isDigit)?.toLongOrNull()
    val compact =
        if (match != null && digits != null && digits >= 10_000) {
            raw.replaceRange(match.range, formatViewCount(digits))
        } else {
            raw
        }
    // YouTube sometimes serves "25Kviews"; the app writes the count and its noun apart.
    return compact.replace(Regex("(\\d[KMB])(?=[A-Za-z])"), "$1 ")
}

/** The reference's compact pill: a primary fill while unsubscribed, a tonal one after. */
@Composable
private fun ShortsSubscribePill(
    isSubscribed: Boolean,
    onClick: () -> Unit,
) {
    val container = if (isSubscribed) PlayerScrimPanel else MaterialTheme.colorScheme.primary
    val content = if (isSubscribed) PlayerScrimContent else MaterialTheme.colorScheme.onPrimary
    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(container)
                .clickable(onClick = onClick)
                .padding(horizontal = SubscribeHorizontalPadding, vertical = SubscribeVerticalPadding),
    ) {
        Text(
            text = stringResource(if (isSubscribed) Res.string.subscribed else Res.string.subscribe),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
        )
    }
}

/** The sound's mark: the bars the reference shows beside the track's name. */
@Composable
private fun ShortsSoundBars() {
    Row(horizontalArrangement = Arrangement.spacedBy(SoundBarGap), verticalAlignment = Alignment.CenterVertically) {
        listOf(SoundBarMaxHeight, SoundBarMinHeight, SoundBarMaxHeight - 4.dp, SoundBarMinHeight + 2.dp).forEach { bar ->
            Box(
                modifier =
                    Modifier
                        .width(SoundBarWidth)
                        .height(bar)
                        .background(PlayerScrimContent, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
