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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.MoodGenre
import io.github.aedev.flow.presentation.feed.MusicSection
import io.github.aedev.flow.presentation.feed.MusicSectionShape
import io.github.aedev.flow.presentation.ui.FlowSegmentedGap
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.MusicMoodButton
import io.github.aedev.flow.presentation.ui.MusicMoodTone
import io.github.aedev.flow.presentation.ui.flowRowGroupShape
import io.github.aedev.flow.ui.theme.Dimensions

private const val DENSE_TILE_ROWS = 3

/** How many song rows a browse page's track lane stacks before it scrolls sideways. */
private const val TRACK_LANE_ROWS = 4

/** How far a song row's byline fades into the row's container, as the app's compact row does. */
private const val TRACK_LANE_SUBTITLE_ALPHA = 0.8f

/** A track lane's narrowest and widest column, and the strip of the next column left in view. */
internal val TrackLaneMinWidth = 240.dp
internal val TrackLaneMaxWidth = 360.dp
internal val TrackLanePeek = 48.dp

/** How many rows of mood buttons the shelf shows before it scrolls sideways. */
private const val MOOD_ROWS = 3

/**
 * A shelf's body, in the shape its entries call for: dense tiles for mixes, portraits for artists,
 * and cards for everything else.
 */
@Composable
internal fun MusicSectionBody(
    section: MusicSection,
    tileWidth: Dp,
    moodWidth: Dp,
    trackLaneWidth: Dp,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    onOpenMood: (MoodGenre) -> Unit,
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

        MusicSectionShape.MoodButtons -> {
            MusicMoodGrid(
                moods = section.moods,
                moodWidth = moodWidth,
                onOpenMood = onOpenMood,
            )
        }

        MusicSectionShape.TrackLane -> {
            MusicCompactTrackLane(
                entries = section.entries,
                laneWidth = trackLaneWidth,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}

/**
 * Four rows of compact song rows, scrolled sideways and segmented down each column — the shape the
 * app's browse body gives a shelf of songs.
 */
@Composable
private fun MusicCompactTrackLane(
    entries: List<FeedEntry>,
    laneWidth: Dp,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(TRACK_LANE_ROWS),
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.DenseTileGap),
        verticalArrangement = Arrangement.spacedBy(FlowSegmentedGap),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimensions.ListItemHeight * TRACK_LANE_ROWS + FlowSegmentedGap * (TRACK_LANE_ROWS - 1)),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
            val columnStart = (index / TRACK_LANE_ROWS) * TRACK_LANE_ROWS
            val rowsInColumn = minOf(TRACK_LANE_ROWS, entries.size - columnStart)
            MusicCompactTrackRow(
                entry = entry,
                width = laneWidth,
                shape = flowRowGroupShape(index = index - columnStart, count = rowsInColumn),
                open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist),
            )
        }
    }
}

/** One song row: a rounded still, its title and byline, in the row's slice of its column's group. */
@Composable
private fun MusicCompactTrackRow(
    entry: FeedEntry,
    width: Dp,
    shape: Shape,
    open: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .width(width)
                .height(Dimensions.ListItemHeight)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = open != null) { open?.invoke() }
                .padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaArtwork(
            title = entry.title,
            imageUrl = entry.thumbnailUrl,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(Dimensions.ListThumbnailSize),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.subtitle?.takeIf { it.isNotBlank() }?.let { line ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = TRACK_LANE_SUBTITLE_ALPHA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Three rows of mood buttons, scrolled sideways, each the shelf's own cell wide. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicMoodGrid(
    moods: List<MoodGenre>,
    moodWidth: Dp,
    onOpenMood: (MoodGenre) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(MOOD_ROWS),
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.ItemSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimensions.ItemSpacing),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ButtonDefaults.MediumContainerHeight * MOOD_ROWS + Dimensions.ItemSpacing * (MOOD_ROWS - 1)),
    ) {
        itemsIndexed(moods, key = { _, mood -> mood.id }) { index, mood ->
            MusicMoodButton(
                title = mood.title,
                onClick = { onOpenMood(mood) },
                width = moodWidth,
                tone = MusicMoodTone.forIndex(index),
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
