package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_current_profile
import io.github.aedev.flow.resources.history_title
import io.github.aedev.flow.resources.music_title
import io.github.aedev.flow.resources.nav_explore
import io.github.aedev.flow.resources.nav_home
import io.github.aedev.flow.resources.nav_library
import io.github.aedev.flow.resources.nav_subscriptions
import io.github.aedev.flow.resources.notes_title
import io.github.aedev.flow.resources.profile_local_desc
import io.github.aedev.flow.resources.profile_name
import io.github.aedev.flow.resources.profiles_title
import io.github.aedev.flow.resources.settings_title
import io.github.aedev.flow.resources.shorts_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The tabs the shared shell hosts. The app's own bar has more (`FlowTab`), but the port only has
 * screens behind these, so the bar names only what it can open. The reel feed is a destination the
 * app's bar carries beside Home, so it is one here too.
 */
enum class FlowTab(
    val label: StringResource,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HOME(Res.string.nav_home, FlowIcons.Home),
    SHORTS(Res.string.shorts_title, FlowIcons.Play),
    MUSIC(Res.string.music_title, FlowIcons.MusicNote),
    SUBSCRIPTIONS(Res.string.nav_subscriptions, FlowIcons.Subscriptions),
    LIBRARY(Res.string.nav_library, FlowIcons.Bookmark),
    EXPLORE(Res.string.nav_explore, FlowIcons.Explore),
}

/**
 * Where the app swaps its bottom bar for a side rail: the same window-width breakpoint Material's
 * expanded class uses. The app's own shell makes the same swap at the same width; a split-screen
 * pane stays well under it and keeps its bar.
 */
internal val WideLayoutMinWidth = 840.dp

/**
 * The single top bar for every shared screen, matching the app's `FlowTopBar`: painted on
 * `background` rather than `surface`, title in [MaterialTheme.typography.titleLarge] at bold, and
 * icons in `onSurfaceVariant`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlowTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

// The app's own mark, in its 24×24 viewport: the badge outline and the glyph cut out of it.
@Suppress("ktlint:standard:max-line-length")
private const val FLOW_LOGO_BADGE =
    "M21.58 7.16C21.33 6.22 20.59 5.48 19.65 5.23C17.96 4.77 12 4.77 12 4.77C12 4.77 6.04 4.77 4.35 5.23C3.41 5.48 2.67 6.22 2.42 7.16C1.96 8.85 1.96 12.38 1.96 12.38C1.96 12.38 1.96 15.91 2.42 17.6C2.67 18.54 3.41 19.28 4.35 19.53C6.04 19.99 12 19.99 12 19.99C12 19.99 17.96 19.99 19.65 19.53C20.59 19.28 21.33 18.54 21.58 17.6C22.04 15.91 22.04 12.38 22.04 12.38C22.04 12.38 22.04 8.85 21.58 7.16Z"
private const val FLOW_LOGO_GLYPH = "M10 7L18 7L17.2 9.5H12.8L12.2 11.5H16L15.2 14H11.5L10.5 17H7.5L10 7Z"

/** The Flow mark beside the wordmark on Home, drawn the way the app draws it. */
@Composable
internal fun FlowLogoMark(modifier: Modifier = Modifier.size(32.dp)) {
    val badgePath = remember { PathParser().parsePathString(FLOW_LOGO_BADGE).toPath() }
    val glyphPath = remember { PathParser().parsePathString(FLOW_LOGO_GLYPH).toPath() }
    val badgeColor = MaterialTheme.colorScheme.primary
    val glyphColor = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier) {
        scale(scaleX = size.width / 24f, scaleY = size.height / 24f, pivot = Offset.Zero) {
            drawPath(path = badgePath, color = badgeColor)
            drawPath(path = glyphPath, color = glyphColor)
        }
    }
}

/**
 * The bottom bar, on the app's own `ShortNavigationBar` with the same `surface` container, for a
 * window too narrow for the rail. It carries all six destinations the app's own bar does, labels
 * and all: every label is measurable and none clips at the tablet's narrowest width.
 */
@Composable
internal fun FlowNavigationBar(
    selected: FlowTab?,
    onSelect: (FlowTab) -> Unit,
) {
    ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        FlowTab.entries.forEach { tab ->
            ShortNavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.label)) },
            )
        }
    }
}

/**
 * The app's landscape navigation: the same destinations as the bar, down the left edge under the
 * Flow mark, on the shell's own background so it reads as chrome rather than a panel.
 */
@Composable
internal fun FlowNavigationRail(
    selected: FlowTab?,
    onSelect: (FlowTab) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        header = {
            Box(modifier = Modifier.padding(vertical = Dimensions.SectionSpacing)) {
                FlowLogoMark()
            }
        },
    ) {
        FlowTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.label)) },
            )
        }
    }
}

/** The avatar Home's bar carries last, on the app's `secondaryContainer` tones. */
@Composable
internal fun FlowProfileAction(onClick: () -> Unit) {
    val name = stringResource(Res.string.profile_name)
    IconButton(onClick = onClick) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/** The one profile this port runs with, in the app's profile sheet shape. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlowProfileSheet(
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()
    val name = stringResource(Res.string.profile_name)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = stringResource(Res.string.profiles_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text(stringResource(Res.string.profile_local_desc)) },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            trailingContent = {
                Icon(
                    imageVector = FlowIcons.Check,
                    contentDescription = stringResource(Res.string.action_current_profile),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            modifier = Modifier.padding(bottom = Dimensions.SectionSpacing),
        )
        ProfileRow(
            icon = FlowIcons.History,
            label = stringResource(Res.string.history_title),
            onClick = onOpenHistory,
        )
        ProfileRow(
            icon = FlowIcons.Note,
            label = stringResource(Res.string.notes_title),
            onClick = onOpenNotes,
        )
        ProfileRow(
            icon = FlowIcons.Tune,
            label = stringResource(Res.string.settings_title),
            onClick = onOpenSettings,
            modifier = Modifier.padding(bottom = Dimensions.SectionSpacing),
        )
    }
}

/** One destination in the profile sheet: its glyph, its name, and the page it opens. */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
