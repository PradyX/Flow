package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.settings.AUTOPLAY_DELAYS
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_close
import io.github.aedev.flow.resources.player_settings_loop
import io.github.aedev.flow.resources.player_settings_off
import io.github.aedev.flow.resources.player_settings_on
import io.github.aedev.flow.resources.player_settings_title
import io.github.aedev.flow.resources.settings_autoplay_delay
import io.github.aedev.flow.resources.settings_autoplay_delay_off
import io.github.aedev.flow.resources.settings_autoplay_delay_seconds
import io.github.aedev.flow.resources.settings_autoplay_next
import io.github.aedev.flow.resources.settings_autoplay_next_desc
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/**
 * The playback settings the reference opens from the player itself, over the surface it plays in:
 * what happens at the end of the track (loop, and the autoplay toggle with its countdown delay), so
 * a viewer never has to leave the player to change the way it hands off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSettingsSheet(
    autoplay: Boolean,
    autoplayDelaySeconds: Int,
    loop: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onAutoplayDelayChange: (Int) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Dimensions.ContentPaddingHorizontal),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.ContentPaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.player_settings_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = FlowIcons.Close,
                        contentDescription = stringResource(Res.string.action_close),
                    )
                }
            }
            // Each switch is the whole row, and its state is written beside its name: the player's
            // surface draws above the sheet's trailing edge on device, so a control pinned there would
            // not be visible; the state a viewer needs is in the part they can always see.
            ListItem(
                headlineContent = { Text(stringResource(Res.string.player_settings_loop)) },
                supportingContent = { Text(stateText(loop)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = loop, role = Role.Switch, onValueChange = onLoopChange),
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_autoplay_next)) },
                supportingContent = {
                    Text(
                        stringResource(Res.string.settings_autoplay_next_desc) + " · " + stateText(autoplay),
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = autoplay, role = Role.Switch, onValueChange = onAutoplayChange),
            )
            Text(
                text = stringResource(Res.string.settings_autoplay_delay),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        start = Dimensions.ContentPaddingHorizontal,
                        top = Dimensions.SectionSpacing,
                        bottom = 4.dp,
                    ),
            )
            AUTOPLAY_DELAYS.forEach { seconds ->
                val selected = autoplayDelaySeconds == seconds
                ListItem(
                    headlineContent = {
                        Text(
                            text =
                                if (seconds == 0) {
                                    stringResource(Res.string.settings_autoplay_delay_off)
                                } else {
                                    stringResource(Res.string.settings_autoplay_delay_seconds, seconds)
                                },
                        )
                    },
                    leadingContent = { RadioButton(selected = selected, onClick = null) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onAutoplayDelayChange(seconds) },
                            ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** The word a row prints for its own switched state. */
@Composable
private fun stateText(on: Boolean): String = stringResource(if (on) Res.string.player_settings_on else Res.string.player_settings_off)
