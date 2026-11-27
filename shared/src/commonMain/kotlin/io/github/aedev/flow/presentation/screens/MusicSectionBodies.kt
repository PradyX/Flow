package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.MoodGenre
import io.github.aedev.flow.presentation.feed.MusicSection
import io.github.aedev.flow.presentation.feed.MusicSectionShape
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.MusicMoodButton
import io.github.aedev.flow.presentation.ui.MusicMoodTone
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.ui.theme.Dimensions

private const val DENSE_TILE_ROWS = 3

/** How many rows of mood buttons the shelf shows before it scrolls sideways. */
private const val MOOD_ROWS = 3

/**
 * The charts lane: the app's own column width and peek, the four ranked rows a column stacks, and
 * the geometry every one of those rows shares.
 */
private val ChartLaneCardWidth = 320.dp
private val ChartLanePeek = 56.dp
private val ChartLaneColumnGap = 8.dp
private val ChartLaneRowGap = 2.dp
private val ChartLaneRowHeight = 64.dp
private val ChartLaneInset = 12.dp
private val ChartRankWidth = 26.dp
private val ChartLaneArtwork = 48.dp
private val ChartLaneTextGap = 16.dp
private const val CHART_LANE_ROWS = 4
private const val CHART_LEADER_COUNT = 3

/**
 * A shelf's body, in the shape its entries call for: dense tiles for mixes, portraits for artists,
 * and cards for everything else.
 */
@Composable
internal fun MusicSectionBody(
    section: MusicSection,
    tileWidth: Dp,
    moodWidth: Dp,
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

        MusicSectionShape.RankedLane -> {
            MusicRankedLane(
                entries = section.entries,
                onOpenPlaylist = onOpenPlaylist,
                onOpenVideo = onOpenVideo,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}

/**
 * The charts lane: each item the app's wide chart card, a rank, the artwork, then the track and its
 * byline under it. The card takes the lane width less one peek, up to the app's own card width.
 */
@Composable
private fun MusicRankedLane(
    entries: List<FeedEntry>,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    val columnWidth =
        with(LocalDensity.current) {
            (
                LocalWindowInfo.current.containerSize.width
                    .toDp() - ChartLanePeek
            ).coerceAtMost(ChartLaneCardWidth)
        }
    val columns = entries.chunked(CHART_LANE_ROWS)
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(ChartLaneColumnGap),
    ) {
        itemsIndexed(columns, key = { index, _ -> index }) { columnIndex, column ->
            Column(verticalArrangement = Arrangement.spacedBy(ChartLaneRowGap)) {
                column.forEachIndexed { rowIndex, entry ->
                    val rank = columnIndex * CHART_LANE_ROWS + rowIndex + 1
                    val open = musicEntryClick(entry, onOpenPlaylist, onOpenVideo, onOpenArtist)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .width(columnWidth)
                                .height(ChartLaneRowHeight)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable(enabled = open != null) { open?.invoke() }
                                .padding(horizontal = ChartLaneInset),
                    ) {
                        Box(modifier = Modifier.width(ChartRankWidth)) {
                            Text(
                                text = chartRankLabel(rank),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (rank <= CHART_LEADER_COUNT) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                        MediaArtwork(
                            title = entry.title,
                            imageUrl = entry.thumbnailUrl,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(ChartLaneArtwork),
                        )
                        Spacer(modifier = Modifier.width(ChartLaneTextGap))
                        Column {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            chartLaneByline(entry)?.let { line ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (entry.explicit) {
                                        MusicExplicitBadge()
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
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
                }
            }
        }
    }
}

/** The lane's second line: the byline the app prints, then the track's length after a bullet. */
private fun chartLaneByline(entry: FeedEntry): String? {
    val parts = listOfNotNull(entry.subtitle?.takeIf { it.isNotBlank() }, entry.durationSeconds?.let(::durationText))
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

/** The rank the app prints before a chart card's artwork. */
private fun chartRankLabel(rank: Int): String = "#$rank"

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
                    modifier = Modifier.fillMaxWidth(),
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
