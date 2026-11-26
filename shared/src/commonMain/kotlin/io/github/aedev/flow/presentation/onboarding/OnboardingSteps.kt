@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.aedev.flow.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aedev.flow.data.recommendation.NeuroTopicCatalog
import io.github.aedev.flow.data.recommendation.TopicCategory
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowLogoMark
import io.github.aedev.flow.presentation.ui.FlowSegmentedGap
import io.github.aedev.flow.presentation.ui.flowRowGroupShape
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.app_name
import io.github.aedev.flow.resources.favourite_artists_picked
import io.github.aedev.flow.resources.favourite_artists_popular
import io.github.aedev.flow.resources.favourite_artists_results
import io.github.aedev.flow.resources.onboarding_alerts_allow
import io.github.aedev.flow.resources.onboarding_alerts_allow_summary
import io.github.aedev.flow.resources.onboarding_alerts_allowed
import io.github.aedev.flow.resources.onboarding_alerts_background
import io.github.aedev.flow.resources.onboarding_alerts_background_summary
import io.github.aedev.flow.resources.onboarding_alerts_footnote
import io.github.aedev.flow.resources.onboarding_alerts_new_videos
import io.github.aedev.flow.resources.onboarding_alerts_new_videos_summary
import io.github.aedev.flow.resources.onboarding_alerts_subtitle
import io.github.aedev.flow.resources.onboarding_alerts_title
import io.github.aedev.flow.resources.onboarding_artists_search_placeholder
import io.github.aedev.flow.resources.onboarding_artists_subtitle
import io.github.aedev.flow.resources.onboarding_artists_title
import io.github.aedev.flow.resources.onboarding_channels_added_count
import io.github.aedev.flow.resources.onboarding_channels_added_count_one
import io.github.aedev.flow.resources.onboarding_channels_empty_prompt
import io.github.aedev.flow.resources.onboarding_channels_no_results
import io.github.aedev.flow.resources.onboarding_channels_results_header
import io.github.aedev.flow.resources.onboarding_channels_search_placeholder
import io.github.aedev.flow.resources.onboarding_channels_subtitle
import io.github.aedev.flow.resources.onboarding_channels_title
import io.github.aedev.flow.resources.onboarding_import_count
import io.github.aedev.flow.resources.onboarding_import_footnote
import io.github.aedev.flow.resources.onboarding_import_subtitle
import io.github.aedev.flow.resources.onboarding_import_title
import io.github.aedev.flow.resources.onboarding_interests_hint
import io.github.aedev.flow.resources.onboarding_interests_ready
import io.github.aedev.flow.resources.onboarding_interests_title
import io.github.aedev.flow.resources.onboarding_ready_artists
import io.github.aedev.flow.resources.onboarding_ready_channels
import io.github.aedev.flow.resources.onboarding_ready_imports
import io.github.aedev.flow.resources.onboarding_ready_interests
import io.github.aedev.flow.resources.onboarding_ready_subtitle
import io.github.aedev.flow.resources.onboarding_ready_title
import io.github.aedev.flow.resources.onboarding_welcome_subtitle
import io.github.aedev.flow.resources.onboarding_welcome_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val StepPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
internal val PanePadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp)
internal val HeroStageSize = 300.dp

/** From this width the middle steps put their hero beside the list instead of above it. */
internal val TwoPaneWidth = 840.dp

private const val SIDE_PANE_WEIGHT = 0.4f
private const val MAX_QUICK_SEARCHES = 6
private val SectionSpacing = 28.dp
private val PillSpacing = 8.dp
private val HeaderSpacing = 8.dp
private val HeaderBottomPadding = 20.dp
private val HeaderIconSize = 18.dp
private val HeaderLetterSpacing = 1.5.sp
private val CountBadgePadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
private val StageTopPadding = 24.dp
private val TextSpacing = 10.dp
private val BrandSpacing = 8.dp
private val BrandLogoWidth = 26.dp
private val SatelliteIconSize = 24.dp
private val RowGap = 10.dp
private const val ALERT_ROWS = 3
private val FootnotePadding = 16.dp
private val TileSpacing = 10.dp
private val WideGridWidth = 600.dp
private const val NARROW_COLUMNS = 2
private const val WIDE_COLUMNS = 3

private class Satellite(
    val shape: Shape,
    val icon: ImageVector,
    val size: Dp,
    val offset: DpOffset,
)

/** The app's three welcome satellites, drawn in its own Material shapes. */
@Composable
private fun satellites(): List<Satellite> {
    val cookie = MaterialShapes.Cookie6Sided.toShape()
    val clover = MaterialShapes.Clover4Leaf.toShape()
    return remember(cookie, clover) {
        listOf(
            Satellite(cookie, FlowIcons.MusicNote, 64.dp, DpOffset((-112).dp, (-96).dp)),
            Satellite(clover, FlowIcons.VideoFrame, 56.dp, DpOffset(116.dp, (-104).dp)),
            Satellite(CircleShape, FlowIcons.MotionVideo, 60.dp, DpOffset(118.dp, 96.dp)),
        )
    }
}

/** A hero drawn at a given size; the screen builds it so one shape can morph between steps. */
internal typealias HeroSlot = @Composable (size: Dp) -> Unit

/** What a step needs from the screen besides its state. */
internal class StepActions(
    val platform: OnboardingPlatform,
    val onTopicToggle: (String) -> Unit,
    val onInterestsRevealed: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onSubscribeToggle: (OnboardingChannel) -> Unit,
    val onNotificationsChange: (String, Boolean) -> Unit,
    val onArtistQuery: (String) -> Unit,
    val onArtistToggle: (OnboardingArtist) -> Unit,
    val onNewVideoAlerts: (Boolean) -> Unit,
    val onImport: (ImportKind) -> Unit,
    val onEdit: (OnboardingStep) -> Unit,
)

/**
 * The step on screen, sliding between steps while the hero grows from the one before. From 840 dp
 * the middle steps put their hero, title and subtitle in a start pane beside the list.
 */
@Composable
internal fun OnboardingSteps(
    state: OnboardingUiState,
    heroFrom: OnboardingStep,
    interestsRevealed: Boolean,
    actions: StepActions,
) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val direction = if (targetState.index > initialState.index) 1 else -1
            (slideInHorizontally { direction * it / 4 } + fadeIn(spatial)) togetherWith
                (slideOutHorizontally { -direction * it / 4 } + fadeOut(spatial))
        },
        modifier = Modifier.fillMaxSize(),
        label = "onboardingStep",
    ) { step ->
        val hero: HeroSlot = { size ->
            OnboardingHero(
                step = step,
                fromStep = heroFrom,
                size = size,
                petals = (state.topics.size.toFloat() / MIN_TOPICS).coerceAtMost(1f),
            )
        }
        when (step) {
            OnboardingStep.Welcome -> {
                WelcomeStep(hero)
            }

            OnboardingStep.Ready -> {
                ReadyStep(state = state, hero = hero, onEdit = actions.onEdit)
            }

            else -> {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val twoPane = maxWidth >= TwoPaneWidth
                    val copy = stepCopy(state, step)
                    val padding = if (twoPane) PanePadding else StepPadding
                    if (twoPane) {
                        Row(Modifier.fillMaxSize()) {
                            StepSidePane(hero, copy, Modifier.weight(SIDE_PANE_WEIGHT).fillMaxHeight())
                            Box(Modifier.weight(1f - SIDE_PANE_WEIGHT).fillMaxHeight()) {
                                StepBody(
                                    step = step,
                                    state = state,
                                    showHeader = false,
                                    hero = hero,
                                    copy = copy,
                                    padding = padding,
                                    revealed = interestsRevealed,
                                    actions = actions,
                                )
                            }
                        }
                    } else {
                        StepBody(
                            step = step,
                            state = state,
                            showHeader = true,
                            hero = hero,
                            copy = copy,
                            padding = padding,
                            revealed = interestsRevealed,
                            actions = actions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBody(
    step: OnboardingStep,
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    padding: PaddingValues,
    revealed: Boolean,
    actions: StepActions,
) {
    when (step) {
        OnboardingStep.Interests -> {
            InterestsStep(
                state = state,
                showHeader = showHeader,
                hero = hero,
                copy = copy,
                revealed = revealed,
                onRevealed = actions.onInterestsRevealed,
                onTopicToggle = actions.onTopicToggle,
                padding = padding,
            )
        }

        OnboardingStep.Channels -> {
            ChannelsStep(
                state = state,
                showHeader = showHeader,
                hero = hero,
                copy = copy,
                onQueryChange = actions.onQueryChange,
                onSubscribeToggle = actions.onSubscribeToggle,
                onNotificationsChange = actions.onNotificationsChange,
                padding = padding,
            )
        }

        OnboardingStep.Artists -> {
            ArtistsStep(
                state = state,
                showHeader = showHeader,
                hero = hero,
                copy = copy,
                onQueryChange = actions.onArtistQuery,
                onToggle = actions.onArtistToggle,
                padding = padding,
            )
        }

        OnboardingStep.Alerts -> {
            AlertsStep(
                state = state,
                showHeader = showHeader,
                hero = hero,
                copy = copy,
                platform = actions.platform,
                onNewVideoAlerts = actions.onNewVideoAlerts,
                padding = padding,
            )
        }

        OnboardingStep.Import -> {
            ImportStep(
                state = state,
                showHeader = showHeader,
                hero = hero,
                copy = copy,
                onImport = actions.onImport,
                padding = padding,
            )
        }

        OnboardingStep.Welcome, OnboardingStep.Ready -> {
            Unit
        }
    }
}

@Composable
private fun WelcomeStep(hero: HeroSlot) {
    val copy = stepCopy(OnboardingUiState(), OnboardingStep.Welcome)
    BookendLayout(
        contentPadding = StepPadding,
        stage = {
            hero(HeroLarge)
            satellites().forEach { satellite ->
                Box(Modifier.offset(satellite.offset.x, satellite.offset.y)) {
                    Surface(
                        shape = satellite.shape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(satellite.size),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = satellite.icon,
                                contentDescription = null,
                                modifier = Modifier.size(SatelliteIconSize),
                            )
                        }
                    }
                }
            }
        },
        text = {
            BrandRow()
            Text(
                text = copy.title,
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun BrandRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BrandSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowLogoMark(Modifier.size(BrandLogoWidth))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun InterestsStep(
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    revealed: Boolean,
    onRevealed: () -> Unit,
    onTopicToggle: (String) -> Unit,
    padding: PaddingValues,
) {
    val categories = NeuroTopicCatalog.TOPIC_CATEGORIES
    var visibleSections by remember { mutableIntStateOf(if (revealed) categories.size else 0) }
    LaunchedEffect(Unit) {
        if (revealed) return@LaunchedEffect
        for (index in 1..categories.size) {
            delay(STAGGER_DELAY_MS)
            visibleSections = index
        }
        onRevealed()
    }

    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        if (showHeader) item(key = "header") { stepHeader(hero, copy.title, copy.subtitle) }
        itemsIndexed(categories, key = { _, category -> category.name }) { index, category ->
            AnimatedVisibility(
                visible = index < visibleSections,
                enter = fadeIn(spatial) + slideInVertically { it / 6 },
            ) {
                InterestSection(
                    category = category,
                    selectedTopics = state.topics,
                    onTopicToggle = onTopicToggle,
                )
            }
        }
    }
}

@Composable
private fun InterestSection(
    category: TopicCategory,
    selectedTopics: Set<String>,
    onTopicToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeaderSpacing)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
        ) {
            Icon(
                imageVector = FlowIcons.Sparkle,
                contentDescription = null,
                modifier = Modifier.size(HeaderIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = category.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = HeaderLetterSpacing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val selected = category.topics.count(selectedTopics::contains)
            if (selected > 0) {
                Text(
                    text = selected.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(CountBadgePadding),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PillSpacing),
            verticalArrangement = Arrangement.spacedBy(PillSpacing),
        ) {
            category.topics.forEach { topic ->
                FlowPillChip(
                    label = topic,
                    selected = topic in selectedTopics,
                    onClick = { onTopicToggle(topic) },
                )
            }
        }
    }
}

@Composable
private fun ChannelsStep(
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    onQueryChange: (String) -> Unit,
    onSubscribeToggle: (OnboardingChannel) -> Unit,
    onNotificationsChange: (String, Boolean) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    ) {
        if (showHeader) item(key = "header") { stepHeader(hero, copy.title, copy.subtitle) }
        item(key = "search") {
            OnboardingSearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(Res.string.onboarding_channels_search_placeholder),
                searching = state.searching,
            )
        }
        item(key = "quick") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PillSpacing),
                modifier = Modifier.padding(top = PillSpacing),
            ) {
                state.topics.take(MAX_QUICK_SEARCHES).forEach { topic ->
                    FilterChip(
                        selected = state.query == topic,
                        onClick = { onQueryChange(topic) },
                        label = { Text(topic, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        shape = CircleShape,
                    )
                }
            }
        }
        when {
            state.query.isBlank() -> {
                item(key = "prompt") {
                    OnboardingPrompt(stringResource(Res.string.onboarding_channels_empty_prompt))
                }
            }

            state.results.isEmpty() && !state.searching -> {
                item(key = "empty") {
                    OnboardingPrompt(stringResource(Res.string.onboarding_channels_no_results, state.query))
                }
            }

            else -> {
                item(key = "results") {
                    OnboardingSectionHeader(stringResource(Res.string.onboarding_channels_results_header))
                }
                itemsIndexed(state.results, key = { _, channel -> channel.id }) { index, channel ->
                    ChannelRow(
                        channel = channel,
                        subscribed = state.isSubscribed(channel.id),
                        notifying = channel.id in state.notifying,
                        onToggle = { onSubscribeToggle(channel) },
                        onNotificationsChange = { onNotificationsChange(channel.id, it) },
                        shape = flowRowGroupShape(index, state.results.size),
                    )
                }
            }
        }
        if (state.subscribed.isNotEmpty()) {
            item(key = "added") {
                Text(
                    text =
                        stringResource(
                            if (state.subscribed.size == 1) {
                                Res.string.onboarding_channels_added_count_one
                            } else {
                                Res.string.onboarding_channels_added_count
                            },
                            state.subscribed.size,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = FootnotePadding),
                )
            }
        }
    }
}

@Composable
private fun ArtistsStep(
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    onQueryChange: (String) -> Unit,
    onToggle: (OnboardingArtist) -> Unit,
    padding: PaddingValues,
) {
    val results = if (state.artistQuery.isBlank()) state.artistCandidates else state.artistResults
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    ) {
        if (showHeader) item(key = "header") { stepHeader(hero, copy.title, copy.subtitle) }
        item(key = "search") {
            OnboardingSearchField(
                query = state.artistQuery,
                onQueryChange = onQueryChange,
                placeholder = stringResource(Res.string.onboarding_artists_search_placeholder),
                searching = state.artistSearching,
            )
        }
        if (state.pickedArtists.isNotEmpty()) {
            item(key = "picked-header") {
                OnboardingSectionHeader(stringResource(Res.string.favourite_artists_picked))
            }
            itemsIndexed(state.pickedArtists, key = { _, artist -> "picked:${artist.id}" }) { index, artist ->
                ArtistRow(
                    artist = artist,
                    picked = true,
                    onToggle = { onToggle(artist) },
                    shape = flowRowGroupShape(index, state.pickedArtists.size),
                )
            }
        }
        if (results.isNotEmpty()) {
            item(key = "results-header") {
                OnboardingSectionHeader(
                    stringResource(
                        if (state.artistQuery.isBlank()) Res.string.favourite_artists_popular else Res.string.favourite_artists_results,
                    ),
                )
            }
            val unpicked = results.filterNot { state.isPicked(it.id) }
            itemsIndexed(unpicked, key = { _, artist -> "result:${artist.id}" }) { index, artist ->
                ArtistRow(
                    artist = artist,
                    picked = false,
                    onToggle = { onToggle(artist) },
                    shape = flowRowGroupShape(index, unpicked.size),
                )
            }
        }
    }
}

@Composable
private fun AlertsStep(
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    platform: OnboardingPlatform,
    onNewVideoAlerts: (Boolean) -> Unit,
    padding: PaddingValues,
) {
    val allowedText = stringResource(Res.string.onboarding_alerts_allowed)
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(platform.notificationsAllowed) }
    var backgroundAllowed by remember { mutableStateOf(platform.backgroundUnrestricted) }
    val refresh = {
        notificationsAllowed = platform.notificationsAllowed
        backgroundAllowed = platform.backgroundUnrestricted
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
        if (showHeader) item(key = "header") { stepHeader(hero, copy.title, copy.subtitle) }
        item(key = "rows") {
            Column(verticalArrangement = Arrangement.spacedBy(FlowSegmentedGap)) {
                OnboardingNavRow(
                    title = stringResource(Res.string.onboarding_alerts_allow),
                    supportingText =
                        if (notificationsAllowed) allowedText else stringResource(Res.string.onboarding_alerts_allow_summary),
                    icon = FlowIcons.Bell,
                    showChevron = !notificationsAllowed,
                    onClick = {
                        when {
                            notificationsAllowed -> {
                                Unit
                            }

                            askedOnce -> {
                                platform.openNotificationSettings()
                            }

                            else -> {
                                askedOnce = true
                                platform.requestNotifications()
                                refresh()
                            }
                        }
                    },
                    shape = flowRowGroupShape(0, ALERT_ROWS),
                )
                OnboardingSwitchRow(
                    title = stringResource(Res.string.onboarding_alerts_new_videos),
                    supportingText = stringResource(Res.string.onboarding_alerts_new_videos_summary),
                    icon = FlowIcons.Play,
                    checked = state.newVideoAlerts,
                    onCheckedChange = onNewVideoAlerts,
                    shape = flowRowGroupShape(1, ALERT_ROWS),
                )
                OnboardingNavRow(
                    title = stringResource(Res.string.onboarding_alerts_background),
                    supportingText =
                        if (backgroundAllowed) allowedText else stringResource(Res.string.onboarding_alerts_background_summary),
                    icon = FlowIcons.Download,
                    showChevron = !backgroundAllowed,
                    onClick = {
                        if (!backgroundAllowed) {
                            platform.requestUnrestrictedBackground()
                            refresh()
                        }
                    },
                    shape = flowRowGroupShape(2, ALERT_ROWS),
                )
            }
        }
        item(key = "footnote") {
            Text(
                text = stringResource(Res.string.onboarding_alerts_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(FootnotePadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportStep(
    state: OnboardingUiState,
    showHeader: Boolean,
    hero: HeroSlot,
    copy: StepCopy,
    onImport: (ImportKind) -> Unit,
    padding: PaddingValues,
) {
    var openSource by rememberSaveable { mutableStateOf<ImportSource?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= WideGridWidth) WIDE_COLUMNS else NARROW_COLUMNS
        val rows = ImportSource.entries.chunked(columns)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            if (showHeader) item(key = "header") { stepHeader(hero, copy.title, copy.subtitle) }
            items(rows, key = { row -> row.first().name }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(TileSpacing)) {
                    row.forEach { source ->
                        ImportTile(
                            title = stringResource(source.shortTitle),
                            kindCount = source.kinds.size,
                            imported = source in state.importedSources,
                            onClick = { openSource = source },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item(key = "footnote") {
                Text(
                    text = stringResource(Res.string.onboarding_import_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(FootnotePadding),
                )
            }
        }
    }

    openSource?.let { source ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { openSource = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(RowGap),
            ) {
                Text(
                    text = stringResource(source.shortTitle),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(8.dp),
                )
                source.kinds.forEach { kind ->
                    ImportKindRow(
                        title = stringResource(kind.title),
                        description = stringResource(kind.description),
                        onClick = {
                            openSource = null
                            onImport(kind)
                        },
                    )
                }
                Spacer(Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun ReadyStep(
    state: OnboardingUiState,
    hero: HeroSlot,
    onEdit: (OnboardingStep) -> Unit,
) {
    val copy = stepCopy(state, OnboardingStep.Ready)
    BookendLayout(
        contentPadding = StepPadding,
        stage = { hero(HeroReady) },
        text = {
            Text(
                text = copy.title,
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PillSpacing)) {
                SummaryChip(
                    text = stringResource(Res.string.onboarding_ready_interests, state.topics.size),
                    icon = FlowIcons.Interests,
                ) { onEdit(OnboardingStep.Interests) }
                SummaryChip(
                    text = stringResource(Res.string.onboarding_ready_channels, state.subscribed.size),
                    icon = FlowIcons.Play,
                ) { onEdit(OnboardingStep.Channels) }
                if (state.pickedArtists.isNotEmpty()) {
                    SummaryChip(
                        text = stringResource(Res.string.onboarding_ready_artists, state.pickedArtists.size),
                        icon = FlowIcons.MusicNote,
                    ) { onEdit(OnboardingStep.Artists) }
                }
                if (state.importedSources.isNotEmpty()) {
                    SummaryChip(
                        text = stringResource(Res.string.onboarding_ready_imports, state.importedSources.size),
                        icon = FlowIcons.Inbox,
                    ) { onEdit(OnboardingStep.Import) }
                }
            }
        },
    )
}

@Composable
private fun FlowPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = FlowIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        shape = CircleShape,
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}

@Composable
private fun SummaryChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
        },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        border = null,
    )
}

/** The Welcome and Ready layout: a stage for the large hero, then the text within thumb reach. */
@Composable
internal fun BookendLayout(
    contentPadding: PaddingValues,
    stage: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(contentPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = StageTopPadding),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(HeroStageSize), contentAlignment = Alignment.Center) { stage() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(TextSpacing)) { text() }
        }
    }
}

/** The hero, title and subtitle at the top of a step's list. */
@Composable
private fun stepHeader(
    hero: HeroSlot,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = HeaderBottomPadding),
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
    ) {
        hero(HeroSmall)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The start pane of the two-pane layout: the hero, a large title and the subtitle. */
@Composable
private fun StepSidePane(
    hero: HeroSlot,
    copy: StepCopy,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        hero(HeroSide)
        Text(
            text = copy.title,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = copy.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal data class StepCopy(
    val title: String,
    val subtitle: String,
)

@Composable
internal fun stepCopy(
    state: OnboardingUiState,
    step: OnboardingStep,
): StepCopy =
    when (step) {
        OnboardingStep.Welcome -> {
            StepCopy(
                stringResource(Res.string.onboarding_welcome_title),
                stringResource(Res.string.onboarding_welcome_subtitle),
            )
        }

        OnboardingStep.Interests -> {
            StepCopy(
                stringResource(Res.string.onboarding_interests_title),
                if (state.topicsLeft > 0) {
                    stringResource(Res.string.onboarding_interests_hint, MIN_TOPICS, state.topicsLeft)
                } else {
                    stringResource(Res.string.onboarding_interests_ready)
                },
            )
        }

        OnboardingStep.Channels -> {
            StepCopy(
                stringResource(Res.string.onboarding_channels_title),
                stringResource(Res.string.onboarding_channels_subtitle),
            )
        }

        OnboardingStep.Artists -> {
            StepCopy(
                stringResource(Res.string.onboarding_artists_title),
                stringResource(Res.string.onboarding_artists_subtitle),
            )
        }

        OnboardingStep.Alerts -> {
            StepCopy(
                stringResource(Res.string.onboarding_alerts_title),
                stringResource(Res.string.onboarding_alerts_subtitle),
            )
        }

        OnboardingStep.Import -> {
            StepCopy(
                stringResource(Res.string.onboarding_import_title),
                stringResource(Res.string.onboarding_import_subtitle),
            )
        }

        OnboardingStep.Ready -> {
            StepCopy(
                stringResource(Res.string.onboarding_ready_title),
                stringResource(Res.string.onboarding_ready_subtitle),
            )
        }
    }
