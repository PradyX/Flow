package io.github.aedev.flow.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.data.downloads.Downloads
import io.github.aedev.flow.data.downloads.loadDownloadsJson
import io.github.aedev.flow.data.downloads.saveDownloadsJson
import io.github.aedev.flow.data.history.WatchHistory
import io.github.aedev.flow.data.history.WatchHistoryEntry
import io.github.aedev.flow.data.history.loadWatchHistoryJson
import io.github.aedev.flow.data.history.saveWatchHistoryJson
import io.github.aedev.flow.data.library.LibraryItem
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.library.openDefaultLibraryDatabase
import io.github.aedev.flow.data.local.LocalMediaItem
import io.github.aedev.flow.data.local.LocalMediaLibrary
import io.github.aedev.flow.data.local.loadLocalMediaJson
import io.github.aedev.flow.data.local.saveLocalMediaJson
import io.github.aedev.flow.data.navigation.loadNavigationStateJson
import io.github.aedev.flow.data.navigation.saveNavigationStateJson
import io.github.aedev.flow.data.notes.VideoNotes
import io.github.aedev.flow.data.notes.loadVideoNotesJson
import io.github.aedev.flow.data.notes.saveVideoNotesJson
import io.github.aedev.flow.data.playback.clearPlaybackCheckpointJson
import io.github.aedev.flow.data.playback.clearPlaybackSessionJson
import io.github.aedev.flow.data.playback.loadPlaybackCheckpointJson
import io.github.aedev.flow.data.playback.loadPlaybackSessionJson
import io.github.aedev.flow.data.playback.savePlaybackCheckpointJson
import io.github.aedev.flow.data.playback.savePlaybackSessionJson
import io.github.aedev.flow.data.playlists.PlaylistVideo
import io.github.aedev.flow.data.playlists.UserPlaylists
import io.github.aedev.flow.data.playlists.loadUserPlaylistsJson
import io.github.aedev.flow.data.playlists.saveUserPlaylistsJson
import io.github.aedev.flow.data.search.SearchHistory
import io.github.aedev.flow.data.search.loadSearchHistoryJson
import io.github.aedev.flow.data.search.loadSearchSessionJson
import io.github.aedev.flow.data.settings.AppSettings
import io.github.aedev.flow.data.settings.loadAppSettingsJson
import io.github.aedev.flow.data.settings.saveAppSettingsJson
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.data.subscriptions.loadSubscriptions
import io.github.aedev.flow.data.subscriptions.saveSubscriptions
import io.github.aedev.flow.data.subscriptions.seedSubscriptions
import io.github.aedev.flow.data.taste.HomeFeedback
import io.github.aedev.flow.data.taste.UserTaste
import io.github.aedev.flow.data.taste.loadHomeFeedback
import io.github.aedev.flow.data.taste.loadUserTaste
import io.github.aedev.flow.data.taste.saveHomeFeedback
import io.github.aedev.flow.data.taste.saveUserTaste
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import io.github.aedev.flow.presentation.downloads.DownloadOutcome
import io.github.aedev.flow.presentation.downloads.deleteDownloadedMedia
import io.github.aedev.flow.presentation.downloads.downloadMedia
import io.github.aedev.flow.presentation.feed.ChannelBook
import io.github.aedev.flow.presentation.feed.HomeFeedState
import io.github.aedev.flow.presentation.feed.MusicFeedState
import io.github.aedev.flow.presentation.feed.SearchFeedState
import io.github.aedev.flow.presentation.feed.SearchSession
import io.github.aedev.flow.presentation.feed.SubscriptionsFeedState
import io.github.aedev.flow.presentation.feed.loadPlayableStream
import io.github.aedev.flow.presentation.local.rememberMediaImporter
import io.github.aedev.flow.presentation.nav.AppDestination
import io.github.aedev.flow.presentation.nav.AppRouter
import io.github.aedev.flow.presentation.nav.navigationStateJson
import io.github.aedev.flow.presentation.nav.restoredNavigationStack
import io.github.aedev.flow.presentation.onboarding.OnboardingScreen
import io.github.aedev.flow.presentation.playback.HostStopEffect
import io.github.aedev.flow.presentation.playback.PlaybackCheckpoint
import io.github.aedev.flow.presentation.playback.PlaybackQueue
import io.github.aedev.flow.presentation.playback.PlaybackSession
import io.github.aedev.flow.presentation.playback.QueueTrack
import io.github.aedev.flow.presentation.playback.clearPlaybackCache
import io.github.aedev.flow.presentation.playback.rememberQueuePlayer
import io.github.aedev.flow.presentation.playback.withCheckpoint
import io.github.aedev.flow.presentation.screens.ArtistScreen
import io.github.aedev.flow.presentation.screens.CategoriesScreen
import io.github.aedev.flow.presentation.screens.ChannelScreen
import io.github.aedev.flow.presentation.screens.ExploreShelfScreen
import io.github.aedev.flow.presentation.screens.HistoryScreen
import io.github.aedev.flow.presentation.screens.HomeScreen
import io.github.aedev.flow.presentation.screens.LibraryScreen
import io.github.aedev.flow.presentation.screens.MoodScreen
import io.github.aedev.flow.presentation.screens.MoodsScreen
import io.github.aedev.flow.presentation.screens.MusicScreen
import io.github.aedev.flow.presentation.screens.NotesScreen
import io.github.aedev.flow.presentation.screens.PlayerScreen
import io.github.aedev.flow.presentation.screens.PlaylistScreen
import io.github.aedev.flow.presentation.screens.PlaylistsScreen
import io.github.aedev.flow.presentation.screens.PostScreen
import io.github.aedev.flow.presentation.screens.SavedShortsScreen
import io.github.aedev.flow.presentation.screens.SearchScreen
import io.github.aedev.flow.presentation.screens.SettingsScreen
import io.github.aedev.flow.presentation.screens.ShortsScreen
import io.github.aedev.flow.presentation.screens.SubscriptionsScreen
import io.github.aedev.flow.presentation.screens.UserPlaylistScreen
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowLogoMark
import io.github.aedev.flow.presentation.ui.FlowNavigationBar
import io.github.aedev.flow.presentation.ui.FlowNavigationRail
import io.github.aedev.flow.presentation.ui.FlowProfileAction
import io.github.aedev.flow.presentation.ui.FlowProfileSheet
import io.github.aedev.flow.presentation.ui.FlowTab
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.InstallFlowImageLoader
import io.github.aedev.flow.presentation.ui.MiniPlayer
import io.github.aedev.flow.presentation.ui.WideLayoutMinWidth
import io.github.aedev.flow.presentation.ui.isVideoPlaybackSupported
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_search
import io.github.aedev.flow.resources.app_name_uppercase
import io.github.aedev.flow.resources.settings_title
import io.github.aedev.flow.resources.shorts_title
import io.github.aedev.flow.ui.theme.FlowTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A router that reopens the stack the last run left — the tab, plus the detail page that was on
 * top of it — or Home when nothing restorable was stored. [App] defaults to it, so every host
 * relaunches onto the page the user was reading.
 */
fun restoredAppRouter(): AppRouter {
    val restored = restoredNavigationStack(loadNavigationStateJson())
    // A detail page never stands alone: if the last run ended on one, the tab it was opened from
    // goes beneath it, so back always has somewhere to return to.
    val stack =
        when {
            restored.isEmpty() -> listOf(AppDestination.Home)
            restored.first().tabName != null -> restored
            else -> listOf(AppDestination.Home) + restored
        }
    return AppRouter(start = stack.first(), restoredDetail = stack.drop(1))
}

/**
 * Shared entry point — the Android activity, the desktop window and the iOS view controller all
 * render this. It is the app's shell: the `FlowTopBar` across the top, the screen below it and the
 * `FlowNavigationBar` at the bottom. Navigation is [AppRouter]; the destinations are Home, Search
 * and the player.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun App(
    router: AppRouter = remember { restoredAppRouter() },
    libraryStore: LibraryStore = remember { LibraryStore(openDefaultLibraryDatabase()) },
    onboardingDone: Boolean = true,
    taste: UserTaste = loadUserTaste() ?: UserTaste(),
    onOnboardingComplete: () -> Unit = {},
    /** The build the settings page reports; the host supplies its own version name. */
    appVersion: String = "3.0.0",
) {
    var viewerTaste by remember { mutableStateOf(taste) }
    // What the settings page owns and the shell obeys: the appearance every surface is drawn in.
    var settings by remember { mutableStateOf(AppSettings.decode(loadAppSettingsJson())) }
    // The records this shell keeps beside the player: what was watched and what was noted.
    var history by remember { mutableStateOf(WatchHistory.decode(loadWatchHistoryJson())) }
    // The files imported from this device, newest first. The record is the app's; the bytes stay
    // where the viewer keeps them, which is what makes an import instant and free.
    var localMedia by remember { mutableStateOf(LocalMediaLibrary.decode(loadLocalMediaJson())) }
    var notes by remember { mutableStateOf(VideoNotes.decode(loadVideoNotesJson())) }
    // The playlists this device made, with the videos each holds in the order they were added.
    var playlists by remember { mutableStateOf(UserPlaylists.decode(loadUserPlaylistsJson())) }
    var lastHistoryWriteMs by remember { mutableLongStateOf(0L) }
    val saveHistory: (WatchHistory) -> Unit = { next ->
        history = next
        saveWatchHistoryJson(next.encode())
    }
    val saveLocalMedia: (LocalMediaLibrary) -> Unit = { next ->
        localMedia = next
        saveLocalMediaJson(next.encode())
    }
    val importMedia = rememberMediaImporter { picked -> saveLocalMedia(localMedia.withAll(picked)) }
    val saveNotes: (VideoNotes) -> Unit = { next ->
        notes = next
        saveVideoNotesJson(next.encode())
    }
    val savePlaylists: (UserPlaylists) -> Unit = { next ->
        playlists = next
        saveUserPlaylistsJson(next.encode())
    }
    // The videos downloaded to this device: the bytes live in the player's media cache, and this
    // record says what they are, so a relaunch still knows a video is downloaded and can play it
    // with no network at all.
    var downloads by remember { mutableStateOf(Downloads.decode(loadDownloadsJson())) }
    var downloadProgress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val shellScope = rememberCoroutineScope()
    val saveDownloads: (Downloads) -> Unit = { next ->
        downloads = next
        saveDownloadsJson(next.encode())
    }
    // A download resolves its stream first: the player already has one, a Library row asks for it.
    // Nothing is recorded until every track is on disk, so an abort leaves no half-entry.
    val startDownload: (String, String, String?, String?, PlayableStream?) -> Unit =
        { videoId, title, channelName, thumbnailUrl, known ->
            if (videoId.isNotBlank() && !downloads.contains(videoId) && !downloadProgress.containsKey(videoId)) {
                downloadError = null
                downloadProgress = downloadProgress + (videoId to 0f)
                shellScope.launch {
                    val stream = known ?: loadPlayableStream(videoId).getOrNull()
                    if (stream == null) {
                        downloadProgress = downloadProgress - videoId
                        downloadError = "Could not reach this video's streams"
                    } else {
                        downloadMedia(
                            videoId = videoId,
                            stream = stream,
                            onProgress = { progress -> downloadProgress = downloadProgress + (videoId to progress.fraction) },
                            onOutcome = { outcome ->
                                downloadProgress = downloadProgress - videoId
                                when (outcome) {
                                    is DownloadOutcome.Done -> {
                                        saveDownloads(
                                            downloads.with(
                                                DownloadEntry(
                                                    videoId = videoId,
                                                    title = title.ifBlank { videoId },
                                                    channelName = channelName,
                                                    thumbnailUrl = thumbnailUrl,
                                                    videoUrl = stream.videoUrl,
                                                    audioUrl = stream.audioUrl,
                                                    userAgent = stream.userAgent,
                                                    mimeType = stream.videoMimeType,
                                                    audioMimeType = stream.audioMimeType,
                                                    qualityLabel = stream.qualityLabel,
                                                    height = stream.height,
                                                    sizeBytes = outcome.bytes,
                                                    completedAtMillis = nowMillis(),
                                                ),
                                            ),
                                        )
                                        shellScope.launch {
                                            libraryStore.markDownloaded(videoId, title, channelName, thumbnailUrl, nowMillis())
                                        }
                                    }

                                    is DownloadOutcome.Failed -> {
                                        downloadError = outcome.reason
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    // Removing a download deletes its bytes from the cache, then forgets the record, so the row
    // stops claiming the video is on disk the moment the files are gone.
    val removeDownload: (String) -> Unit = { videoId ->
        downloads.entry(videoId)?.let { entry ->
            deleteDownloadedMedia(entry)
            saveDownloads(downloads.without(videoId))
            shellScope.launch { libraryStore.clearDownloaded(videoId) }
        }
    }
    val toggleDownload: (String, String, String?, String?, PlayableStream?) -> Unit =
        { videoId, title, channelName, thumbnailUrl, stream ->
            if (downloads.contains(videoId)) removeDownload(videoId) else startDownload(videoId, title, channelName, thumbnailUrl, stream)
        }
    // One row per video, newest first: the player writes it as it plays, so History has something
    // to resume even when the process was killed mid-track.
    val recordWatch: (String, String, String?, String?, Long, Long) -> Unit =
        { videoId, title, channelName, thumbnailUrl, positionMs, durationMs ->
            if (videoId.isNotBlank()) {
                val existing = history.entry(videoId)
                saveHistory(
                    history.with(
                        WatchHistoryEntry(
                            videoId = videoId,
                            title = title.ifBlank { existing?.title ?: videoId },
                            channelName = channelName ?: existing?.channelName,
                            thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
                            positionMs = positionMs,
                            durationMs = durationMs.takeIf { it > 0 } ?: existing?.durationMs ?: 0,
                            watchedAtMillis = nowMillis(),
                        ),
                    ),
                )
            }
        }
    // The player as the last run left it: the queue, the track it was on, the modes and the position.
    // A relaunch continues from that point instead of dropping to nothing or to a single video.
    val restoredSession =
        remember {
            PlaybackSession
                .decode(loadPlaybackSessionJson())
                ?.withCheckpoint(PlaybackCheckpoint.decode(loadPlaybackCheckpointJson()))
        }
    // The collection the player is working through, when Play or Shuffle started one. It lives in the
    // shell because stepping it swaps the player page for the next track's, and the album page it
    // came from stays underneath either way.
    var playerQueue by remember { mutableStateOf(restoredSession?.toQueue()) }
    var queuePositionMs by remember { mutableStateOf(restoredSession?.position() ?: 0L) }
    // One engine for the whole queue, held here so swapping the player page for the next track's
    // does not release and rebuild it: the tracks already handed to it keep playing.
    val queuePlayer =
        rememberQueuePlayer(
            onTrackStarted = { videoId ->
                val queue = playerQueue
                val index = queue?.tracks?.indexOfFirst { it.videoId == videoId } ?: -1
                if (queue != null && index >= 0) {
                    if (queue.index != index) {
                        queuePositionMs = 0
                        playerQueue = queue.copy(index = index)
                        savePlaybackSessionJson(PlaybackSession.of(queue.copy(index = index), 0).encode())
                    }
                    // A new track starts at zero; the checkpoint says so before the first tick.
                    savePlaybackCheckpointJson(PlaybackCheckpoint(videoId, 0).encode())
                    val track = queue.tracks[index]
                    recordWatch(track.videoId, track.title, null, track.thumbnailUrl, 0, 0)
                    // Only a player page is re-pointed at the new track. A viewer who is on a tab
                    // sees the change in the mini player, and replacing the tab with the player
                    // page would take the tab out of the stack a relaunch restores.
                    val current = router.current as? AppDestination.Player
                    if (current != null && current.videoId != track.videoId) {
                        router.replaceTop(AppDestination.Player(track.videoId, track.title, track.thumbnailUrl))
                    }
                }
            },
            onEnded = { queuePositionMs = 0 },
            onError = {},
            // Written straight to its own record, so no recomposition is caused by the ticks and a
            // kill loses only the time since this one.
            onCheckpoint = { videoId, positionMs ->
                if (playerQueue?.current?.videoId == videoId) {
                    savePlaybackCheckpointJson(PlaybackCheckpoint(videoId, positionMs).encode())
                }
            },
        )
    // The queue's page reports where it is; History is written from that as it plays, not only
    // when the track ends.
    val historyWrite: (String, String, String?, Long, Long) -> Unit =
        { videoId, title, thumbnailUrl, positionMs, durationMs ->
            val now = nowMillis()
            if (now - lastHistoryWriteMs > HISTORY_WRITE_INTERVAL_MS) {
                lastHistoryWriteMs = now
                recordWatch(videoId, title, null, thumbnailUrl, positionMs, durationMs)
            }
        }
    // The last guaranteed moment: the host going to the background writes the session and the
    // position as they stand, so an ordinary app switch does not lose the tail of the track.
    HostStopEffect {
        val queue = playerQueue
        if (queue != null) {
            val position = queuePlayer.positionMs()
            queuePositionMs = position
            savePlaybackSessionJson(PlaybackSession.of(queue, position).encode())
            queuePlayer.currentVideoId()?.let { videoId ->
                savePlaybackCheckpointJson(PlaybackCheckpoint(videoId, position).encode())
            }
            queue.current?.let { track ->
                lastHistoryWriteMs = nowMillis()
                recordWatch(track.videoId, track.title, null, track.thumbnailUrl, position, queuePlayer.durationMs())
            }
        }
    }
    // A relaunch opens the player on the queue it left, even when the last page it stored was the
    // tab underneath: the track resumes where the session says it was.
    LaunchedEffect(Unit) {
        val queue = playerQueue ?: return@LaunchedEffect
        val track = queue.current ?: return@LaunchedEffect
        if ((router.current as? AppDestination.Player)?.videoId != track.videoId) {
            router.push(AppDestination.Player(track.videoId, track.title, track.thumbnailUrl))
        }
    }
    // Written down whenever the queue or the position changes, so a killed process comes back to
    // the same track at the same second.
    LaunchedEffect(playerQueue, queuePositionMs) {
        val queue = playerQueue
        if (queue == null) {
            clearPlaybackSessionJson()
            clearPlaybackCheckpointJson()
        } else {
            savePlaybackSessionJson(PlaybackSession.of(queue, queuePositionMs).encode())
        }
    }
    val playCollection: (List<QueueTrack>, Boolean, Int) -> Unit = { tracks, shuffled, startIndex ->
        PlaybackQueue.of(tracks, shuffled)?.let { queue ->
            val started =
                if (shuffled) queue else queue.copy(index = startIndex.coerceIn(0, queue.tracks.lastIndex))
            val track = started.current
            if (track != null) {
                // Tapping another row of the queue the engine is already playing is a jump, and the
                // engine's own tail cannot express one that begins before it: the record is dropped
                // so the page's window is installed fresh, at the row that was tapped.
                if (queuePlayer.currentVideoId()?.let { it != track.videoId } == true) queuePlayer.resetQueue()
                playerQueue = started
                queuePositionMs = 0
                router.push(AppDestination.Player(track.videoId, track.title, track.thumbnailUrl))
            }
        }
    }
    // "Play next" and "Add to queue" extend the queue that is playing, or start one from those tracks
    // when nothing is queued yet. Either way the player comes back up on the queue's current track,
    // since the page the action came from is the only other place the queue can be seen from.
    val queueTracks: (List<QueueTrack>, Boolean) -> Unit = { tracks, next ->
        val queue = playerQueue
        val updated =
            when {
                queue == null -> PlaybackQueue.of(tracks)
                next -> queue.withNext(tracks)
                else -> queue.withLast(tracks)
            }
        val track = updated?.current
        if (updated != null && track != null) {
            if (queue == null) queuePositionMs = 0
            playerQueue = updated
            if (router.current !is AppDestination.Player) {
                router.push(AppDestination.Player(track.videoId, track.title, track.thumbnailUrl))
            }
        }
    }
    val stepQueue: (PlaybackQueue) -> Unit = { stepped ->
        val track = stepped.current
        if (track != null) {
            if (stepped.index != playerQueue?.index) queuePositionMs = 0
            playerQueue = stepped
            // A step is the one caller that moves the engine deliberately; an auto advance is the
            // engine moving the app.
            queuePlayer.select(track.videoId)
            router.replaceTop(AppDestination.Player(track.videoId, track.title, track.thumbnailUrl))
        }
    }
    // The queue's modes are the shell's, and its page only reports the change.
    val queueModes: (PlaybackQueue) -> Unit = { changed -> playerQueue = changed }
    // A video at a moment: where History stopped, or the spot a note is about. The engine resumes
    // there through the same queue path everything else plays through.
    val playAt: (String, String, String?, Long) -> Unit = { videoId, title, thumbnailUrl, positionMs ->
        val at = positionMs.coerceAtLeast(0)
        if (playerQueue?.current?.videoId == videoId && queuePlayer.currentVideoId() == videoId) {
            queuePlayer.select(videoId, at)
            queuePositionMs = at
        } else {
            PlaybackQueue.of(listOf(QueueTrack(videoId, title, thumbnailUrl)))?.let { queue ->
                playerQueue = queue
                queuePositionMs = at
            }
        }
        val page = AppDestination.Player(videoId, title, thumbnailUrl)
        router.push(page)
    }
    // Every video the app opens plays through the one session-backed queue: tapping a card is the
    // same act as resuming a note, so the notification, the lock screen and the media keys always
    // describe what is actually being heard.
    val openVideo: (String, String, String?) -> Unit = { videoId, title, thumbnailUrl ->
        playAt(videoId, title, thumbnailUrl, 0)
    }
    // The bar's close action: the engine drops its playlist, so the session stops publishing and
    // the foreground service lets go of the notification. Clearing the queue is what takes the bar
    // off the tabs.
    val stopPlayback: () -> Unit = {
        // The queue leaves first: a transition the engine reports while it is cleared must not read
        // as a track starting, which would push the player page back over the tab.
        playerQueue = null
        queuePositionMs = 0
        queuePlayer.stop()
    }
    // An imported file is a track like any other: it joins the queue, the notification prints its
    // name, and it keeps playing when the library is left.
    val playLocal: (LocalMediaItem) -> Unit = { item ->
        PlaybackQueue.of(listOf(QueueTrack(item.uri, item.title, null)))?.let { queue ->
            playerQueue = queue
            queuePositionMs = 0
            router.push(AppDestination.Player(item.uri, item.title, null))
        }
    }
    // A player page revealed by back (the video a related card was opened from) becomes the queue's
    // track again, so leaving a page never leaves the engine playing a video nobody is looking at.
    val topDestination = router.current
    LaunchedEffect(topDestination) {
        val top = topDestination as? AppDestination.Player ?: return@LaunchedEffect
        if (playerQueue?.current?.videoId != top.videoId) {
            PlaybackQueue.of(listOf(QueueTrack(top.videoId, top.title, top.thumbnailUrl)))?.let { queue ->
                playerQueue = queue
                queuePositionMs = 0
            }
        }
    }
    // Setup's picks seed this once; after that the channel page's control is the only writer and
    // the Subscriptions tab the only reader.
    val storedSubscriptions = remember { loadSubscriptions() }
    var subscriptions by remember { mutableStateOf(storedSubscriptions ?: taste.seedSubscriptions()) }
    // The channel page and the player's channel row write the one store, so the tab always agrees.
    val toggleSubscription: (Subscription) -> Unit = { channel ->
        val next =
            if (subscriptions.isSubscribed(channel.id)) {
                subscriptions.without(channel.id)
            } else {
                subscriptions.with(channel)
            }
        saveSubscriptions(next)
        subscriptions = next
    }
    var viewerFeedback by remember { mutableStateOf(loadHomeFeedback() ?: HomeFeedback()) }
    // Owned by the shell, so a detail screen opening and closing neither refetches a feed nor
    // loses the search somebody was reading.
    val homeFeed = remember { HomeFeedState() }
    val musicFeed = remember { MusicFeedState() }
    val subscriptionsFeed = remember { SubscriptionsFeedState() }
    // The search page the last run left behind, so a relaunch opens on it instead of an empty one.
    val searchFeed =
        remember {
            SearchFeedState.fromSession(
                session = SearchSession.fromJson(loadSearchSessionJson()),
                history = SearchHistory.decode(loadSearchHistoryJson()),
            )
        }
    val channelBook = remember { ChannelBook() }
    var showProfiles by remember { mutableStateOf(false) }

    InstallFlowImageLoader()

    // The appearance the settings page chose; SYSTEM follows the device, as the app's own mode does.
    FlowTheme(
        themeMode = settings.appearance.themeMode,
        themeVariant = settings.appearance.themeVariant,
    ) {
        // First run is the app's setup, not the shell: nothing behind it is composed yet.
        if (!onboardingDone) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                OnboardingScreen(
                    onComplete = { picked ->
                        saveUserTaste(picked)
                        val seeded = picked.seedSubscriptions()
                        saveSubscriptions(seeded)
                        subscriptions = seeded
                        viewerTaste = picked
                        onOnboardingComplete()
                    },
                )
            }
            return@FlowTheme
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // The app swaps to a side rail at the same width it does, so a pane of a split
                // screen keeps its bar.
                val wide = maxWidth >= WideLayoutMinWidth
                // A build that predates the subscription store seeds it from setup's picks, so an
                // upgrade keeps the channels the tab was already showing.
                LaunchedEffect(Unit) {
                    if (storedSubscriptions == null) saveSubscriptions(subscriptions)
                }
                val destination = router.current

                // The page the last run left, read once: a route that no longer resolves has the
                // tab beneath it as its floor, while a page opened by a tap keeps its error state.
                val restoredDetail = remember { router.stack.lastOrNull()?.takeIf { router.stack.size > 1 } }
                val onRestoredPageUnavailable: (() -> Unit)? =
                    if (destination == restoredDetail) {
                        { router.dropToTabRoot() }
                    } else {
                        null
                    }
                LaunchedEffect(router.stack) {
                    saveNavigationStateJson(navigationStateJson(router.stack))
                }
                // The player fills the window, so it is the one page with nothing beside it; every
                // other page keeps the rail the way the app's own shell does.
                val railVisible = wide && destination !is AppDestination.Player
                Row(modifier = Modifier.fillMaxSize()) {
                    if (railVisible) {
                        FlowNavigationRail(selected = selectedTab(router), onSelect = onSelectTab(router))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        // Home draws its own bar inside a scroll-away header, so its bar leaves
                        // with the feed; every other page keeps the shell's fixed bar.
                        if (destination != AppDestination.Home) {
                            TopBar(
                                router = router,
                                destination = destination,
                                onOpenProfiles = { showProfiles = true },
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (destination) {
                                AppDestination.Home -> {
                                    HomeScreen(
                                        state = homeFeed,
                                        taste = viewerTaste,
                                        feedback = viewerFeedback,
                                        onFeedback = { next ->
                                            saveHomeFeedback(next)
                                            viewerFeedback = next
                                        },
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        topBar = {
                                            TopBar(
                                                router = router,
                                                destination = AppDestination.Home,
                                                onOpenProfiles = { showProfiles = true },
                                            )
                                        },
                                        continueWatching =
                                            history.entries
                                                .filter { it.positionMs > 0 }
                                                .take(10),
                                        onOpenHistoryEntry = { entry ->
                                            openVideo(entry.videoId, entry.title, entry.thumbnailUrl)
                                        },
                                        onRemoveHistoryEntry = { videoId ->
                                            saveHistory(history.without(videoId))
                                        },
                                        onOpenHistory = { router.push(AppDestination.History) },
                                    )
                                }

                                AppDestination.Subscriptions -> {
                                    SubscriptionsScreen(
                                        state = subscriptionsFeed,
                                        channels = subscriptions.channels,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                        onUnsubscribe = { channel ->
                                            val next = subscriptions.without(channel.id)
                                            saveSubscriptions(next)
                                            subscriptions = next
                                        },
                                        onRestoreSubscription = { channel, index ->
                                            val next = subscriptions.restoring(channel, index)
                                            saveSubscriptions(next)
                                            subscriptions = next
                                        },
                                    )
                                }

                                AppDestination.Music -> {
                                    MusicScreen(
                                        state = musicFeed,
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenArtist = { artistId, name, thumbnailUrl ->
                                            router.push(AppDestination.Artist(artistId, name, thumbnailUrl))
                                        },
                                        onSearch = { router.openTab(AppDestination.Search) },
                                        onOpenMoods = { router.push(AppDestination.Moods) },
                                        onOpenSettings = { router.push(AppDestination.Settings) },
                                        onOpenProfiles = { showProfiles = true },
                                    )
                                }

                                AppDestination.Moods -> {
                                    MoodsScreen(
                                        onBack = { router.pop() },
                                        onOpenMood = { genre ->
                                            router.push(
                                                AppDestination.Mood(
                                                    browseId = genre.browseId,
                                                    params = genre.params,
                                                    title = genre.title,
                                                ),
                                            )
                                        },
                                    )
                                }

                                is AppDestination.Mood -> {
                                    MoodScreen(
                                        browseId = destination.browseId,
                                        params = destination.params,
                                        title = destination.title,
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenArtist = { artistId, name, thumbnailUrl ->
                                            router.push(AppDestination.Artist(artistId, name, thumbnailUrl))
                                        },
                                        onBack = { router.pop() },
                                    )
                                }

                                is AppDestination.Artist -> {
                                    ArtistScreen(
                                        artistId = destination.artistId,
                                        fallbackName = destination.name,
                                        fallbackThumbnailUrl = destination.thumbnailUrl,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenArtist = { artistId, name, thumbnailUrl ->
                                            router.push(AppDestination.Artist(artistId, name, thumbnailUrl))
                                        },
                                        onBack = { router.pop() },
                                        onUnavailable = onRestoredPageUnavailable,
                                    )
                                }

                                AppDestination.Search -> {
                                    SearchScreen(
                                        state = searchFeed,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                    )
                                }

                                AppDestination.Library -> {
                                    LibraryScreen(
                                        store = libraryStore,
                                        downloads = downloads,
                                        downloadProgress = downloadProgress,
                                        downloadError = downloadError,
                                        onToggleDownload = { item ->
                                            toggleDownload(
                                                item.videoId,
                                                item.title,
                                                item.channelName,
                                                item.thumbnailUrl,
                                                null,
                                            )
                                        },
                                        onSearch = { router.openTab(AppDestination.Search) },
                                        // A Library row plays through the queue engine like everything
                                        // else, so it keeps playing (and stays in the notification)
                                        // when the screen goes away.
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            playAt(videoId, title, thumbnailUrl, 0)
                                        },
                                        onOpenHistory = { router.push(AppDestination.History) },
                                        localMedia = localMedia.items,
                                        onImportMedia = importMedia,
                                        onOpenLocal = playLocal,
                                        onRemoveLocal = { item -> saveLocalMedia(localMedia.without(item.uri)) },
                                        playlists = playlists,
                                        onOpenPlaylists = { router.push(AppDestination.Playlists) },
                                        onOpenSavedShorts = { router.push(AppDestination.SavedShorts) },
                                        onToggleInPlaylist = { playlistId, item ->
                                            val now = nowMillis()
                                            savePlaylists(
                                                if (playlists.contains(playlistId, item.videoId)) {
                                                    playlists.remove(playlistId, item.videoId, now)
                                                } else {
                                                    playlists.add(playlistId, item.toPlaylistVideo(now), now)
                                                },
                                            )
                                        },
                                        onCreatePlaylistWith = { name, item ->
                                            val now = nowMillis()
                                            val created = playlists.create(name, now)
                                            val playlistId = created.playlists.firstOrNull()?.id
                                            if (playlistId != null) {
                                                savePlaylists(created.add(playlistId, item.toPlaylistVideo(now), now))
                                            }
                                        },
                                    )
                                }

                                AppDestination.Explore -> {
                                    CategoriesScreen(
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            playAt(videoId, title, thumbnailUrl, 0)
                                        },
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenShelf = { browseId, params, title ->
                                            router.push(AppDestination.ExploreShelf(browseId, params, title))
                                        },
                                    )
                                }

                                is AppDestination.ExploreShelf -> {
                                    ExploreShelfScreen(
                                        browseId = destination.browseId,
                                        params = destination.params,
                                        title = destination.title,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            playAt(videoId, title, thumbnailUrl, 0)
                                        },
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onBack = { router.pop() },
                                    )
                                }

                                AppDestination.Shorts -> {
                                    ShortsScreen(
                                        libraryStore = libraryStore,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                    )
                                }

                                AppDestination.SavedShorts -> {
                                    SavedShortsScreen(
                                        store = libraryStore,
                                        onBack = { router.pop() },
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            playAt(videoId, title, thumbnailUrl, 0)
                                        },
                                    )
                                }

                                AppDestination.History -> {
                                    HistoryScreen(
                                        history = history,
                                        onOpen = { entry ->
                                            playAt(
                                                entry.videoId,
                                                entry.title,
                                                entry.thumbnailUrl,
                                                entry.positionMs,
                                            )
                                        },
                                        onRemove = { videoId -> saveHistory(history.without(videoId)) },
                                        onClear = { saveHistory(history.cleared()) },
                                        onBack = { router.pop() },
                                    )
                                }

                                AppDestination.Notes -> {
                                    NotesScreen(
                                        notes = notes.notes,
                                        onOpen = { note ->
                                            playAt(note.videoId, note.videoTitle, null, note.timestampMs)
                                        },
                                        onDelete = { id -> saveNotes(notes.without(id)) },
                                        onBack = { router.pop() },
                                    )
                                }

                                AppDestination.Playlists -> {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        onOpen = { playlistId ->
                                            router.push(AppDestination.LocalPlaylist(playlistId))
                                        },
                                        onCreate = { name -> savePlaylists(playlists.create(name, nowMillis())) },
                                        onRename = { playlistId, name ->
                                            savePlaylists(playlists.rename(playlistId, name, nowMillis()))
                                        },
                                        onDelete = { playlistId -> savePlaylists(playlists.delete(playlistId)) },
                                        onBack = { router.pop() },
                                    )
                                }

                                is AppDestination.LocalPlaylist -> {
                                    // A playlist deleted from its own page leaves nothing to draw, so
                                    // the page pops rather than read a record that is gone.
                                    val selected = playlists.find(destination.playlistId)
                                    if (selected == null) {
                                        LaunchedEffect(destination.playlistId) { router.pop() }
                                    } else {
                                        val tracks =
                                            selected.videos.map { video ->
                                                QueueTrack(video.videoId, video.title, video.thumbnailUrl)
                                            }
                                        UserPlaylistScreen(
                                            playlist = selected,
                                            onPlay = { shuffled -> playCollection(tracks, shuffled, 0) },
                                            onOpenIndex = { index -> playCollection(tracks, false, index) },
                                            onRemoveVideo = { videoId ->
                                                savePlaylists(
                                                    playlists.remove(selected.id, videoId, nowMillis()),
                                                )
                                            },
                                            onRename = { name ->
                                                savePlaylists(playlists.rename(selected.id, name, nowMillis()))
                                            },
                                            onDelete = {
                                                savePlaylists(playlists.delete(selected.id))
                                                router.pop()
                                            },
                                            onBack = { router.pop() },
                                        )
                                    }
                                }

                                AppDestination.Settings -> {
                                    SettingsScreen(
                                        settings = settings,
                                        version = appVersion,
                                        onAppearanceChange = { next ->
                                            val updated = settings.withAppearance(next)
                                            settings = updated
                                            saveAppSettingsJson(updated.encode())
                                        },
                                        onClearHistory = { saveHistory(history.cleared()) },
                                        onClearCache = { clearPlaybackCache() },
                                        onBack = { router.pop() },
                                    )
                                }

                                is AppDestination.Playlist -> {
                                    PlaylistScreen(
                                        playlistId = destination.playlistId,
                                        fallbackTitle = destination.title,
                                        libraryStore = libraryStore,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenArtist = { artistId, name ->
                                            router.push(AppDestination.Artist(artistId, name))
                                        },
                                        onPlayCollection = playCollection,
                                        onAddToQueue = queueTracks,
                                        onBack = { router.pop() },
                                        onUnavailable = onRestoredPageUnavailable,
                                    )
                                }

                                is AppDestination.Channel -> {
                                    val subscribed = subscriptions.isSubscribed(destination.channelId)
                                    ChannelScreen(
                                        channelId = destination.channelId,
                                        fallbackName = destination.name,
                                        subscribed = subscribed,
                                        onSubscriptionChange = toggleSubscription,
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenPlaylist = { playlistId, title, thumbnailUrl ->
                                            router.push(AppDestination.Playlist(playlistId, title, thumbnailUrl))
                                        },
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                        onOpenPost = { post ->
                                            post.commentEndpointParams?.let { params ->
                                                router.push(AppDestination.Post(post.id, params, post.authorName))
                                            }
                                        },
                                        onBack = { router.pop() },
                                        book = channelBook,
                                        onUnavailable = onRestoredPageUnavailable,
                                    )
                                }

                                is AppDestination.Post -> {
                                    PostScreen(
                                        postId = destination.postId,
                                        params = destination.params,
                                        fallbackAuthor = destination.authorName,
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                        onBack = { router.pop() },
                                    )
                                }

                                is AppDestination.Player -> {
                                    PlayerScreen(
                                        videoId = destination.videoId,
                                        fallbackTitle = destination.title,
                                        thumbnailUrl = destination.thumbnailUrl,
                                        onBack = { router.pop() },
                                        // A related card is a new watch page; back returns to the one it
                                        // was opened from, as the app's player stack does.
                                        onOpenVideo = { videoId, title, thumbnailUrl ->
                                            openVideo(videoId, title, thumbnailUrl)
                                        },
                                        onOpenChannel = { channelId, name ->
                                            router.push(AppDestination.Channel(channelId, name))
                                        },
                                        libraryStore = libraryStore,
                                        download = downloads.entry(destination.videoId),
                                        downloadProgress = downloadProgress[destination.videoId],
                                        downloadError = downloadError,
                                        onDownloadErrorShown = { downloadError = null },
                                        onDownload = { title, channelName, thumbnailUrl, stream ->
                                            toggleDownload(destination.videoId, title, channelName, thumbnailUrl, stream)
                                        },
                                        subscribed = { channelId -> subscriptions.isSubscribed(channelId) },
                                        onSubscriptionChange = toggleSubscription,
                                        onUnavailable = onRestoredPageUnavailable,
                                        // An imported file plays from the device itself, so the page
                                        // takes its stream instead of asking YouTube for one.
                                        localStream = localMedia.streamFor(destination.videoId),
                                        queue = playerQueue,
                                        onQueueStep = stepQueue,
                                        queuePlayer = queuePlayer.takeIf { isVideoPlaybackSupported },
                                        startPositionMs = queuePositionMs,
                                        onPosition = { index, positionMs ->
                                            // A page the queue has already left can still compose
                                            // once and report the track it was showing; only the
                                            // queue's current track may write the position the next
                                            // page resumes from, or a row opened further down a
                                            // playlist starts at the previous row's second.
                                            if (index == playerQueue?.index) {
                                                queuePositionMs = positionMs
                                                playerQueue?.current?.let { track ->
                                                    if (track.videoId == destination.videoId) {
                                                        historyWrite(
                                                            track.videoId,
                                                            track.title,
                                                            track.thumbnailUrl,
                                                            positionMs,
                                                            queuePlayer?.durationMs() ?: 0,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onQueueMode = queueModes,
                                        videoNotes = notes.forVideo(destination.videoId),
                                        onAddNote = { text, timestampMs ->
                                            val title =
                                                playerQueue
                                                    ?.current
                                                    ?.takeIf { it.videoId == destination.videoId }
                                                    ?.title
                                                    ?: destination.title
                                            saveNotes(
                                                notes.add(destination.videoId, title, timestampMs, text, nowMillis()),
                                            )
                                        },
                                        onWatched = { title, channelName, thumbnailUrl ->
                                            lastHistoryWriteMs = nowMillis()
                                            recordWatch(
                                                destination.videoId,
                                                title,
                                                channelName,
                                                thumbnailUrl ?: destination.thumbnailUrl,
                                                0,
                                                0,
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        if (showProfiles) {
                            FlowProfileSheet(
                                onDismiss = { showProfiles = false },
                                onOpenHistory = {
                                    showProfiles = false
                                    router.push(AppDestination.History)
                                },
                                onOpenNotes = {
                                    showProfiles = false
                                    router.push(AppDestination.Notes)
                                },
                                onOpenSettings = {
                                    showProfiles = false
                                    router.push(AppDestination.Settings)
                                },
                            )
                        }

                        // A tab keeps the sound it started, so the bar showing what is on above
                        // the navigation is the app's own mini player rather than a second engine.
                        val nowPlaying = playerQueue?.current
                        if (navDestination(destination) && nowPlaying != null) {
                            MiniPlayer(
                                player = queuePlayer,
                                title = nowPlaying.title,
                                subtitle = history.entry(nowPlaying.videoId)?.channelName,
                                artworkUrl = nowPlaying.thumbnailUrl,
                                onOpen = {
                                    router.push(
                                        AppDestination.Player(
                                            nowPlaying.videoId,
                                            nowPlaying.title,
                                            nowPlaying.thumbnailUrl,
                                        ),
                                    )
                                },
                                onClose = stopPlayback,
                            )
                        }

                        // The app's player is full screen and draws no chrome beneath it; a
                        // channel is a detail page pushed over the tab that opened it, so only the
                        // tabs carry the bar, and a wide window carries the rail instead.
                        if (!wide && navDestination(destination)) {
                            FlowNavigationBar(
                                selected = selectedTab(router),
                                onSelect = onSelectTab(router),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Home keeps neither tab selected while a detail is open; its parent tab stays highlighted. */
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** A library row as the playlist record keeps it: the metadata its row draws, and when it went on. */
private fun LibraryItem.toPlaylistVideo(nowMillis: Long): PlaylistVideo =
    PlaylistVideo(
        videoId = videoId,
        title = title,
        channelName = channelName,
        thumbnailUrl = thumbnailUrl,
        addedAtMillis = nowMillis,
    )

/** How often the playing position is folded into History while a track runs. */
private const val HISTORY_WRITE_INTERVAL_MS = 10_000L

private fun selectedTab(router: AppRouter): FlowTab? =
    when {
        router.current == AppDestination.Shorts -> FlowTab.SHORTS

        router.stack.first() == AppDestination.Music -> FlowTab.MUSIC

        router.stack.first() == AppDestination.Subscriptions -> FlowTab.SUBSCRIPTIONS

        router.stack.first() == AppDestination.Library -> FlowTab.LIBRARY

        router.stack.first() == AppDestination.Explore -> FlowTab.EXPLORE

        // Search is a page over the viewer's work, not one of the five tabs.
        router.stack.first() == AppDestination.Search -> null

        else -> FlowTab.HOME
    }

/**
 * The destinations the bar covers: the tabs, plus the reel feed, which the bar offers as one of
 * its own. A detail page is pushed over a tab and draws none.
 */
private fun navDestination(destination: AppDestination): Boolean =
    destination == AppDestination.Home ||
        destination == AppDestination.Shorts ||
        destination == AppDestination.Music ||
        destination == AppDestination.Subscriptions ||
        destination == AppDestination.Library ||
        destination == AppDestination.Explore ||
        destination == AppDestination.Search

/**
 * A tab replaces the stack; the reel feed is a page over the tab that opened it, so selecting it
 * pushes rather than resetting, and its back arrow returns where it came from.
 */
private fun onSelectTab(router: AppRouter): (FlowTab) -> Unit =
    { tab ->
        when (tab) {
            FlowTab.SHORTS -> {
                if (router.current != AppDestination.Shorts) router.push(AppDestination.Shorts)
            }

            FlowTab.HOME -> {
                router.openTab(AppDestination.Home)
            }

            FlowTab.MUSIC -> {
                router.openTab(AppDestination.Music)
            }

            FlowTab.SUBSCRIPTIONS -> {
                router.openTab(AppDestination.Subscriptions)
            }

            FlowTab.LIBRARY -> {
                router.openTab(AppDestination.Library)
            }

            FlowTab.EXPLORE -> {
                router.openTab(AppDestination.Explore)
            }
        }
    }

@Composable
private fun TopBar(
    router: AppRouter,
    destination: AppDestination,
    onOpenProfiles: () -> Unit,
) {
    when (destination) {
        AppDestination.Home -> {
            FlowTopBar(
                title = { HomeWordmark() },
                actions = {
                    IconButton(onClick = { router.openTab(AppDestination.Search) }) {
                        Icon(
                            imageVector = FlowIcons.Search,
                            contentDescription = stringResource(Res.string.action_search),
                        )
                    }
                    IconButton(onClick = { router.push(AppDestination.Settings) }) {
                        Icon(
                            imageVector = FlowIcons.SettingsOutlined,
                            contentDescription = stringResource(Res.string.settings_title),
                        )
                    }
                    FlowProfileAction(onClick = onOpenProfiles)
                },
            )
        }

        // Search draws its own bar with the query field in it, the way the app's search page does,
        // and the player has no bar at all.
        AppDestination.Music,
        AppDestination.Subscriptions,
        AppDestination.Library,
        AppDestination.Search,
        AppDestination.Shorts,
        AppDestination.Explore,
        AppDestination.Moods,
        AppDestination.History,
        AppDestination.SavedShorts,
        AppDestination.Notes,
        AppDestination.Playlists,
        AppDestination.Settings,
        is AppDestination.Player,
        is AppDestination.Post,
        is AppDestination.Channel,
        is AppDestination.Playlist,
        is AppDestination.Artist,
        is AppDestination.LocalPlaylist,
        is AppDestination.ExploreShelf,
        is AppDestination.Mood,
        -> {
            Unit
        }
    }
}

/** The app's Home wordmark: the red mark, then FLOW tracked out in extra bold. */
@Composable
private fun HomeWordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // The bar's leading slot carries more start padding than the reference app's, so the
        // mark is pulled back to where the reference draws it.
        modifier = Modifier.offset(x = (-12).dp),
    ) {
        FlowLogoMark()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.app_name_uppercase),
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
