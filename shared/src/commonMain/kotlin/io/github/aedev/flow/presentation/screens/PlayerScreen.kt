package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.data.library.LibraryItem
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.model.LiveChatMessage
import io.github.aedev.flow.data.notes.VideoNote
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import io.github.aedev.flow.presentation.downloads.toPlayableStream
import io.github.aedev.flow.presentation.feed.FeedComment
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.VideoDetail
import io.github.aedev.flow.presentation.feed.liveChatMessages
import io.github.aedev.flow.presentation.feed.loadCommentReplies
import io.github.aedev.flow.presentation.feed.loadPlayableStream
import io.github.aedev.flow.presentation.feed.loadVideoDetail
import io.github.aedev.flow.presentation.feed.videoPosterUrl
import io.github.aedev.flow.presentation.feed.videoWatchUrl
import io.github.aedev.flow.presentation.playback.PlaybackQueue
import io.github.aedev.flow.presentation.playback.PlayerMedia
import io.github.aedev.flow.presentation.playback.QueuePlayer
import io.github.aedev.flow.presentation.playback.QueuePlayerSurface
import io.github.aedev.flow.presentation.playback.RepeatMode
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.LocalShareAction
import io.github.aedev.flow.presentation.ui.NoteEditorDialog
import io.github.aedev.flow.presentation.ui.PlayerFullscreenBackHandler
import io.github.aedev.flow.presentation.ui.PlayerFullscreenEffect
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.presentation.ui.isVideoPlaybackSupported
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.resources.link_copied
import io.github.aedev.flow.resources.notes_add
import io.github.aedev.flow.resources.notes_at
import io.github.aedev.flow.resources.notes_cancel
import io.github.aedev.flow.resources.notes_hint
import io.github.aedev.flow.resources.notes_save
import io.github.aedev.flow.resources.notes_saved
import io.github.aedev.flow.resources.player_exit_fullscreen
import io.github.aedev.flow.resources.player_fullscreen
import io.github.aedev.flow.resources.player_next_track
import io.github.aedev.flow.resources.player_previous_track
import io.github.aedev.flow.resources.player_queue_position
import io.github.aedev.flow.resources.player_repeat
import io.github.aedev.flow.ui.theme.Dimensions
import io.github.aedev.flow.ui.theme.PlayerScrimAffordance
import io.github.aedev.flow.ui.theme.PlayerScrimContent
import io.github.aedev.flow.ui.theme.PlayerScrimContentDisabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A selected video, laid out the way the app's `VideoInfoSection` lays it out: the black 16:9
 * surface with the back control over it, the title, one views/date line, the channel row and the
 * action row, then the description. The surface plays the resolved progressive stream through the
 * platform engine where one exists (Android/Media3) and shows the poster elsewhere. Under the description is the related rail:
 * the same full-width cards the feeds draw, each opening the player on its own video, then the
 * watch page's comments.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun PlayerScreen(
    videoId: String,
    fallbackTitle: String,
    thumbnailUrl: String?,
    onBack: () -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    libraryStore: LibraryStore,
    /** Whether the player's channel row shows Subscribed; the surface reads the one store. */
    subscribed: (channelId: String) -> Boolean = { false },
    onSubscriptionChange: (Subscription) -> Unit = {},
    /** A relaunch reopened this page: if the item no longer resolves, the tab beneath it takes over. */
    onUnavailable: (() -> Unit)? = null,
    /** The collection this page is playing, when it was opened by Play or Shuffle. */
    queue: PlaybackQueue? = null,
    /** Hands the stepped queue back to the host, which opens the track it now points at. */
    onQueueStep: (PlaybackQueue) -> Unit = {},
    /** The host's engine for the queue: one player across every track, so a change is not a reload. */
    queuePlayer: QueuePlayer? = null,
    /** Where the queue's current track resumes, used only when the engine has to load it fresh. */
    startPositionMs: Long = 0,
    /** How far the engine is into the queue's current track, for the host to write down. */
    onPosition: (index: Int, positionMs: Long) -> Unit = { _, _ -> },
    /** A change to the queue's own modes (repeat), kept by the host. */
    onQueueMode: (PlaybackQueue) -> Unit = {},
    /** The notes this video already has, so its action row shows it is noted. */
    videoNotes: List<VideoNote> = emptyList(),
    /** Writes a note at [timestampMs]; the host keeps the record. */
    onAddNote: (text: String, timestampMs: Long) -> Unit = { _, _ -> },
    /** This video's details arrived: the host records the watch even when nothing is queued. */
    onWatched: (title: String, channelName: String?, thumbnailUrl: String?) -> Unit = { _, _, _ -> },
    /** The imported file this page plays when the video is one of the device's own. */
    localStream: PlayableStream? = null,
    /** The download this device holds for this video, when it has one. */
    download: DownloadEntry? = null,
    /** How far a download running now has got, 0..1, or null when none is running. */
    downloadProgress: Float? = null,
    /** A download that failed, shown once; the host clears it when it has been shown. */
    downloadError: String? = null,
    onDownloadErrorShown: () -> Unit = {},
    /** Downloads this video's stream, or removes the download the device already holds. */
    onDownload: (title: String, channelName: String?, thumbnailUrl: String?, stream: PlayableStream?) -> Unit =
        { _, _, _, _ -> },
) {
    // A queue belongs to the page only while it points at it: opening any other video leaves it.
    val activeQueue = queue?.takeIf { it.current?.videoId == videoId }
    var state by remember(videoId) { mutableStateOf<LoadState<VideoDetail>>(LoadState.Loading) }
    var attempt by remember(videoId) { mutableIntStateOf(0) }
    val share = LocalShareAction.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val linkCopiedMessage = stringResource(Res.string.link_copied)
    val scope = rememberCoroutineScope()
    // Replies are fetched once per comment, on the first expand, and kept for this video's page.
    var repliesByComment by remember(videoId) { mutableStateOf<Map<String, List<FeedComment>>>(emptyMap()) }
    var expandedReplies by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var loadingReplies by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    // The library row for this video, read from Room; null while the video is neither saved nor
    // downloaded. Toggling writes before the state changes, so the row reflects what is on disk.
    var libraryItem by remember(videoId) { mutableStateOf<LibraryItem?>(null) }
    // A live stream's chat. The token comes out of the watch page already fetched, so a normal
    // video never issues a chat request, and the poll loop stops when this screen leaves.
    var chatMessages by remember(videoId) { mutableStateOf<List<LiveChatMessage>>(emptyList()) }
    // The surface plays the resolved stream where the platform has an engine, and stays a poster
    // until resolution succeeds (or forever on targets without playback).
    var stream by remember(videoId) { mutableStateOf<PlayableStream?>(null) }
    // GVS occasionally refuses a freshly minted URL with 403; a new resolution mints new ones, so a
    // failed player asks for the stream again a couple of times before giving up.
    var playbackAttempt by remember(videoId) { mutableIntStateOf(0) }
    // Fullscreen grows the surface to the whole window and puts the Android window in immersive
    // landscape; the rest of the page leaves the list until it is turned off.
    var fullscreen by remember(videoId) { mutableStateOf(false) }
    // The note editor: the moment is taken when it opens, so the text lands where the viewer saw.
    var noteEditorOpen by remember(videoId) { mutableStateOf(false) }
    var noteText by remember(videoId) { mutableStateOf("") }
    var noteAtMs by remember(videoId) { mutableLongStateOf(0L) }
    var notesMessage by remember(videoId) { mutableStateOf<String?>(null) }
    val noteSavedMessage = stringResource(Res.string.notes_saved)
    val liveChatToken = (state as? LoadState.Ready)?.value?.liveChatToken

    // The queue's pre-resolve window and the streams already fetched for it, so the engine can be
    // handed the next tracks before the current one ends instead of fetching them on the boundary.
    val queueWindow =
        activeQueue
            ?.let { playing -> playing.preResolveWindow(PRE_RESOLVE_AHEAD).mapNotNull { playing.tracks.getOrNull(it) } }
            .orEmpty()
    var queueStreams by remember(videoId) { mutableStateOf<Map<String, PlayableStream>>(emptyMap()) }

    LaunchedEffect(queueWindow.map { it.videoId }) {
        queueWindow.drop(1).forEach { track ->
            if (!queueStreams.containsKey(track.videoId)) {
                loadPlayableStream(track.videoId).getOrNull()?.let { resolved ->
                    queueStreams = queueStreams + (track.videoId to resolved)
                }
            }
        }
    }

    // The window as media, the playing track first: the engine only ever adds what it lacks.
    val queueMedia: List<PlayerMedia> =
        queueWindow.mapNotNull { track ->
            val resolved = if (track.videoId == videoId) stream else queueStreams[track.videoId]
            resolved?.let {
                PlayerMedia(
                    videoId = track.videoId,
                    stream = it,
                    title = track.title,
                    artist = (state as? LoadState.Ready)?.value?.channel,
                    artworkUri = track.thumbnailUrl,
                )
            }
        }
    val queueStartIndex = queueMedia.indexOfFirst { it.videoId == videoId }.coerceAtLeast(0)

    LaunchedEffect(queueMedia.map { it.videoId }, activeQueue?.index, activeQueue?.repeat, queuePlayer) {
        val playing = activeQueue ?: return@LaunchedEffect
        val engine = queuePlayer ?: return@LaunchedEffect
        if (queueMedia.isEmpty()) return@LaunchedEffect
        // The window must begin at this page's track. A stream still resolving would leave the
        // next track as the window's head, and the engine would start the queue on it.
        if (queueMedia.first().videoId != videoId) return@LaunchedEffect
        val engineCurrent = engine.currentVideoId()
        engine.sync(
            media = queueMedia,
            startIndex = queueStartIndex,
            startPositionMs = startPositionMs,
            repeat = playing.repeat,
            playing = true,
            // The engine holding a track of this same queue means the window is an extension of
            // it; a track from another collection means a different queue starts here.
            replace = engineCurrent != null && playing.tracks.none { it.videoId == engineCurrent },
        )
    }

    LaunchedEffect(activeQueue?.index, queuePlayer) {
        val playing = activeQueue ?: return@LaunchedEffect
        val engine = queuePlayer ?: return@LaunchedEffect
        val mine = playing.current?.videoId ?: return@LaunchedEffect
        while (true) {
            // A page that is being replaced must not report the position of the track the engine has
            // already left, or the next page would restore to it.
            if (engine.currentVideoId() == mine) onPosition(playing.index, engine.positionMs())
            delay(POSITION_REPORT_INTERVAL_MS)
        }
    }

    PlayerFullscreenEffect(fullscreen)
    PlayerFullscreenBackHandler(enabled = fullscreen) { fullscreen = false }

    val onToggleReplies: (FeedComment) -> Unit = { comment ->
        if (comment.id in expandedReplies) {
            expandedReplies = expandedReplies - comment.id
        } else {
            expandedReplies = expandedReplies + comment.id
            val token = comment.replyToken
            if (token != null && comment.id !in repliesByComment && comment.id !in loadingReplies) {
                loadingReplies = loadingReplies + comment.id
                scope.launch {
                    loadCommentReplies(token, videoId).onSuccess { fetched ->
                        repliesByComment = repliesByComment + (comment.id to fetched)
                    }
                    loadingReplies = loadingReplies - comment.id
                }
            }
        }
    }

    LaunchedEffect(videoId) {
        libraryItem = libraryStore.find(videoId)
    }

    // With the radio off there is no response to resolve from, so the download's own tracks are the
    // stream: they point at bytes the cache already holds, which is what makes it play offline.
    // GVS occasionally refuses a freshly minted URL with 403, so a resolution that comes back empty
    // is tried again a couple of times before the page settles on its poster.
    LaunchedEffect(videoId, playbackAttempt, download?.videoId, localStream) {
        // An imported file is already on this device: there is nothing to resolve, and asking
        // YouTube for a `content://` id would only mint a 404.
        if (localStream != null) {
            stream = localStream
            return@LaunchedEffect
        }
        if (!isVideoPlaybackSupported) return@LaunchedEffect
        val resolved = loadPlayableStream(videoId).getOrNull()
        stream = resolved ?: download?.toPlayableStream()
        if (stream == null && playbackAttempt < PLAYBACK_RESOLUTION_ATTEMPTS) {
            delay(PLAYBACK_RETRY_DELAY_MS)
            playbackAttempt++
        }
    }

    LaunchedEffect(videoId, liveChatToken) {
        val token = liveChatToken ?: return@LaunchedEffect
        chatMessages = emptyList()
        liveChatMessages(token).collect { batch ->
            chatMessages = (chatMessages + batch).distinctBy { it.id }
        }
    }

    LaunchedEffect(videoId, attempt, download?.videoId, localStream) {
        // A local file has no watch page: the page's header is the file's own name, and the sections
        // that need a response (actions, description, related, comments) stay away.
        if (localStream != null) {
            state =
                LoadState.Ready(
                    VideoDetail(
                        videoId = videoId,
                        title = fallbackTitle,
                        channel = null,
                        channelId = null,
                        channelAvatarUrl = null,
                        subscribers = null,
                        views = null,
                        likes = null,
                        published = null,
                        description = null,
                    ),
                )
            return@LaunchedEffect
        }
        state =
            loadVideoDetail(videoId).fold(
                onSuccess = { detail ->
                    // A removed video answers without a title rather than with an error, so a
                    // nameless page is the unresolvable case for a restored player.
                    if (onUnavailable != null && detail.title.isBlank()) {
                        onUnavailable()
                        LoadState.Failed("")
                    } else {
                        onWatched(
                            detail.title.ifBlank { fallbackTitle },
                            detail.channel,
                            thumbnailUrl ?: videoPosterUrl(videoId),
                        )
                        LoadState.Ready(detail)
                    }
                },
                onFailure = { error ->
                    // Offline, a downloaded video still has everything its page needs: the record
                    // made when it was fetched is the header and the action row.
                    if (download != null) {
                        LoadState.Ready(
                            VideoDetail(
                                videoId = videoId,
                                title = download.title,
                                channel = download.channelName,
                                channelId = null,
                                channelAvatarUrl = null,
                                subscribers = null,
                                views = null,
                                likes = null,
                                published = null,
                                description = null,
                            ),
                        )
                    } else {
                        onUnavailable?.invoke()
                        LoadState.Failed(error.message.orEmpty())
                    }
                },
            )
    }

    // The poster and the back control belong to the frame, not to the response: they stay while the
    // metadata loads, and the control is pinned so scrolling cannot carry it out of reach.
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
        ) {
            item("surface") {
                // The shell gives every video an entry in the queue engine, so the surface is the
                // engine's whenever the page has its track; a page still waiting for that shows the
                // poster instead of a second player, which is what kept two players alive at once.
                val engine = queuePlayer?.takeIf { activeQueue != null && queueMedia.isNotEmpty() }
                // The surface stays this same item in both modes, so entering fullscreen does not
                // release the player and start the video over from the beginning.
                val surfaceModifier =
                    if (fullscreen) {
                        Modifier.fillParentMaxSize()
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    }
                Box(modifier = surfaceModifier.background(Color.Black)) {
                    if (engine != null) {
                        QueuePlayerSurface(player = engine, modifier = Modifier.fillMaxSize())
                    } else {
                        PlayerSurface(
                            posterUrl = thumbnailUrl ?: videoPosterUrl(videoId),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    SurfaceControls(
                        queue = activeQueue,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onStep = onQueueStep,
                        repeat = activeQueue?.repeat ?: RepeatMode.OFF,
                        onCycleRepeat = { activeQueue?.withRepeat(activeQueue.repeat.next())?.let(onQueueMode) },
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(Dimensions.ContentPaddingHorizontal),
                    )
                }
            }

            if (!fullscreen) {
                when (val current = state) {
                    LoadState.Loading -> {
                        item("loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is LoadState.Failed -> {
                        item("failed") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PlayerInfoPadding, vertical = Dimensions.SectionSpacing),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    }

                    is LoadState.Ready -> {
                        val detail = current.value
                        item("header") {
                            PlayerHeader(
                                detail = detail,
                                fallbackTitle = fallbackTitle,
                                onOpenChannel = onOpenChannel,
                                subscribed = detail.channelId?.let(subscribed) == true,
                                onSubscriptionChange = onSubscriptionChange,
                            )
                        }
                        item("actions") {
                            // Share, save and download describe a YouTube video; an imported file has
                            // none of those, so its row is the header alone.
                            if (localStream != null) return@item
                            VideoActionRow(
                                likes = detail.likes,
                                onShare = { share(detail.title.ifBlank { fallbackTitle }, videoWatchUrl(videoId)) },
                                onCopyLink = {
                                    clipboard.setText(AnnotatedString(videoWatchUrl(videoId)))
                                    scope.launch { snackbar.showSnackbar(linkCopiedMessage) }
                                },
                                isSaved = libraryItem != null,
                                isDownloaded = download != null,
                                downloadProgress = downloadProgress,
                                hasNote = videoNotes.isNotEmpty(),
                                onNote = {
                                    noteAtMs = queuePlayer?.takeIf { activeQueue != null }?.positionMs() ?: 0L
                                    noteText = ""
                                    noteEditorOpen = true
                                },
                                onSave = {
                                    scope.launch {
                                        libraryStore.toggleSaved(
                                            videoId = videoId,
                                            title = detail.title.ifBlank { fallbackTitle },
                                            channelName = detail.channel,
                                            thumbnailUrl = thumbnailUrl ?: videoPosterUrl(videoId),
                                            nowMillis = currentTimeMillis(),
                                        )
                                        libraryItem = libraryStore.find(videoId)
                                    }
                                },
                                onDownload = {
                                    onDownload(
                                        detail.title.ifBlank { fallbackTitle },
                                        detail.channel,
                                        thumbnailUrl ?: videoPosterUrl(videoId),
                                        stream,
                                    )
                                },
                            )
                        }
                        detail.description?.takeIf { it.isNotBlank() }?.let { description ->
                            item("description") { DescriptionCard(text = description) }
                        }
                        if (detail.related.isNotEmpty()) {
                            item("related-gap") { Spacer(modifier = Modifier.height(Dimensions.SectionSpacing)) }
                            detail.related.forEach { entry ->
                                item("related-${entry.id}") {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = PlayerInfoPadding, vertical = 8.dp),
                                    ) {
                                        VideoCard(entry = entry, onOpen = onOpenVideo)
                                    }
                                }
                            }
                        }
                        commentsSection(
                            comments = detail.comments,
                            countText = detail.commentCountText,
                            replies = repliesByComment,
                            expanded = expandedReplies,
                            loading = loadingReplies,
                            onToggleReplies = onToggleReplies,
                        )
                        liveChatSection(chatMessages)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (noteEditorOpen) {
            NoteEditorDialog(
                title = stringResource(Res.string.notes_add),
                momentLabel =
                    if (noteAtMs > 0) {
                        stringResource(Res.string.notes_at, durationText((noteAtMs / 1000).toInt()))
                    } else {
                        null
                    },
                text = noteText,
                onTextChange = { noteText = it },
                placeholder = stringResource(Res.string.notes_hint),
                saveLabel = stringResource(Res.string.notes_save),
                cancelLabel = stringResource(Res.string.notes_cancel),
                onSave = {
                    onAddNote(noteText, noteAtMs)
                    notesMessage = noteSavedMessage
                    noteEditorOpen = false
                },
                onDismiss = { noteEditorOpen = false },
            )
        }

        LaunchedEffect(notesMessage) {
            val message = notesMessage ?: return@LaunchedEffect
            snackbar.showSnackbar(message)
            notesMessage = null
        }

        LaunchedEffect(downloadError) {
            val message = downloadError ?: return@LaunchedEffect
            snackbar.showSnackbar(message)
            onDownloadErrorShown()
        }

        // Fullscreen's own exit control is on the surface; the page's back control would only
        // duplicate it, and the system back gesture leaves fullscreen first.
        if (!fullscreen) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(Dimensions.ContentPaddingHorizontal),
            ) {
                Icon(
                    imageVector = FlowIcons.Back,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
        }
    }
}

/**
 * The controls resting on the video: the queue's step buttons and where it is, then fullscreen.
 * The step buttons disable at either end rather than wrapping, matching the queue, which stops
 * after its last track.
 */
@Composable
private fun SurfaceControls(
    queue: PlaybackQueue?,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onStep: (PlaybackQueue) -> Unit,
    repeat: RepeatMode = RepeatMode.OFF,
    onCycleRepeat: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val active = queue?.takeIf { it.size > 1 }
        if (active != null) {
            Surface(
                color = PlayerScrimAffordance,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = { active.previous()?.let(onStep) },
                        enabled = active.hasPrevious,
                    ) {
                        Icon(
                            imageVector = FlowIcons.SkipPrevious,
                            contentDescription = stringResource(Res.string.player_previous_track),
                            tint = if (active.hasPrevious) PlayerScrimContent else PlayerScrimContentDisabled,
                        )
                    }
                    Text(
                        text =
                            stringResource(
                                Res.string.player_queue_position,
                                active.index + 1,
                                active.size,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlayerScrimContent,
                    )
                    IconButton(
                        onClick = { active.next()?.let(onStep) },
                        enabled = active.hasNext,
                    ) {
                        Icon(
                            imageVector = FlowIcons.SkipNext,
                            contentDescription = stringResource(Res.string.player_next_track),
                            tint = if (active.hasNext) PlayerScrimContent else PlayerScrimContentDisabled,
                        )
                    }
                }
            }
        }
        if (queue != null) {
            FilledTonalIconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (repeat == RepeatMode.ONE) FlowIcons.RepeatOne else FlowIcons.Repeat,
                    contentDescription = stringResource(Res.string.player_repeat),
                    tint =
                        if (repeat == RepeatMode.OFF) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }
        }
        FullscreenToggle(fullscreen = fullscreen, onToggle = onToggleFullscreen)
    }
}

/** The player's fullscreen control: one button that enters and leaves the immersive surface. */
@Composable
private fun FullscreenToggle(
    fullscreen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (fullscreen) FlowIcons.FullscreenExit else FlowIcons.Fullscreen,
            contentDescription =
                stringResource(
                    if (fullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen,
                ),
        )
    }
}

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** How many times a refused stream is minted again before the poster stays. */
private const val PLAYBACK_RESOLUTION_ATTEMPTS = 2
private const val PLAYBACK_RETRY_DELAY_MS = 1_500L

/** Tracks kept resolved ahead of the playing one, so the engine never fetches on a boundary. */
private const val PRE_RESOLVE_AHEAD = 3

/** How often the engine's position is handed to the host to be written down. */
private const val POSITION_REPORT_INTERVAL_MS = 2_000L
