package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.subscriptions.loadSubscribedArtistIds
import io.github.aedev.flow.data.subscriptions.rememberArtistSubscription
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.presentation.feed.ArtistFeed
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadArtist
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.LocalShareAction
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.action_share
import io.github.aedev.flow.resources.artist_failed
import io.github.aedev.flow.resources.artist_subscribe
import io.github.aedev.flow.resources.artist_subscribed
import io.github.aedev.flow.resources.artist_subscribers
import io.github.aedev.flow.resources.artist_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * A music artist: the full-bleed hero the immersive header carries, with the back and share actions
 * over it and the name, audience line and subscribe pill on a sheet over its lower edge, then the
 * description and the artist's own shelves. A ranked shelf is a list of rows; every other shelf is
 * the same card row the music tab uses. Every row and card opens its own destination.
 */
@Composable
internal fun ArtistScreen(
    artistId: String,
    fallbackName: String,
    fallbackThumbnailUrl: String?,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    onBack: () -> Unit,
    /** A relaunch reopened this page: if it no longer resolves, the tab beneath it takes over. */
    onUnavailable: (() -> Unit)? = null,
) {
    var state by remember(artistId) { mutableStateOf<LoadState<ArtistFeed>>(LoadState.Loading) }
    var attempt by remember(artistId) { mutableIntStateOf(0) }
    var subscribed by remember(artistId) { mutableStateOf(false) }
    var pendingSubscribe by remember(artistId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val share = LocalShareAction.current

    LaunchedEffect(artistId, attempt) {
        state = LoadState.Loading
        state =
            loadArtist(artistId).fold(
                onSuccess = {
                    // YouTube's own answer first, then what the last run stored: an anonymous
                    // visitor's subscribe has no account behind it, so the app keeps it.
                    val stored = it.channelId != null && it.channelId in loadSubscribedArtistIds()
                    subscribed = it.isSubscribed || stored
                    LoadState.Ready(it)
                },
                onFailure = { error ->
                    onUnavailable?.invoke()
                    LoadState.Failed(error.message.orEmpty())
                },
            )
    }

    when (val current = state) {
        LoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is LoadState.Failed -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.artist_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { attempt++ }) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }

        is LoadState.Ready -> {
            val artist =
                current.value.copy(
                    name = current.value.name.ifBlank { fallbackName },
                    thumbnailUrl = current.value.thumbnailUrl ?: fallbackThumbnailUrl,
                )
            ArtistBody(
                artist = artist,
                subscribed = subscribed,
                subscribeEnabled = artist.channelId != null && !pendingSubscribe,
                onSubscribeClick = {
                    val channelId = artist.channelId ?: return@ArtistBody
                    val target = !subscribed
                    // The pill flips at once, the way the channel page's does; the network call is
                    // best effort because an anonymous visitor has nothing to subscribe with.
                    subscribed = target
                    rememberArtistSubscription(channelId, target)
                    pendingSubscribe = true
                    scope.launch {
                        YouTube.subscribeChannel(channelId, target)
                        pendingSubscribe = false
                    }
                },
                onShareClick = {
                    val link =
                        artist.channelId
                            ?.let { "https://music.youtube.com/channel/$it" }
                            ?: "https://music.youtube.com/browse/$artistId"
                    share(artist.name, link)
                },
                onBack = onBack,
                onOpenVideo = onOpenVideo,
                onOpenPlaylist = onOpenPlaylist,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}

@Composable
private fun ArtistBody(
    artist: ArtistFeed,
    subscribed: Boolean,
    subscribeEnabled: Boolean,
    onSubscribeClick: () -> Unit,
    onShareClick: () -> Unit,
    onBack: () -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(MusicSectionSpacing),
    ) {
        item("hero") {
            ArtistHero(
                artist = artist,
                subscribed = subscribed,
                subscribeEnabled = subscribeEnabled,
                onSubscribeClick = onSubscribeClick,
                onShareClick = onShareClick,
                onBack = onBack,
            )
        }

        artist.description?.takeIf { it.isNotBlank() }?.let { text ->
            item("bio") {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Dimensions.ContentPaddingHorizontal),
                )
            }
        }

        artist.shelves.forEach { shelf ->
            item("heading-${shelf.title}") {
                Text(
                    text = shelf.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = Dimensions.ContentPaddingHorizontal),
                )
            }
            if (shelf.isList) {
                itemsIndexed(shelf.entries, key = { index, entry -> "row-${shelf.title}-$index-${entry.id}" }) { index, entry ->
                    MusicTrackRow(
                        rank = index + 1,
                        entry = entry,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenVideo = onOpenVideo,
                        onOpenArtist = onOpenArtist,
                    )
                }
            } else {
                item("carousel-${shelf.title}") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(MusicCardSpacing),
                    ) {
                        items(shelf.entries, key = { "${shelf.title}-${it.id}" }) { entry ->
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
        }
    }
}

/**
 * The immersive header: the artist's photo edge to edge, the back and share controls over its top,
 * and the name, audience and subscribe pill on a surface that sits over its lower edge — the
 * reference's hero, with the name block riding on the artwork instead of beside it.
 */
@Composable
private fun ArtistHero(
    artist: ArtistFeed,
    subscribed: Boolean,
    subscribeEnabled: Boolean,
    onSubscribeClick: () -> Unit,
    onShareClick: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(HeroHeight)) {
        MediaArtwork(
            title = artist.name,
            imageUrl = artist.thumbnailUrl,
            shape = RectangleShape,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(HeroControlPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(
                    imageVector = FlowIcons.Back,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
            FilledTonalIconButton(onClick = onShareClick) {
                Icon(
                    imageVector = FlowIcons.Share,
                    contentDescription = stringResource(Res.string.action_share),
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape =
                MaterialTheme.shapes.extraLarge.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                ),
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(HeroSheetPadding)) {
                Text(
                    text = artist.name.ifBlank { stringResource(Res.string.artist_title) },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subscribers =
                    artist.subscriberText?.takeIf { it.isNotBlank() }?.let { text ->
                        // The header gives a bare count; the app names what it counts.
                        if (text.contains(' ')) text else stringResource(Res.string.artist_subscribers, text)
                    }
                val audience =
                    listOfNotNull(subscribers, artist.monthlyListenersText)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                if (audience.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = audience,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (artist.channelId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onSubscribeClick, enabled = subscribeEnabled) {
                        Text(
                            text =
                                if (subscribed) {
                                    stringResource(Res.string.artist_subscribed)
                                } else {
                                    stringResource(Res.string.artist_subscribe)
                                },
                        )
                    }
                }
            }
        }
    }
}

private val HeroHeight = 320.dp
private val HeroControlPadding = 12.dp
private val HeroSheetPadding = 20.dp
