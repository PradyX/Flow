package io.github.aedev.flow.presentation.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.taste.TasteChannel
import io.github.aedev.flow.data.taste.UserTaste
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.ArtistItem
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.onboarding_btn_back
import io.github.aedev.flow.resources.onboarding_btn_continue
import io.github.aedev.flow.resources.onboarding_btn_skip
import io.github.aedev.flow.resources.onboarding_get_started
import io.github.aedev.flow.resources.onboarding_import_unavailable
import io.github.aedev.flow.resources.onboarding_imported
import io.github.aedev.flow.resources.onboarding_pick_more
import io.github.aedev.flow.resources.onboarding_progress
import io.github.aedev.flow.resources.onboarding_restore_backup
import io.github.aedev.flow.resources.onboarding_start_watching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val TopBarHeight = 64.dp
private val TopBarStartPadding = 24.dp
private val TopBarEndPadding = 8.dp
private val ProgressHeight = 6.dp
private val BarSpacing = 12.dp
private val BottomBarPadding = 20.dp
private val ProgressGap = 4.dp
private const val CURRENT_SEGMENT_WEIGHT = 2.4f

/**
 * First-run setup, the shared equivalent of the app's onboarding screen: the same five steps in the
 * same order, the same progress bar, the same bottom actions and gating. The Android activity
 * persists completion and shows the shell once [onComplete] runs.
 */
@Composable
fun OnboardingScreen(
    onComplete: (UserTaste) -> Unit,
    onRestoreBackup: () -> Unit = {},
) {
    val platform = rememberOnboardingPlatform()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val importedTemplate = stringResource(Res.string.onboarding_imported)
    val unavailableMessage = stringResource(Res.string.onboarding_import_unavailable)

    var state by remember { mutableStateOf(OnboardingUiState()) }
    var shownStep by remember { mutableStateOf(state.step) }
    var backdropFrom by remember { mutableStateOf(state.step) }
    var interestsRevealed by remember { mutableStateOf(false) }
    val backdrop = remember { Animatable(1f) }
    val backdropSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val heroFrom = shownStep

    LaunchedEffect(state.step) {
        if (state.step == shownStep) return@LaunchedEffect
        backdropFrom = shownStep
        shownStep = state.step
        backdrop.snapTo(0f)
        backdrop.animateTo(1f, backdropSpec)
    }

    LaunchedEffect(state.query) {
        if (state.query.isBlank()) {
            state = state.copy(results = emptyList(), searching = false)
            return@LaunchedEffect
        }
        state = state.copy(searching = true)
        delay(SEARCH_DEBOUNCE_MS)
        val results = searchChannels(state.query)
        state = state.copy(results = results, searching = false)
    }

    LaunchedEffect(state.artistQuery, state.step) {
        if (state.step != OnboardingStep.Artists) return@LaunchedEffect
        if (state.artistQuery.isBlank()) {
            if (state.artistCandidates.isNotEmpty()) return@LaunchedEffect
            state = state.copy(artistCandidates = popularArtists())
            return@LaunchedEffect
        }
        state = state.copy(artistSearching = true)
        delay(SEARCH_DEBOUNCE_MS)
        val results = searchArtists(state.artistQuery)
        state = state.copy(artistResults = results, artistSearching = false)
    }

    fun goTo(step: OnboardingStep) {
        state = state.copy(step = step)
    }

    fun next() {
        val following = OnboardingStep.entries.getOrNull(state.step.index + 1)
        if (following == null) onComplete(state.toUserTaste()) else goTo(following)
    }

    fun back() {
        OnboardingStep.entries.getOrNull(state.step.index - 1)?.let(::goTo)
    }

    fun startImport(kind: ImportKind) {
        platform.pickFiles(kind.mimeTypes, kind.takesSeveralFiles) { texts ->
            val channels = texts.flatMap(::parseNewPipeSubscriptions)
            if (kind == ImportKind.NewPipeSubscriptions && channels.isNotEmpty()) {
                state =
                    state.copy(
                        subscribed = (state.subscribed + channels).distinctBy { it.id },
                        importedSources = state.importedSources + ImportSource.NewPipe,
                        importedCount = state.importedCount + channels.size,
                    )
                scope.launch {
                    snackbar.showSnackbar(importedTemplate.format("${channels.size} subscriptions"))
                }
            } else {
                scope.launch { snackbar.showSnackbar(unavailableMessage) }
            }
        }
    }

    val actions =
        StepActions(
            platform = platform,
            onTopicToggle = { topic ->
                state =
                    state.copy(
                        topics = if (topic in state.topics) state.topics - topic else state.topics + topic,
                    )
            },
            onInterestsRevealed = { interestsRevealed = true },
            onQueryChange = { state = state.copy(query = it) },
            onSubscribeToggle = { channel ->
                state =
                    if (state.isSubscribed(channel.id)) {
                        state.copy(
                            subscribed = state.subscribed.filterNot { it.id == channel.id },
                            notifying = state.notifying - channel.id,
                        )
                    } else {
                        state.copy(subscribed = state.subscribed + channel)
                    }
            },
            onNotificationsChange = { channelId, enabled ->
                state =
                    state.copy(
                        notifying = if (enabled) state.notifying + channelId else state.notifying - channelId,
                    )
            },
            onArtistQuery = { state = state.copy(artistQuery = it) },
            onArtistToggle = { artist ->
                state =
                    state.copy(
                        pickedArtists =
                            if (state.isPicked(artist.id)) {
                                state.pickedArtists.filterNot { it.id == artist.id }
                            } else {
                                state.pickedArtists + artist
                            },
                    )
            },
            onNewVideoAlerts = { state = state.copy(newVideoAlerts = it) },
            onImport = ::startImport,
            onEdit = ::goTo,
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .onboardingBackdrop(
                    fromStep = backdropFrom,
                    step = shownStep,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) { backdrop.value },
        )
        Column(Modifier.fillMaxSize()) {
            OnboardingTopBar(step = state.step, onSkip = ::next)
            Box(Modifier.weight(1f)) {
                OnboardingSteps(
                    state = state,
                    heroFrom = heroFrom,
                    interestsRevealed = interestsRevealed,
                    actions = actions,
                )
            }
            OnboardingBottomBar(
                state = state,
                onBack = ::back,
                onNext = ::next,
                onRestore = onRestoreBackup,
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }
}

@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    onSkip: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TopBarHeight)
                .padding(start = TopBarStartPadding, end = TopBarEndPadding),
        horizontalArrangement = Arrangement.spacedBy(BarSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!step.showsProgress) return@Row
        StepProgress(step, Modifier.weight(1f))
        TextButton(onClick = onSkip) { Text(stringResource(Res.string.onboarding_btn_skip)) }
    }
}

@Composable
private fun StepProgress(
    step: OnboardingStep,
    modifier: Modifier,
) {
    val steps = OnboardingStep.progressSteps
    val current = steps.indexOf(step)
    val label = stringResource(Res.string.onboarding_progress, current + 1, steps.size)
    Box(
        modifier =
            modifier
                .height(ProgressHeight)
                .semantics { contentDescription = label }
                .drawSegmentedProgress(
                    count = steps.size,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    weight = { index -> if (index == current) CURRENT_SEGMENT_WEIGHT else 1f },
                ) { index -> if (index <= current) 1f else 0f },
    )
}

@Composable
private fun OnboardingBottomBar(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BottomBarPadding),
        horizontalArrangement = Arrangement.spacedBy(BarSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.step == OnboardingStep.Welcome) {
            TextButton(
                onClick = onRestore,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(stringResource(Res.string.onboarding_restore_backup))
            }
            ActionButton(
                text = stringResource(Res.string.onboarding_get_started),
                onClick = onNext,
                modifier = Modifier.weight(1f),
                trailing = FlowIcons.ArrowForward,
            )
            return@Row
        }
        FilledTonalIconButton(onClick = onBack) {
            Icon(
                imageVector = FlowIcons.Back,
                contentDescription = stringResource(Res.string.onboarding_btn_back),
            )
        }
        when {
            state.step == OnboardingStep.Ready -> {
                ActionButton(
                    text = stringResource(Res.string.onboarding_start_watching),
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    leading = FlowIcons.Play,
                )
            }

            !state.canAdvance -> {
                ActionButton(
                    text = stringResource(Res.string.onboarding_pick_more, state.topicsLeft),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                )
            }

            else -> {
                ActionButton(
                    text = stringResource(Res.string.onboarding_btn_continue),
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    trailing = FlowIcons.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val iconSize = 18.dp
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        leading?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            Icon(it, contentDescription = null, modifier = Modifier.size(iconSize))
        }
    }
}

/** A row of rounded segments, each filled by [fill] and as wide as its share of [weight]. */
private fun Modifier.drawSegmentedProgress(
    count: Int,
    color: Color,
    trackColor: Color,
    gap: Dp = ProgressGap,
    weight: (Int) -> Float = { 1f },
    fill: (Int) -> Float,
): Modifier =
    drawBehind {
        if (count <= 0) return@drawBehind
        val gapPx = gap.toPx()
        val available = size.width - gapPx * (count - 1)
        var totalWeight = 0f
        repeat(count) { totalWeight += weight(it) }
        val radius = CornerRadius(size.height / 2f)
        var x = 0f
        repeat(count) { index ->
            val width = available * weight(index) / totalWeight
            drawRoundRect(trackColor, Offset(x, 0f), Size(width, size.height), radius)
            val filled = fill(index).coerceIn(0f, 1f)
            if (filled > 0f) drawRoundRect(color, Offset(x, 0f), Size(width * filled, size.height), radius)
            x += width + gapPx
        }
    }

/** Maps the music search's profile results onto the setup's channel rows. */
private suspend fun searchChannels(query: String): List<OnboardingChannel> =
    YouTube.search(query, YouTube.SearchFilter.FILTER_PROFILE).getOrNull()?.items.orEmpty().mapNotNull { item ->
        val profile = item as? ArtistItem ?: return@mapNotNull null
        OnboardingChannel(
            id = profile.id,
            name = profile.title,
            thumbnailUrl = profile.thumbnail,
            subscriberCountText = profile.subscriberCountText,
        )
    }

/**
 * "Popular now": the artists the app's own catalog leads with, each resolved through the music
 * search so the row carries the real id and artwork. A seed the search cannot answer still shows,
 * so the list is never empty.
 */
private val PopularArtistSeeds =
    listOf(
        "Drake",
        "Taylor Swift",
        "Bad Bunny",
        "The Weeknd",
        "Billie Eilish",
        "Kendrick Lamar",
        "Ariana Grande",
        "Ed Sheeran",
        "Dua Lipa",
        "Post Malone",
        "Rihanna",
        "Bruno Mars",
    )

private suspend fun popularArtists(): List<OnboardingArtist> =
    coroutineScope {
        PopularArtistSeeds
            .map { seed ->
                async {
                    searchArtists(seed).firstOrNull() ?: OnboardingArtist(id = seed, name = seed, thumbnailUrl = null)
                }
            }.awaitAll()
    }

/** Maps the music search's artist results onto the setup's artist rows. */
private suspend fun searchArtists(query: String): List<OnboardingArtist> =
    YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()?.items.orEmpty().mapNotNull { item ->
        val artist = item as? ArtistItem ?: return@mapNotNull null
        OnboardingArtist(id = artist.id, name = artist.title, thumbnailUrl = artist.thumbnail)
    }

/** The setup's answers as the taste Home ranks with. */
private fun OnboardingUiState.toUserTaste(): UserTaste =
    UserTaste(
        topics = topics.toList(),
        channels = subscribed.map { TasteChannel(it.id, it.name, it.thumbnailUrl) },
        artists = pickedArtists.map { it.name },
    )
