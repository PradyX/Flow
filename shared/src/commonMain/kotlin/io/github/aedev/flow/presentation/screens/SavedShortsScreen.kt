package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.library.LibraryItem
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.saved_shorts_empty
import io.github.aedev.flow.resources.saved_shorts_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/** The grid's tag, so a test can find the collection the reel saved into. */
internal const val SAVED_SHORTS_GRID_TEST_TAG = "saved_shorts_grid"

/** One saved short's card, tagged so a test can open the short it names. */
internal fun savedShortTag(videoId: String): String = "saved_short_$videoId"

private val SavedShortsPadding = 12.dp
private val SavedShortsSpacing = 8.dp
private val SavedShortsMinWidth = 140.dp

/**
 * The shorts saved from the reel, as their own collection: the library keeps them beside the
 * videos, and this page draws the ones that came from Shorts as the 9:16 cards the reel uses.
 * Opening one plays it in the same session the rest of the app uses.
 */
@Composable
internal fun SavedShortsScreen(
    store: LibraryStore,
    onBack: () -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    var shorts by remember { mutableStateOf(emptyList<LibraryItem>()) }

    LaunchedEffect(store) {
        shorts = store.savedShorts()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.saved_shorts_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FlowIcons.Back,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
            },
        )
        if (shorts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.saved_shorts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(SavedShortsMinWidth),
                modifier = Modifier.fillMaxSize().testTag(SAVED_SHORTS_GRID_TEST_TAG),
                contentPadding = PaddingValues(SavedShortsPadding),
                horizontalArrangement = Arrangement.spacedBy(SavedShortsSpacing),
                verticalArrangement = Arrangement.spacedBy(SavedShortsSpacing),
            ) {
                items(shorts, key = { it.videoId }) { short ->
                    SavedShortCard(
                        short = short,
                        onOpen = { onOpenVideo(short.videoId, short.title, short.thumbnailUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedShortCard(
    short: LibraryItem,
    onOpen: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .testTag(savedShortTag(short.videoId)),
    ) {
        AsyncImage(
            model = short.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Spacer(modifier = Modifier.height(SavedShortsSpacing))
        Text(
            text = short.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        short.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
            Text(
                text = channel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
