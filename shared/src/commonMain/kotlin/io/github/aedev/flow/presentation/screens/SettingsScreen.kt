package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.settings.AppSettings
import io.github.aedev.flow.data.settings.Appearance
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.appearance_amoled
import io.github.aedev.flow.resources.appearance_dark
import io.github.aedev.flow.resources.appearance_light
import io.github.aedev.flow.resources.appearance_material_you
import io.github.aedev.flow.resources.appearance_system
import io.github.aedev.flow.resources.settings_about
import io.github.aedev.flow.resources.settings_appearance
import io.github.aedev.flow.resources.settings_clear_cache
import io.github.aedev.flow.resources.settings_clear_cache_desc
import io.github.aedev.flow.resources.settings_clear_cache_done
import io.github.aedev.flow.resources.settings_clear_history
import io.github.aedev.flow.resources.settings_clear_history_desc
import io.github.aedev.flow.resources.settings_clear_history_done
import io.github.aedev.flow.resources.settings_data
import io.github.aedev.flow.resources.settings_title
import io.github.aedev.flow.resources.settings_version
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Settings, over the values this port actually owns: the appearance the shell renders with, the
 * records the app keeps (watch history, the buffered media holding disk space) and the build it is.
 * Every row here changes something the app then behaves by, rather than pointing at a page that
 * does not exist.
 */
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    version: String,
    onAppearanceChange: (Appearance) -> Unit,
    onClearHistory: () -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val historyCleared = stringResource(Res.string.settings_clear_history_done)
    val cacheCleared = stringResource(Res.string.settings_clear_cache_done)

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.settings_title),
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
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
        ) {
            SettingsSectionHeader(stringResource(Res.string.settings_appearance))
            AppearanceRow(
                label = stringResource(Res.string.appearance_system),
                selected = settings.appearance == Appearance.SYSTEM,
                onSelect = { onAppearanceChange(Appearance.SYSTEM) },
            )
            AppearanceRow(
                label = stringResource(Res.string.appearance_light),
                selected = settings.appearance == Appearance.LIGHT,
                onSelect = { onAppearanceChange(Appearance.LIGHT) },
            )
            AppearanceRow(
                label = stringResource(Res.string.appearance_dark),
                selected = settings.appearance == Appearance.DARK,
                onSelect = { onAppearanceChange(Appearance.DARK) },
            )
            AppearanceRow(
                label = stringResource(Res.string.appearance_amoled),
                selected = settings.appearance == Appearance.AMOLED,
                onSelect = { onAppearanceChange(Appearance.AMOLED) },
            )
            AppearanceRow(
                label = stringResource(Res.string.appearance_material_you),
                selected = settings.appearance == Appearance.MATERIAL_YOU,
                onSelect = { onAppearanceChange(Appearance.MATERIAL_YOU) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(Res.string.settings_data))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_clear_history)) },
                supportingContent = { Text(stringResource(Res.string.settings_clear_history_desc)) },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        onClearHistory()
                        scope.launch { snackbar.showSnackbar(historyCleared) }
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_clear_cache)) },
                supportingContent = { Text(stringResource(Res.string.settings_clear_cache_desc)) },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        onClearCache()
                        scope.launch { snackbar.showSnackbar(cacheCleared) }
                    },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(Res.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_version)) },
                supportingContent = { Text(version) },
            )
        }
        SnackbarHost(hostState = snackbar)
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(
                start = Dimensions.ContentPaddingHorizontal,
                top = Dimensions.SectionSpacing,
                bottom = 4.dp,
            ),
    )
}

@Composable
private fun AppearanceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    )
}
