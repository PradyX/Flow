package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.library.LibraryItem
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.music.loadPlaylistSortOrder
import io.github.aedev.flow.data.music.savePlaylistSortOrder
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.PlaylistFeed
import io.github.aedev.flow.presentation.feed.loadPlaylist
import io.github.aedev.flow.presentation.feed.loadPlaylistSongs
import io.github.aedev.flow.presentation.feed.videoWatchUrl
import io.github.aedev.flow.presentation.playback.QueueTrack
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.LocalShareAction
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.WideLayoutMinWidth
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_copy_link
import io.github.aedev.flow.resources.action_download
import io.github.aedev.flow.resources.action_downloaded
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.action_save
import io.github.aedev.flow.resources.action_saved
import io.github.aedev.flow.resources.action_share
import io.github.aedev.flow.resources.music_explicit_badge
import io.github.aedev.flow.resources.playlist_add_to_queue
import io.github.aedev.flow.resources.playlist_deselect_all
import io.github.aedev.flow.resources.playlist_empty
import io.github.aedev.flow.resources.playlist_failed
import io.github.aedev.flow.resources.playlist_load_more
import io.github.aedev.flow.resources.playlist_more_options
import io.github.aedev.flow.resources.playlist_open_artist
import io.github.aedev.flow.resources.playlist_play
import io.github.aedev.flow.resources.playlist_play_next
import io.github.aedev.flow.resources.playlist_search
import io.github.aedev.flow.resources.playlist_select_all
import io.github.aedev.flow.resources.playlist_select_songs
import io.github.aedev.flow.resources.playlist_selected_count
import io.github.aedev.flow.resources.playlist_shuffle
import io.github.aedev.flow.resources.playlist_sort_album_order
import io.github.aedev.flow.resources.playlist_sort_longest
import io.github.aedev.flow.resources.playlist_sort_order
import io.github.aedev.flow.resources.playlist_sort_shortest
import io.github.aedev.flow.resources.playlist_sort_title
import io.github.aedev.flow.resources.playlist_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val PlaylistHeroArtworkSize = 200.dp
private val PlaylistRowArtworkSize = 56.dp
private val HeaderPaneWidth = 360.dp
private val HeaderPanePadding = 16.dp
private val HeaderPaneInset = 24.dp
private val InlineExplicitBadgeSize = 14.dp
private val RowIndexWidth = 20.dp
private val ActionGap = 8.dp

/**
 * A playlist or album: the reference's centred header — large artwork, the type label, the title
 * with its explicit badge, the artist's name and the year · songs · length line — above the songs.
 * On a window wide enough for two partitions the header moves into the leading pane and the songs
 * fill the pane beside it, as the reference does. Under the metadata both layouts carry the
 * reference's Play/Shuffle buttons, its save, download, share and overflow controls, and the sort
 * chip that reorders the rows; every row opens the player for its own video and offers the same
 * save, download and share actions from its own overflow. The artist's name opens the artist page.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun PlaylistScreen(
    playlistId: String,
    fallbackTitle: String,
    libraryStore: LibraryStore,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String) -> Unit,
    /** Starts the page as a playback queue, in row order or shuffled, from [startIndex]. */
    onPlayCollection: (tracks: List<QueueTrack>, shuffled: Boolean, startIndex: Int) -> Unit,
    /** Extends the playing queue with these tracks, after the current one or at its end. */
    onAddToQueue: (tracks: List<QueueTrack>, next: Boolean) -> Unit,
    onBack: () -> Unit,
    /** A relaunch reopened this page: if it no longer resolves, the tab beneath it takes over. */
    onUnavailable: (() -> Unit)? = null,
) {
    var state by remember(playlistId) { mutableStateOf<LoadState<PlaylistFeed>>(LoadState.Loading) }
    var attempt by remember(playlistId) { mutableIntStateOf(0) }
    // The library rows this page's save and download controls read, and a version they bump after
    // each write so the row marks and the two buttons agree with the database.
    var libraryVersion by remember { mutableIntStateOf(0) }
    var libraryItems by remember { mutableStateOf(emptyList<LibraryItem>()) }
    val scope = rememberCoroutineScope()
    // Pages after the first, from the continuation the page was served with.
    var moreEntries by remember(playlistId) { mutableStateOf(emptyList<FeedEntry>()) }
    var continuation by remember(playlistId) { mutableStateOf<String?>(null) }
    var loadingMore by remember(playlistId) { mutableStateOf(false) }
    // Null outside selection mode; a set of row ids (possibly empty) while selecting.
    var selectedIds by remember(playlistId) { mutableStateOf<Set<String>?>(null) }
    // Null when the page is not being searched; the query filters the rows while it is.
    var searchQuery by remember(playlistId) { mutableStateOf<String?>(null) }

    LaunchedEffect(playlistId, attempt) {
        state =
            loadPlaylist(playlistId).fold(
                onSuccess = { feed ->
                    moreEntries = emptyList()
                    continuation = feed.continuation
                    if (onUnavailable != null && feed.entries.isEmpty()) {
                        onUnavailable()
                        LoadState.Failed("")
                    } else {
                        LoadState.Ready(feed)
                    }
                },
                onFailure = { error ->
                    onUnavailable?.invoke()
                    LoadState.Failed(error.message.orEmpty())
                },
            )
    }
    LaunchedEffect(libraryVersion) {
        libraryItems = libraryStore.items()
    }

    val savedIds = remember(libraryItems) { libraryItems.mapTo(mutableSetOf()) { it.videoId } }
    val downloadedIds =
        remember(libraryItems) {
            libraryItems.filterTo(mutableListOf()) { it.isDownloaded }.mapTo(mutableSetOf()) { it.videoId }
        }

    val readyFeed = (state as? LoadState.Ready)?.value
    val entries: List<FeedEntry> = remember(readyFeed, moreEntries) { (readyFeed?.entries.orEmpty()) + moreEntries }
    val allVideoIds = remember(entries) { entries.mapNotNullTo(mutableSetOf()) { it.videoId } }
    // The reference loads the next page as the list nears its end; here the list ends in a control
    // that asks for it, so a page of a long playlist is one tap away instead of a half-loaded list.
    val loadMore: () -> Unit = {
        val token = continuation
        if (token != null && !loadingMore) {
            loadingMore = true
            scope.launch {
                loadPlaylistSongs(token).onSuccess { page ->
                    moreEntries = moreEntries + page.entries
                    continuation = page.continuation
                }
                loadingMore = false
            }
        }
    }

    fun save(entry: FeedEntry) {
        val videoId = entry.videoId ?: return
        scope.launch {
            libraryStore.toggleSaved(
                videoId = videoId,
                title = entry.title,
                channelName = entry.subtitle,
                thumbnailUrl = entry.thumbnailUrl,
                nowMillis = Clock.System.now().toEpochMilliseconds(),
            )
            libraryVersion++
        }
    }

    fun download(entry: FeedEntry) {
        val videoId = entry.videoId ?: return
        scope.launch {
            libraryStore.toggleDownloaded(
                videoId = videoId,
                title = entry.title,
                channelName = entry.subtitle,
                thumbnailUrl = entry.thumbnailUrl,
                nowMillis = Clock.System.now().toEpochMilliseconds(),
            )
            libraryVersion++
        }
    }

    // The page's save and download controls act on every song: the library stores videos, so saving
    // an album means its songs are in the library and the bookmark reads saved when they all are.
    fun saveAll(
        entries: List<FeedEntry>,
        saved: Boolean,
    ) {
        scope.launch {
            entries.forEach { entry ->
                val videoId = entry.videoId ?: return@forEach
                if ((videoId in savedIds) != saved) {
                    libraryStore.toggleSaved(
                        videoId = videoId,
                        title = entry.title,
                        channelName = entry.subtitle,
                        thumbnailUrl = entry.thumbnailUrl,
                        nowMillis = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            }
            libraryVersion++
        }
    }

    fun downloadAll(
        entries: List<FeedEntry>,
        downloaded: Boolean,
    ) {
        scope.launch {
            entries.forEach { entry ->
                val videoId = entry.videoId ?: return@forEach
                if ((videoId in downloadedIds) != downloaded) {
                    libraryStore.toggleDownloaded(
                        videoId = videoId,
                        title = entry.title,
                        channelName = entry.subtitle,
                        thumbnailUrl = entry.thumbnailUrl,
                        nowMillis = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            }
            libraryVersion++
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val selection = selectedIds
        FlowTopBar(
            title = {
                if (searchQuery != null && selection == null) {
                    TextField(
                        value = searchQuery.orEmpty(),
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(Res.string.playlist_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (selection != null) {
                    Text(
                        text = pluralStringResource(Res.plurals.playlist_selected_count, selection.size, selection.size),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                } else {
                    Text(
                        text = readyFeed?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        when {
                            selection != null -> selectedIds = null
                            searchQuery != null -> searchQuery = null
                            else -> onBack()
                        }
                    },
                ) {
                    Icon(
                        imageVector = FlowIcons.Back,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
            },
            actions = {
                if (selection != null) {
                    val allSelected = allVideoIds.isNotEmpty() && selection.containsAll(allVideoIds)
                    IconButton(onClick = { selectedIds = if (allSelected) emptySet() else allVideoIds }) {
                        Icon(
                            imageVector = FlowIcons.Checklist,
                            contentDescription =
                                stringResource(
                                    if (allSelected) Res.string.playlist_deselect_all else Res.string.playlist_select_all,
                                ),
                        )
                    }
                } else if (entries.isNotEmpty()) {
                    if (searchQuery == null) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = FlowIcons.Search,
                                contentDescription = stringResource(Res.string.playlist_search),
                            )
                        }
                    }
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(
                            imageVector = FlowIcons.Checklist,
                            contentDescription = stringResource(Res.string.playlist_select_songs),
                        )
                    }
                }
            },
        )

        when (val current = state) {
            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                PlaylistMessage(
                    message = stringResource(Res.string.playlist_failed),
                    action = {
                        TextButton(onClick = { attempt++ }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    },
                )
            }

            is LoadState.Ready -> {
                if (current.value.entries.isEmpty()) {
                    PlaylistMessage(message = stringResource(Res.string.playlist_empty))
                } else {
                    PlaylistBody(
                        playlistId = playlistId,
                        playlist = current.value,
                        entries = entries,
                        query = searchQuery.orEmpty(),
                        savedIds = savedIds,
                        downloadedIds = downloadedIds,
                        selection = selection,
                        onToggleSelected = { entry ->
                            selectedIds =
                                selectedIds?.let { ids ->
                                    entry.videoId?.let { if (it in ids) ids - it else ids + it } ?: ids
                                }
                        },
                        hasMore = continuation != null,
                        loadingMore = loadingMore,
                        onLoadMore = loadMore,
                        onSave = ::save,
                        onDownload = ::download,
                        onSaveAll = ::saveAll,
                        onDownloadAll = ::downloadAll,
                        onOpenArtist = onOpenArtist,
                        onPlayCollection = onPlayCollection,
                        onAddToQueue = onAddToQueue,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistMessage(
    message: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

/** Everything the header's buttons and menus do, built once for both layouts. */
private class PlaylistActions(
    val onPlay: () -> Unit,
    val onShuffle: () -> Unit,
    val onShare: () -> Unit,
    val onCopyLink: () -> Unit,
    val onOpenArtist: (() -> Unit)?,
    val isSaved: Boolean,
    val isDownloaded: Boolean,
    val onSaveToggle: () -> Unit,
    val onDownloadToggle: () -> Unit,
)

@Composable
private fun PlaylistBody(
    playlistId: String,
    playlist: PlaylistFeed,
    entries: List<FeedEntry>,
    query: String,
    savedIds: Set<String>,
    downloadedIds: Set<String>,
    selection: Set<String>?,
    onToggleSelected: (FeedEntry) -> Unit,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onSave: (FeedEntry) -> Unit,
    onDownload: (FeedEntry) -> Unit,
    onSaveAll: (List<FeedEntry>, Boolean) -> Unit,
    onDownloadAll: (List<FeedEntry>, Boolean) -> Unit,
    onOpenArtist: (artistId: String, name: String) -> Unit,
    onPlayCollection: (tracks: List<QueueTrack>, shuffled: Boolean, startIndex: Int) -> Unit,
    onAddToQueue: (tracks: List<QueueTrack>, next: Boolean) -> Unit,
) {
    val share = LocalShareAction.current
    val clipboard = LocalClipboardManager.current
    var sort by remember(playlistId) { mutableStateOf(PlaylistSortOrder.fromStorage(loadPlaylistSortOrder(playlistId))) }
    val ordered = remember(entries, sort) { entries.sortedForPlaylist(sort) }
    val shown =
        remember(ordered, query) {
            val needle = query.trim()
            if (needle.isEmpty()) {
                ordered
            } else {
                ordered.filter { entry ->
                    entry.title.contains(needle, ignoreCase = true) ||
                        entry.subtitle?.contains(needle, ignoreCase = true) == true
                }
            }
        }
    val isAlbum = playlist.releaseType?.equals("Album", ignoreCase = true) == true
    val playlistUrl = remember(playlistId) { "https://music.youtube.com/playlist?list=$playlistId" }
    val shareUrl = { share(playlist.title, playlistUrl) }
    val copyUrl = { clipboard.setText(AnnotatedString(playlistUrl)) }
    val videoIds = remember(entries) { entries.mapNotNull { it.videoId } }
    val allSaved = videoIds.isNotEmpty() && videoIds.all { it in savedIds }
    val allDownloaded = videoIds.isNotEmpty() && videoIds.all { it in downloadedIds }

    val actions =
        PlaylistActions(
            onPlay = { onPlayCollection(ordered.queueTracks(), false, 0) },
            onShuffle = { onPlayCollection(ordered.queueTracks(), true, 0) },
            onShare = shareUrl,
            onCopyLink = copyUrl,
            onOpenArtist = playlist.authorId?.let { id -> { onOpenArtist(id, playlist.subtitle.orEmpty()) } },
            isSaved = allSaved,
            isDownloaded = allDownloaded,
            onSaveToggle = { onSaveAll(entries, !allSaved) },
            onDownloadToggle = { onDownloadAll(entries, !allDownloaded) },
        )

    val sortChip: @Composable () -> Unit = {
        PlaylistSortChip(
            isAlbum = isAlbum,
            selected = sort,
            onSelected = {
                sort = it
                savePlaylistSortOrder(playlistId, it.storageValue)
            },
        )
    }

    val rowShare: (FeedEntry) -> Unit = { entry -> share(entry.title, entry.shareUrl()) }
    val rowCopy: (FeedEntry) -> Unit = { entry -> clipboard.setText(AnnotatedString(entry.shareUrl())) }
    val queueRow: (FeedEntry, Boolean) -> Unit = { entry, next ->
        entry.videoId?.let { id -> onAddToQueue(listOf(QueueTrack(id, entry.title, entry.thumbnailUrl)), next) }
    }
    val selectedTracks = remember(ordered, selection) { selection?.let { ids -> ordered.filter { it.videoId in ids } }.orEmpty() }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= WideLayoutMinWidth) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PlaylistHeaderPane(
                        playlist = playlist,
                        actions = actions,
                        onOpenArtist = onOpenArtist,
                        modifier = Modifier.width(HeaderPaneWidth).fillMaxHeight(),
                    )
                    PlaylistTrackList(
                        entries = shown,
                        savedIds = savedIds,
                        downloadedIds = downloadedIds,
                        selection = selection,
                        onSave = onSave,
                        onDownload = onDownload,
                        sortChip = sortChip,
                        onOpenAt = { _, entry ->
                            if (selection != null) {
                                onToggleSelected(entry)
                            } else {
                                onPlayCollection(
                                    ordered.queueTracks(),
                                    false,
                                    ordered.indexOfFirst { it.id == entry.id }.coerceAtLeast(0),
                                )
                            }
                        },
                        onShare = rowShare,
                        onCopyLink = rowCopy,
                        onPlayNext = { entry -> queueRow(entry, true) },
                        onAddToQueue = { entry -> queueRow(entry, false) },
                        hasMore = hasMore,
                        loadingMore = loadingMore,
                        onLoadMore = onLoadMore,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item("header") {
                        PlaylistHeader(
                            playlist = playlist,
                            actions = actions,
                            sortChip = sortChip,
                            onOpenArtist = onOpenArtist,
                        )
                    }
                    itemsIndexed(shown, key = { _, entry -> entry.id }) { index, entry ->
                        PlaylistSongRow(
                            entry = entry,
                            index = index + 1,
                            selected = selection?.let { entry.videoId in it },
                            isSaved = entry.videoId in savedIds,
                            isDownloaded = entry.videoId in downloadedIds,
                            onClick = {
                                if (selection != null) {
                                    onToggleSelected(entry)
                                } else {
                                    onPlayCollection(
                                        ordered.queueTracks(),
                                        false,
                                        ordered.indexOfFirst { it.id == entry.id }.coerceAtLeast(0),
                                    )
                                }
                            },
                            onSave = onSave,
                            onDownload = onDownload,
                            onShare = { rowShare(entry) },
                            onCopyLink = { rowCopy(entry) },
                            onPlayNext = { queueRow(entry, true) },
                            onAddToQueue = { queueRow(entry, false) },
                        )
                    }
                    if (hasMore) {
                        item("more") { LoadMoreRow(loading = loadingMore, onLoadMore = onLoadMore) }
                    }
                }
            }
        }
        if (selectedTracks.isNotEmpty()) {
            PlaylistSelectionBar(
                count = selectedTracks.size,
                onPlayNext = { onAddToQueue(selectedTracks.queueTracks(), true) },
                onAddToQueue = { onAddToQueue(selectedTracks.queueTracks(), false) },
                onDownload = { onDownloadAll(selectedTracks, true) },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = Dimensions.SectionSpacing),
            )
        }
    }
}

@Composable
private fun PlaylistTrackList(
    entries: List<FeedEntry>,
    savedIds: Set<String>,
    downloadedIds: Set<String>,
    selection: Set<String>?,
    onSave: (FeedEntry) -> Unit,
    onDownload: (FeedEntry) -> Unit,
    sortChip: @Composable () -> Unit,
    onOpenAt: (Int, FeedEntry) -> Unit,
    onShare: (FeedEntry) -> Unit,
    onCopyLink: (FeedEntry) -> Unit,
    onPlayNext: (FeedEntry) -> Unit,
    onAddToQueue: (FeedEntry) -> Unit,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) { sortChip() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                PlaylistSongRow(
                    entry = entry,
                    index = index + 1,
                    selected = selection?.let { entry.videoId in it },
                    isSaved = entry.videoId in savedIds,
                    isDownloaded = entry.videoId in downloadedIds,
                    onClick = { onOpenAt(index, entry) },
                    onSave = onSave,
                    onDownload = onDownload,
                    onShare = { onShare(entry) },
                    onCopyLink = { onCopyLink(entry) },
                    onPlayNext = { onPlayNext(entry) },
                    onAddToQueue = { onAddToQueue(entry) },
                )
            }
            if (hasMore) {
                item("more") { LoadMoreRow(loading = loadingMore, onLoadMore = onLoadMore) }
            }
        }
    }
}

/** The list's last row while the page has another one behind it. */
@Composable
private fun LoadMoreRow(
    loading: Boolean,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            TextButton(onClick = onLoadMore) {
                Text(stringResource(Res.string.playlist_load_more))
            }
        }
    }
}

/**
 * The reference's selection toolbar: what can be done to the songs picked out of the list. It rests
 * on the bottom edge of the page while a selection exists.
 */
@Composable
private fun PlaylistSelectionBar(
    count: Int,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(Res.plurals.playlist_selected_count, count, count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onPlayNext) {
                Icon(
                    imageVector = FlowIcons.QueuePlayNext,
                    contentDescription = stringResource(Res.string.playlist_play_next),
                )
            }
            IconButton(onClick = onAddToQueue) {
                Icon(
                    imageVector = FlowIcons.AddToQueue,
                    contentDescription = stringResource(Res.string.playlist_add_to_queue),
                )
            }
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = FlowIcons.Download,
                    contentDescription = stringResource(Res.string.action_download),
                )
            }
        }
    }
}

/** The narrow-window header: everything centred over the songs. */
@Composable
private fun PlaylistHeader(
    playlist: PlaylistFeed,
    actions: PlaylistActions,
    sortChip: @Composable () -> Unit,
    onOpenArtist: (artistId: String, name: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = Dimensions.SectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaArtwork(
            title = playlist.title,
            imageUrl = playlist.thumbnailUrl,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(PlaylistHeroArtworkSize),
        )
        Spacer(modifier = Modifier.height(Dimensions.SectionSpacing))
        playlist.releaseType?.let { type ->
            Text(
                text = type,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = playlist.title.ifBlank { stringResource(Res.string.playlist_title) },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (playlist.explicit) {
                Spacer(modifier = Modifier.width(6.dp))
                MusicExplicitBadge()
            }
        }
        playlist.subtitle?.let { line ->
            Spacer(modifier = Modifier.height(4.dp))
            ArtistLine(playlist = playlist, line = line, onOpenArtist = onOpenArtist, centered = true)
        }
        playlist.details?.let { line ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(Dimensions.SectionSpacing))
        PlaylistPrimaryActions(actions = actions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistSecondaryActions(actions = actions)
            Spacer(modifier = Modifier.weight(1f))
            sortChip()
        }
    }
}

/**
 * The wide-window header: the same page in the leading pane — artwork, type label, title, artist
 * and metadata left-aligned — beside the song list, the reference's two-pane album page.
 */
@Composable
private fun PlaylistHeaderPane(
    playlist: PlaylistFeed,
    actions: PlaylistActions,
    onOpenArtist: (artistId: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier =
            modifier.padding(
                start = HeaderPanePadding,
                end = HeaderPanePadding,
                bottom = HeaderPanePadding,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(HeaderPaneInset),
            verticalArrangement = Arrangement.spacedBy(HeaderPanePadding),
        ) {
            // The cover gives way when the pane is short, so the actions under it stay on screen.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HeaderPanePadding),
            ) {
                MediaArtwork(
                    title = playlist.title,
                    imageUrl = playlist.thumbnailUrl,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f, fill = false).aspectRatio(1f),
                )
                playlist.releaseType?.let { type ->
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.title.ifBlank { stringResource(Res.string.playlist_title) },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (playlist.explicit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        MusicExplicitBadge()
                    }
                }
                playlist.subtitle?.let { line ->
                    ArtistLine(playlist = playlist, line = line, onOpenArtist = onOpenArtist, centered = false)
                }
                playlist.details?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PlaylistSecondaryActions(actions = actions)
            PlaylistPrimaryActions(actions = actions)
        }
    }
}

/** The reference's Play and Shuffle pair: a filled button and its tonal partner, side by side. */
@Composable
private fun PlaylistPrimaryActions(actions: PlaylistActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ActionGap),
    ) {
        Button(
            onClick = actions.onPlay,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ActionLabel(icon = FlowIcons.Play, label = stringResource(Res.string.playlist_play))
        }
        FilledTonalButton(
            onClick = actions.onShuffle,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ActionLabel(icon = FlowIcons.Shuffle, label = stringResource(Res.string.playlist_shuffle))
        }
    }
}

@Composable
private fun ActionLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

/**
 * The reference's row of icon controls beside the metadata: save, download, share and the page's
 * overflow. The save and download controls write the library's own rows for this page's songs.
 */
@Composable
private fun PlaylistSecondaryActions(actions: PlaylistActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.onSaveToggle) {
            Icon(
                imageVector = if (actions.isSaved) FlowIcons.Bookmark else FlowIcons.BookmarkBorder,
                contentDescription =
                    stringResource(if (actions.isSaved) Res.string.action_saved else Res.string.action_save),
                tint =
                    if (actions.isSaved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        IconButton(onClick = actions.onDownloadToggle) {
            Icon(
                imageVector = FlowIcons.Download,
                contentDescription =
                    stringResource(
                        if (actions.isDownloaded) Res.string.action_downloaded else Res.string.action_download,
                    ),
                tint =
                    if (actions.isDownloaded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        IconButton(onClick = actions.onShare) {
            Icon(imageVector = FlowIcons.Share, contentDescription = stringResource(Res.string.action_share))
        }
        PlaylistOverflowMenu(actions = actions)
    }
}

/** The header's ⋮: the actions that also live in a menu, in the reference's order. */
@Composable
private fun PlaylistOverflowMenu(actions: PlaylistActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = FlowIcons.MoreVert,
                contentDescription = stringResource(Res.string.playlist_more_options),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_copy_link)) },
                leadingIcon = { Icon(FlowIcons.Link, contentDescription = null) },
                onClick = {
                    open = false
                    actions.onCopyLink()
                },
            )
            actions.onOpenArtist?.let { openArtist ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.playlist_open_artist)) },
                    leadingIcon = { Icon(FlowIcons.MusicNote, contentDescription = null) },
                    onClick = {
                        open = false
                        openArtist()
                    },
                )
            }
        }
    }
}

/** The reference's sort chip: the current order's name with a menu of the orders this page has. */
@Composable
private fun PlaylistSortChip(
    isAlbum: Boolean,
    selected: PlaylistSortOrder,
    onSelected: (PlaylistSortOrder) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(sortLabel(selected, isAlbum)) },
            trailingIcon = { Icon(imageVector = FlowIcons.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PlaylistSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(sortLabel(order, isAlbum)) },
                    trailingIcon =
                        if (order == selected) {
                            { Icon(FlowIcons.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    onClick = {
                        open = false
                        onSelected(order)
                    },
                )
            }
        }
    }
}

@Composable
private fun sortLabel(
    order: PlaylistSortOrder,
    isAlbum: Boolean,
): String =
    when (order) {
        PlaylistSortOrder.COLLECTION -> {
            stringResource(if (isAlbum) Res.string.playlist_sort_album_order else Res.string.playlist_sort_order)
        }

        PlaylistSortOrder.TITLE -> {
            stringResource(Res.string.playlist_sort_title)
        }

        PlaylistSortOrder.LONGEST -> {
            stringResource(Res.string.playlist_sort_longest)
        }

        PlaylistSortOrder.SHORTEST -> {
            stringResource(Res.string.playlist_sort_shortest)
        }
    }

/** The artist's name under the title: a link when the header carried the artist's id. */
@Composable
private fun ArtistLine(
    playlist: PlaylistFeed,
    line: String,
    onOpenArtist: (artistId: String, name: String) -> Unit,
    centered: Boolean,
) {
    val artistId = playlist.authorId
    Text(
        text = line,
        style = MaterialTheme.typography.titleSmall,
        color =
            if (artistId != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.clickable(enabled = artistId != null) {
                artistId?.let { onOpenArtist(it, line) }
            },
    )
}

/** The app's explicit marker, drawn at [size] so a row can carry it without growing. */
@Composable
internal fun MusicExplicitBadge(size: androidx.compose.ui.unit.Dp = 18.dp) {
    Box(
        modifier =
            Modifier
                .size(size)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.music_explicit_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun FeedEntry.shareUrl(): String = videoId?.let(::videoWatchUrl) ?: ""

/** The rows that can play, in the order the page is showing them. */
private fun List<FeedEntry>.queueTracks(): List<QueueTrack> =
    mapNotNull { entry -> entry.videoId?.let { QueueTrack(it, entry.title, entry.thumbnailUrl) } }

@Composable
private fun PlaylistSongRow(
    entry: FeedEntry,
    index: Int,
    /** Null outside selection mode; otherwise whether this row is picked. */
    selected: Boolean?,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onSave: (FeedEntry) -> Unit,
    onDownload: (FeedEntry) -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = entry.title,
                maxLines = 2,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected != null) {
                    Checkbox(checked = selected, onCheckedChange = { onClick() })
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(RowIndexWidth),
                    )
                }
                MediaArtwork(
                    title = entry.title,
                    imageUrl = entry.thumbnailUrl,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(PlaylistRowArtworkSize),
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                entry.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                    Text(
                        text = durationText(seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected == null) {
                    PlaylistRowMenu(
                        isSaved = isSaved,
                        isDownloaded = isDownloaded,
                        onSave = { onSave(entry) },
                        onDownload = { onDownload(entry) },
                        onShare = onShare,
                        onCopyLink = onCopyLink,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                    )
                }
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = entry.videoId != null) { onClick() },
    )
}

/** A row's ⋮: the library's save and download rows, the song's link and the album's own links. */
@Composable
private fun PlaylistRowMenu(
    isSaved: Boolean,
    isDownloaded: Boolean,
    onSave: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = FlowIcons.MoreVert,
                contentDescription = stringResource(Res.string.playlist_more_options),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.playlist_play_next)) },
                leadingIcon = { Icon(FlowIcons.QueuePlayNext, contentDescription = null) },
                onClick = {
                    open = false
                    onPlayNext()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.playlist_add_to_queue)) },
                leadingIcon = { Icon(FlowIcons.AddToQueue, contentDescription = null) },
                onClick = {
                    open = false
                    onAddToQueue()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(if (isSaved) Res.string.action_saved else Res.string.action_save)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSaved) FlowIcons.Bookmark else FlowIcons.BookmarkBorder,
                        contentDescription = null,
                    )
                },
                onClick = {
                    open = false
                    onSave()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isDownloaded) Res.string.action_downloaded else Res.string.action_download,
                        ),
                    )
                },
                leadingIcon = { Icon(FlowIcons.Download, contentDescription = null) },
                onClick = {
                    open = false
                    onDownload()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_share)) },
                leadingIcon = { Icon(FlowIcons.Share, contentDescription = null) },
                onClick = {
                    open = false
                    onShare()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_copy_link)) },
                leadingIcon = { Icon(FlowIcons.Link, contentDescription = null) },
                onClick = {
                    open = false
                    onCopyLink()
                },
            )
        }
    }
}
