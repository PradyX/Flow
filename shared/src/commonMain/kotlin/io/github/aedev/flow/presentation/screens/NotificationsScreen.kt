package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.notifications.InboxNotification
import io.github.aedev.flow.data.notifications.InboxSection
import io.github.aedev.flow.data.notifications.InboxTime
import io.github.aedev.flow.data.notifications.NotificationInbox
import io.github.aedev.flow.data.notifications.deviceDayNumber
import io.github.aedev.flow.data.notifications.inboxTime
import io.github.aedev.flow.data.notifications.sectionOf
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowRowGroup
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.flowRowGroupShape
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.action_undo
import io.github.aedev.flow.resources.inbox_clear_action
import io.github.aedev.flow.resources.inbox_clear_body
import io.github.aedev.flow.resources.inbox_clear_confirm
import io.github.aedev.flow.resources.inbox_clear_title
import io.github.aedev.flow.resources.inbox_meta
import io.github.aedev.flow.resources.inbox_remove
import io.github.aedev.flow.resources.inbox_removed
import io.github.aedev.flow.resources.inbox_section_earlier
import io.github.aedev.flow.resources.inbox_section_last_week
import io.github.aedev.flow.resources.inbox_section_new
import io.github.aedev.flow.resources.inbox_section_today
import io.github.aedev.flow.resources.inbox_section_yesterday
import io.github.aedev.flow.resources.inbox_time_days
import io.github.aedev.flow.resources.inbox_time_hours
import io.github.aedev.flow.resources.inbox_time_just_now
import io.github.aedev.flow.resources.inbox_time_minutes
import io.github.aedev.flow.resources.notifications_empty_body
import io.github.aedev.flow.resources.notifications_empty_title
import io.github.aedev.flow.resources.notifications_open_settings
import io.github.aedev.flow.resources.notifications_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val StateIconContainerSize = 96.dp
private val StateIconSize = 44.dp
private val StatePadding = 32.dp
private val StateSpacing = 12.dp

private const val THUMBNAIL_WIDTH = 120
private const val THUMBNAIL_ASPECT = 0.5625f
private val RowPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
private val MetaSpacing = 6.dp
private val SwipeIconPadding = 24.dp

/**
 * The inbox the bar's bell opens, the app's `NotificationScreen`. The app builds it from the rows a
 * subscription check writes when it finds an upload; this port's check is the Subscriptions tab's
 * own refresh, so the rows are the uploads that page read after each channel's last visit.
 *
 * The page marks the inbox read as it opens and keeps the rows it found unread marked `New` for the
 * visit; swiping a row away offers Undo, and the bar's clear action asks before emptying the list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationsScreen(
    inbox: NotificationInbox,
    nowMillis: Long,
    onOpen: (InboxNotification) -> Unit,
    onRemove: (InboxNotification, Int) -> Unit,
    onUndoRemove: () -> Unit,
    onClearAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onOpened: () -> Unit,
) {
    // The unread snapshot the page opened with, taken before the inbox is marked read.
    val newIds = remember { inbox.entries.filterNot { it.isRead }.mapTo(HashSet()) { it.videoId } }
    LaunchedEffect(Unit) { onOpened() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    val removedMessage = stringResource(Res.string.inbox_removed)
    val undoLabel = stringResource(Res.string.action_undo)

    val todayNumber = deviceDayNumber(nowMillis)
    val sections =
        remember(inbox.entries, newIds, todayNumber) {
            inbox.entries
                .map { it to sectionOf(it, newIds, todayNumber) }
                .groupBy({ it.second }, { it.first })
                .toSortedMap()
                .map { (section, rows) -> section to rows }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FlowTopBar(
                title = {
                    Text(
                        text = stringResource(Res.string.notifications_title),
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
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = FlowIcons.SettingsOutlined,
                            contentDescription = stringResource(Res.string.notifications_open_settings),
                        )
                    }
                    if (inbox.entries.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(
                                imageVector = FlowIcons.Delete,
                                contentDescription = stringResource(Res.string.inbox_clear_action),
                            )
                        }
                    }
                },
            )
            if (sections.isEmpty()) {
                EmptyInbox(onOpenSettings)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                ) {
                    sections.forEach { (section, rows) ->
                        item(key = "header-${section.name}", contentType = "header") {
                            SectionHeader(stringResource(section.titleRes()))
                        }
                        item(key = "group-${section.name}", contentType = "group") {
                            FlowRowGroup {
                                rows.forEachIndexed { index, notification ->
                                    // Keyed by video: without this the row below a removed one inherits
                                    // the removed row's swipe state and is dismissed in the same frame.
                                    key(notification.videoId) {
                                        InboxRow(
                                            notification = notification,
                                            isNew = section == InboxSection.NEW,
                                            time = timeLabel(notification, section, nowMillis),
                                            shape = flowRowGroupShape(index, rows.size),
                                            onClick = { onOpen(notification) },
                                            onDismiss = {
                                                onRemove(notification, index)
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    val result =
                                                        snackbarHostState.showSnackbar(
                                                            removedMessage,
                                                            undoLabel,
                                                            duration = SnackbarDuration.Short,
                                                        )
                                                    if (result == SnackbarResult.ActionPerformed) onUndoRemove()
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (confirmClear) {
        ClearAllDialog(
            count = inbox.entries.size,
            onConfirm = {
                confirmClear = false
                onClearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

private fun InboxSection.titleRes() =
    when (this) {
        InboxSection.NEW -> Res.string.inbox_section_new
        InboxSection.TODAY -> Res.string.inbox_section_today
        InboxSection.YESTERDAY -> Res.string.inbox_section_yesterday
        InboxSection.LAST_WEEK -> Res.string.inbox_section_last_week
        InboxSection.EARLIER -> Res.string.inbox_section_earlier
    }

@Composable
private fun timeLabel(
    notification: InboxNotification,
    section: InboxSection,
    nowMillis: Long,
): String =
    when (val time = inboxTime(notification, section, nowMillis)) {
        is InboxTime.JustNow -> stringResource(Res.string.inbox_time_just_now)
        is InboxTime.Minutes -> pluralStringResource(Res.plurals.inbox_time_minutes, time.value.toInt(), time.value.toInt())
        is InboxTime.Hours -> pluralStringResource(Res.plurals.inbox_time_hours, time.value.toInt(), time.value.toInt())
        is InboxTime.Days -> pluralStringResource(Res.plurals.inbox_time_days, time.value.toInt(), time.value.toInt())
        is InboxTime.Clock -> time.text
        is InboxTime.Weekday -> time.text
        is InboxTime.Date -> time.text
    }

/** One inbox row: the upload's still, its title, then the channel and when it arrived. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InboxRow(
    notification: InboxNotification,
    isNew: Boolean,
    time: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.clip(shape),
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                // The row leaves the list, but a group that re-composes before it does would hand
                // the same dismissal back: settle the state so a row is only ever removed once.
                scope.launch { dismissState.reset() }
            }
        },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.errorContainer)
                        .padding(horizontal = SwipeIconPadding),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = FlowIcons.Delete,
                    contentDescription = stringResource(Res.string.inbox_remove),
                    tint = colors.onErrorContainer,
                )
            }
        },
    ) {
        SegmentedListItem(
            onClick = onClick,
            shapes = ListItemDefaults.shapes(shape = shape),
            modifier =
                if (isNew) {
                    Modifier.semantics { stateDescription = NEW_ROW_LABEL }
                } else {
                    Modifier
                },
            verticalAlignment = Alignment.CenterVertically,
            colors =
                ListItemDefaults.segmentedColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface,
                    supportingContentColor = colors.onSurfaceVariant,
                ),
            contentPadding = RowPadding,
            leadingContent = {
                AsyncImage(
                    model = notification.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(
                                width = THUMBNAIL_WIDTH.dp,
                                height = (THUMBNAIL_WIDTH * THUMBNAIL_ASPECT).dp,
                            ).clip(MaterialTheme.shapes.small),
                )
            },
            supportingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MetaSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isNew) Badge(containerColor = colors.primary) { }
                    Text(
                        text = stringResource(Res.string.inbox_meta, notification.channelName, time),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        ) {
            Text(
                text = notification.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isNew) FontWeight.SemiBold else null,
            )
        }
    }
}

/** The label a new row's accessibility state carries; the app reads "New" there too. */
private const val NEW_ROW_LABEL = "New"

@Composable
private fun ClearAllDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = FlowIcons.Delete, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.inbox_clear_title)) },
        text = { Text(text = pluralStringResource(Res.plurals.inbox_clear_body, count, count)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(Res.string.inbox_clear_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.action_cancel))
            }
        },
    )
}

/** The app's `EmptyInbox`: the state icon, the title, the body and the settings button it offers. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyInbox(onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(StatePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StateSpacing),
        ) {
            Surface(
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(StateIconContainerSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = FlowIcons.Bell,
                        contentDescription = null,
                        modifier = Modifier.size(StateIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.notifications_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.notifications_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onOpenSettings,
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = FlowIcons.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(end = ButtonDefaults.IconSpacing),
                )
                Text(text = stringResource(Res.string.notifications_open_settings))
            }
        }
    }
}
