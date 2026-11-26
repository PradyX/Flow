package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.MusicSection
import io.github.aedev.flow.ui.theme.Dimensions

/**
 * YouTube Music's shelves as the music surfaces draw them: a heading over a row of square cards.
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(MusicSectionSpacing),
    ) {
        itemsIndexed(sections) { index, section ->
            Column {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = Dimensions.ContentPaddingHorizontal),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(MusicCardSpacing),
                ) {
                    itemsIndexed(section.entries) { cardIndex, entry ->
                        MusicCard(
                            entry = entry,
                            onOpenPlaylist = onOpenPlaylist,
                            onOpenVideo = onOpenVideo,
                            onOpenArtist = onOpenArtist,
                        )
                    }
                }
            }
        }

        if (continuation != null) {
            item {
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
