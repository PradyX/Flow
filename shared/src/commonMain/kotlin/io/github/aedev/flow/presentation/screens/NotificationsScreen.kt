package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.notifications_empty_body
import io.github.aedev.flow.resources.notifications_empty_title
import io.github.aedev.flow.resources.notifications_open_settings
import io.github.aedev.flow.resources.notifications_title
import org.jetbrains.compose.resources.stringResource

private val StateIconContainerSize = 96.dp
private val StateIconSize = 44.dp
private val StatePadding = 32.dp
private val StateSpacing = 12.dp

/**
 * The inbox the bar's bell opens, the app's `NotificationScreen`. The app builds it from the
 * notification rows its `NotificationHelper` writes when a subscription refresh finds an upload;
 * this port has no notification record to write or read, so the page draws the empty inbox the app
 * shows when the list is empty, with the bar's settings action and the empty state's own.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationsScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
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
            },
        )
        EmptyInbox(onOpenSettings)
    }
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
