package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.MoodGenre
import io.github.aedev.flow.presentation.feed.MusicChip
import io.github.aedev.flow.presentation.feed.MusicSection
import io.github.aedev.flow.presentation.feed.MusicSectionShape
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.gridCellWidth
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.section_mood_and_genres
import io.github.aedev.flow.resources.ui_navigate
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

private const val DENSE_TILE_COLUMNS = 3
private val DenseTileColumnsMinimumWidth = 360.dp

/**
 * YouTube Music's page as the music surfaces draw it: the response's own chips over its shelves,
 * each shelf a heading and the body its entries call for.
 *
 * The heading carries the app's own section header: 12dp of padding above and below the title on
 * the 12dp content inset, in the emphasized large title. Measured on the reference's music page,
 * its heading text sits 16px under the chip row and the shelf starts 16px under the heading, with
 * every following heading 16px under the row before it.
 *
 * The footer asks for the next page as it comes into view, which is how the app keeps its carousels
 * coming while the page still has a token.
 */
@Composable
internal fun MusicSectionList(
    sections: List<MusicSection>,
    continuation: String?,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    onOpenMood: (MoodGenre) -> Unit = {},
    onSeeAllMoods: () -> Unit = {},
    chips: List<MusicChip> = emptyList(),
    selectedChip: MusicChip? = null,
    onChipSelected: (MusicChip?) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentWidth = maxWidth - Dimensions.ContentPaddingHorizontal * 2
        val columns = if (contentWidth >= DenseTileColumnsMinimumWidth) DENSE_TILE_COLUMNS else 2
        val tileWidth = gridCellWidth(contentWidth, columns, Dimensions.DenseTileGap)
        // The app sizes the mood buttons by window class: two across a phone, three on a medium
        // window, four on an expanded one.
        val moodColumns =
            when {
                maxWidth >= 840.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }
        val moodWidth = gridCellWidth(contentWidth, moodColumns, Dimensions.ItemSpacing)
        // The app sizes a track lane's column to the window less its insets and the next column's
        // peek, capped at 360dp: on a medium window one column of four rows fills the lane.
        val trackLaneWidth =
            (maxWidth - Dimensions.ContentPaddingHorizontal * 2 - TrackLanePeek)
                .coerceIn(TrackLaneMinWidth, TrackLaneMaxWidth)

        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            if (chips.isNotEmpty()) {
                item(key = "music_chips") {
                    MusicChipRow(
                        chips = chips,
                        selected = selectedChip,
                        onChipSelected = onChipSelected,
                    )
                }
            }

            itemsIndexed(sections, key = { index, _ -> "music_section:$index" }) { _, section ->
                Column {
                    if (section.shape == MusicSectionShape.MoodButtons) {
                        MoodsHeader(onSeeAll = onSeeAllMoods)
                    } else {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.padding(
                                    start = Dimensions.ContentPaddingHorizontal,
                                    end = Dimensions.ContentPaddingHorizontal,
                                    top = Dimensions.ContentPaddingVertical,
                                    bottom = Dimensions.ContentPaddingVertical,
                                ),
                        )
                    }
                    MusicSectionBody(
                        section = section,
                        tileWidth = tileWidth,
                        moodWidth = moodWidth,
                        trackLaneWidth = trackLaneWidth,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenVideo = onOpenVideo,
                        onOpenArtist = onOpenArtist,
                        onOpenMood = onOpenMood,
                    )
                }
            }

            if (continuation != null) {
                item(key = "music_more") {
                    LaunchedEffect(continuation) {
                        if (!loadingMore) onLoadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

/**
 * The moods shelf's heading: the section title and the arrow that opens the whole index, on the
 * row the whole header is clickable along — the app's own `MusicMoodsShelf` header.
 */
@Composable
private fun MoodsHeader(onSeeAll: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeeAll)
                .padding(
                    horizontal = Dimensions.ContentPaddingHorizontal,
                    vertical = Dimensions.ContentPaddingVertical,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.section_mood_and_genres),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            imageVector = FlowIcons.ArrowForward,
            contentDescription = stringResource(Res.string.ui_navigate),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The page's own chips, one row tall and scrolled sideways. Choosing one asks for the page it
 * names; choosing the selected chip again clears the choice, which is how the app returns to the
 * page it started on.
 */
@Composable
private fun MusicChipRow(
    chips: List<MusicChip>,
    selected: MusicChip?,
    onChipSelected: (MusicChip?) -> Unit,
) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.DenseTileGap),
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.DenseTileGap),
    ) {
        items(chips, key = { it.title }) { chip ->
            val isSelected = selected?.title == chip.title
            FilterChip(
                selected = isSelected,
                onClick = { onChipSelected(if (isSelected) null else chip) },
                label = {
                    Text(
                        text = chip.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.height(Dimensions.MoodButtonHeight),
            )
        }
    }
}
