package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.ui.theme.Dimensions

internal val MusicCardWidth = Dimensions.GridThumbnailHeightBig
internal val MusicCardSpacing = 12.dp
internal val MusicSectionSpacing = 20.dp
private val MusicCardTextSpacing = 6.dp
private val InlineExplicitBadgeSize = 14.dp

/** What a music card opens: its playlist page, its song, or its artist; null when it opens none. */
internal fun musicEntryClick(
    entry: FeedEntry,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
): (() -> Unit)? {
    entry.playlistId?.let { return { onOpenPlaylist(it, entry.title, entry.thumbnailUrl) } }
    entry.videoId?.let { return { onOpenVideo(it, entry.title, entry.thumbnailUrl) } }
    if (entry.isArtist) return { onOpenArtist(entry.id, entry.title, entry.thumbnailUrl) }
    return null
}

/** The cookie an artist's artwork takes wherever a card carries one. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun artistCardShape(): Shape = MaterialShapes.Cookie9Sided.toShape()

/**
 * A square music card: artwork, title, byline. An artist's card is the same card with the artist's
 * own shape and a centred name, which is how the app draws artists among collections.
 */
@Composable
internal fun MusicCard(
    entry: FeedEntry,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    width: Dp = MusicCardWidth,
) {
    val open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist)
    val centred = entry.isArtist
    val textAlign = if (centred) TextAlign.Center else TextAlign.Start
    Column(
        horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
        modifier =
            Modifier
                .width(width)
                .clickable(enabled = open != null) { open?.invoke() },
    ) {
        MediaArtwork(
            title = entry.title,
            imageUrl = entry.thumbnailUrl,
            shape = if (centred) artistCardShape() else MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(modifier = Modifier.height(MusicCardTextSpacing))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        entry.subtitle?.takeIf { it.isNotBlank() }?.let { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A ranked song row, the layout an artist's Popular shelf uses. */
@Composable
internal fun MusicTrackRow(
    rank: Int?,
    entry: FeedEntry,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    val open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist)
    ListItem(
        headlineContent = {
            Text(
                text = entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.explicit) {
                    MusicExplicitBadge(size = InlineExplicitBadgeSize)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                entry.subtitle?.takeIf { it.isNotBlank() }?.let { line ->
                    Text(
                        text = line,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent = {
            rank?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        },
        trailingContent = {
            MediaArtwork(
                title = entry.title,
                imageUrl = entry.thumbnailUrl,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(48.dp).aspectRatio(1f),
            )
        },
        modifier = Modifier.clickable(enabled = open != null) { open?.invoke() },
    )
}
