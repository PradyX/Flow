package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.MusicSection
import io.github.aedev.flow.presentation.feed.MusicSectionShape
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.ui.theme.Dimensions

private const val DENSE_TILE_ROWS = 3

/**
 * A shelf's body, in the shape its entries call for: dense tiles for mixes, portraits for artists,
 * and cards for everything else.
 */
@Composable
internal fun MusicSectionBody(
    section: MusicSection,
    tileWidth: Dp,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    when (section.shape) {
        MusicSectionShape.DenseTiles -> {
            MusicDenseTileGrid(
                entries = section.entries,
                tileWidth = tileWidth,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }

        MusicSectionShape.ArtistPortraits -> {
            MusicArtistPortraitRow(
                entries = section.entries,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }

        MusicSectionShape.CollectionCards -> {
            MusicCardRow(
                entries = section.entries,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}

/** Three rows of compact tiles, scrolled sideways: a square artwork and two lines of text each. */
@Composable
private fun MusicDenseTileGrid(
    entries: List<FeedEntry>,
    tileWidth: Dp,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(DENSE_TILE_ROWS),
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.DenseTileGap),
        verticalArrangement = Arrangement.spacedBy(Dimensions.DenseTileGap),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimensions.ListItemHeight * DENSE_TILE_ROWS + Dimensions.DenseTileGap * (DENSE_TILE_ROWS - 1)),
    ) {
        items(entries, key = { it.id }) { entry ->
            MusicDenseTile(
                entry = entry,
                width = tileWidth,
                open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist),
            )
        }
    }
}

@Composable
private fun MusicDenseTile(
    entry: FeedEntry,
    width: Dp,
    open: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .width(width)
                .height(Dimensions.ListItemHeight)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = open != null) { open?.invoke() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaArtwork(
            title = entry.title,
            imageUrl = entry.thumbnailUrl,
            shape = RectangleShape,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = Dimensions.ContentPaddingHorizontal),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.subtitle?.takeIf { it.isNotBlank() }?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Portraits in the artist shape, a centred name under each, scrolled sideways. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicArtistPortraitRow(
    entries: List<FeedEntry>,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    val portraitShape: Shape = MaterialShapes.Cookie9Sided.toShape()

    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.ItemSpacing),
    ) {
        items(entries, key = { it.id }) { entry ->
            val open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist)
            Column(
                modifier =
                    Modifier
                        .width(Dimensions.ArtistPortraitSize)
                        .clickable(enabled = open != null) { open?.invoke() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MediaArtwork(
                    title = entry.title,
                    imageUrl = entry.thumbnailUrl,
                    shape = portraitShape,
                    modifier = Modifier.size(Dimensions.ArtistPortraitSize),
                )
                Spacer(modifier = Modifier.height(Dimensions.DenseTileGap))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Square cards with two lines under the artwork, scrolled sideways. */
@Composable
private fun MusicCardRow(
    entries: List<FeedEntry>,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.ItemSpacing),
    ) {
        items(entries, key = { it.id }) { entry ->
            MusicCard(
                entry = entry,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}
