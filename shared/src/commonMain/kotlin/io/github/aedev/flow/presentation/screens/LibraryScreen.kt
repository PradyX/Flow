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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.downloads.Downloads
import io.github.aedev.flow.data.library.LibraryItem
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.local.LocalMediaItem
import io.github.aedev.flow.data.playlists.UserPlaylists
import io.github.aedev.flow.presentation.feed.LibraryFeed
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadLibrary
import io.github.aedev.flow.presentation.local.isMediaImportSupported
import io.github.aedev.flow.presentation.ui.AddToPlaylistDialog
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_download
import io.github.aedev.flow.resources.action_downloaded
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.action_search
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.resources.history_title
import io.github.aedev.flow.resources.library_downloads
import io.github.aedev.flow.resources.library_empty
import io.github.aedev.flow.resources.library_filter_all
import io.github.aedev.flow.resources.library_import
import io.github.aedev.flow.resources.library_local
import io.github.aedev.flow.resources.library_local_empty
import io.github.aedev.flow.resources.library_local_remove
import io.github.aedev.flow.resources.library_title
import io.github.aedev.flow.resources.playlist_add_to
import io.github.aedev.flow.resources.playlists_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val LibraryContentPadding = 12.dp
private val LibraryArtwork = RoundedCornerShape(8.dp)
private const val LIBRARY_ARTWORK_SIZE = 56
private const val LIBRARY_ROW_SPACING = 4

/**
 * The library: the saved and downloaded videos this device keeps in Room, the request the reference
 * app's library page makes, and YouTube's own sign-in notice when that request had no account behind
 * it. A row counts as downloaded only while the media cache really holds its bytes, and the
 * downloads filter reads that same record.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun LibraryScreen(
    store: LibraryStore,
    /** The videos whose media is on disk, and how far the ones being fetched have got. */
    downloads: Downloads = Downloads(),
    downloadProgress: Map<String, Float> = emptyMap(),
    /** The last download that failed here, shown above the list until the next attempt. */
    downloadError: String? = null,
    onToggleDownload: (LibraryItem) -> Unit = {},
    onSearch: () -> Unit = {},
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenHistory: () -> Unit = {},
    /** The playlists this device made, so a saved row can be put on one. */
    playlists: UserPlaylists = UserPlaylists(),
    onOpenPlaylists: () -> Unit = {},
    /** Puts the video on a playlist it is not on, or takes it off one it is. */
    onToggleInPlaylist: (playlistId: String, item: LibraryItem) -> Unit = { _, _ -> },
    /** Makes a playlist with this video on it. */
    onCreatePlaylistWith: (name: String, item: LibraryItem) -> Unit = { _, _ -> },
    /** The files this device imported, newest first. */
    localMedia: List<LocalMediaItem> = emptyList(),
    /** Opens the device's picker; the host adds what comes back to [localMedia]. */
    onImportMedia: () -> Unit = {},
    /** Plays an imported file through the same player, and the same session, as a video. */
    onOpenLocal: (LocalMediaItem) -> Unit = {},
    onRemoveLocal: (LocalMediaItem) -> Unit = {},
) {
    var state by remember { mutableStateOf<LoadState<LibraryFeed>>(LoadState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(emptyList<LibraryItem>()) }
    var filter by remember { mutableStateOf(LibraryFilter.All) }
    var addingToPlaylist by remember { mutableStateOf<LibraryItem?>(null) }
    val scope = rememberCoroutineScope()
    val downloadedIds = downloads.entries.map { it.videoId }

    LaunchedEffect(attempt) {
        saved = store.items()
        state =
            loadLibrary().fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    // A download that lands adds its own row, so the list re-reads the store when the set changes.
    LaunchedEffect(downloadedIds) {
        saved = store.items()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.library_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            actions = {
                if (isMediaImportSupported) {
                    IconButton(onClick = onImportMedia) {
                        Icon(
                            imageVector = FlowIcons.Add,
                            contentDescription = stringResource(Res.string.library_import),
                        )
                    }
                }
                IconButton(onClick = onOpenPlaylists) {
                    Icon(
                        imageVector = FlowIcons.Checklist,
                        contentDescription = stringResource(Res.string.playlists_title),
                    )
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        imageVector = FlowIcons.History,
                        contentDescription = stringResource(Res.string.history_title),
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = FlowIcons.Search,
                        contentDescription = stringResource(Res.string.action_search),
                    )
                }
            },
        )
        // The remote half of the page needs a network; the rows on this device do not, so the same
        // body draws either, with an offline feed that has no notice to show.
        val body: @Composable (LibraryFeed) -> Unit = { feed ->
            LibraryBody(
                feed = feed,
                saved = saved,
                downloads = downloads,
                downloadProgress = downloadProgress,
                downloadError = downloadError,
                filter = filter,
                onFilter = { filter = it },
                localMedia = localMedia,
                onOpenLocal = onOpenLocal,
                onRemoveLocal = onRemoveLocal,
                onOpenVideo = onOpenVideo,
                onToggleDownload = { item ->
                    onToggleDownload(item)
                    scope.launch { saved = store.items() }
                },
                onAddToPlaylist = { item -> addingToPlaylist = item },
            )
        }
        when (val current = state) {
            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                if (saved.isNotEmpty()) {
                    body(LibraryFeed(entries = emptyList()))
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.feed_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { attempt++ }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }
            }

            is LoadState.Ready -> {
                body(current.value)
            }
        }
    }

    addingToPlaylist?.let { item ->
        AddToPlaylistDialog(
            playlists = playlists,
            videoId = item.videoId,
            onToggle = { playlistId -> onToggleInPlaylist(playlistId, item) },
            onCreate = { name -> onCreatePlaylistWith(name, item) },
            onDismiss = { addingToPlaylist = null },
        )
    }
}

@Composable
private fun LibraryBody(
    feed: LibraryFeed,
    saved: List<LibraryItem>,
    downloads: Downloads,
    downloadProgress: Map<String, Float>,
    downloadError: String?,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    localMedia: List<LocalMediaItem>,
    onOpenLocal: (LocalMediaItem) -> Unit,
    onRemoveLocal: (LocalMediaItem) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onToggleDownload: (LibraryItem) -> Unit,
    onAddToPlaylist: (LibraryItem) -> Unit,
) {
    val visible =
        when (filter) {
            LibraryFilter.All -> saved
            LibraryFilter.Downloads -> saved.filter { downloads.contains(it.videoId) }
            LibraryFilter.Local -> emptyList()
        }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(LIBRARY_ROW_SPACING.dp),
    ) {
        feed.notice?.let { notice ->
            item("notice") {
                LibraryNotice(notice = notice, subtext = feed.noticeSubtext)
            }
        }
        downloadError?.takeIf { it.isNotBlank() }?.let { message ->
            item("download-error") {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = LibraryContentPadding, vertical = 4.dp),
                )
            }
        }
        item("filters") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = LibraryContentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == LibraryFilter.All,
                    onClick = { onFilter(LibraryFilter.All) },
                    label = { Text(stringResource(Res.string.library_filter_all)) },
                )
                FilterChip(
                    selected = filter == LibraryFilter.Downloads,
                    onClick = { onFilter(LibraryFilter.Downloads) },
                    label = { Text(stringResource(Res.string.library_downloads)) },
                )
                FilterChip(
                    selected = filter == LibraryFilter.Local,
                    onClick = { onFilter(LibraryFilter.Local) },
                    label = { Text(stringResource(Res.string.library_local)) },
                )
            }
        }
        if (filter == LibraryFilter.Local) {
            if (localMedia.isEmpty()) {
                item("local-empty") {
                    Text(
                        text = stringResource(Res.string.library_local_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                    )
                }
            } else {
                items(items = localMedia, key = { it.uri }) { item ->
                    LocalMediaRow(
                        item = item,
                        onOpen = { onOpenLocal(item) },
                        onRemove = { onRemoveLocal(item) },
                    )
                }
            }
        } else if (visible.isEmpty()) {
            item("empty") {
                Text(
                    text = stringResource(Res.string.library_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                )
            }
        } else {
            items(items = visible, key = { it.videoId }) { item ->
                LibraryItemRow(
                    item = item,
                    isDownloaded = downloads.contains(item.videoId),
                    downloadProgress = downloadProgress[item.videoId],
                    onOpen = { onOpenVideo(item.videoId, item.title, item.thumbnailUrl) },
                    onToggleDownload = { onToggleDownload(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                )
            }
        }
    }
}

@Composable
private fun LibraryNotice(
    notice: String,
    subtext: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = LibraryContentPadding),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimensions.SectionSpacing)) {
            Text(
                text = notice,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtext?.let { line ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocalMediaRow(
    item: LocalMediaItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val supporting =
        listOfNotNull(
            item.sizeBytes.takeIf { it > 0 }?.let(::mediaSize),
            item.mimeType
                ?.substringAfter('/')
                ?.uppercase()
                ?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    ListItem(
        headlineContent = {
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (supporting.isNotBlank()) {
                Text(
                    text = supporting,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (item.mimeType?.startsWith("audio/") == true) FlowIcons.MusicNote else FlowIcons.VideoFrame,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = FlowIcons.Delete,
                    contentDescription = stringResource(Res.string.library_local_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    )
}

/** A file size as the row prints it: one decimal from KB up, whole bytes below that. */
private fun mediaSize(bytes: Long): String =
    when {
        bytes >= 1_000_000_000 -> "${(bytes / 100_000_000) / 10.0} GB"
        bytes >= 1_000_000 -> "${(bytes / 100_000) / 10.0} MB"
        bytes >= 1_000 -> "${bytes / 1_000} KB"
        else -> "$bytes B"
    }

/** What the chips above the list narrow it to; Local is the device's own imported files. */
private enum class LibraryFilter { All, Downloads, Local }

@Composable
private fun LibraryItemRow(
    item: LibraryItem,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onOpen: () -> Unit,
    onToggleDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            item.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                Text(
                    text = channel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(LIBRARY_ARTWORK_SIZE.dp).clip(LibraryArtwork),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = FlowIcons.AddToQueue,
                        contentDescription = stringResource(Res.string.playlist_add_to),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleDownload, enabled = downloadProgress == null) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(LIBRARY_ARTWORK_SIZE.dp / 2),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = FlowIcons.Download,
                            contentDescription =
                                stringResource(
                                    if (isDownloaded) Res.string.action_downloaded else Res.string.action_download,
                                ),
                            tint =
                                if (isDownloaded) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    )
}
