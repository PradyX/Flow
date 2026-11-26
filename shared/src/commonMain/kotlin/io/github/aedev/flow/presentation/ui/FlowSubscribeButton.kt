package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.subscribe
import io.github.aedev.flow.resources.subscribed
import io.github.aedev.flow.resources.unsubscribe
import org.jetbrains.compose.resources.stringResource

/**
 * The app's subscribe control: a primary pill reading "Subscribe", which becomes a tonal
 * "Subscribed" pill with the menu that unsubscribes. The reference's menu also holds notification,
 * Shorts and group switches; this port has no notification or group service, so Unsubscribe is the
 * one action it can honour.
 *
 * [compact] is Material's extra-small size, for a row that already carries an avatar, a channel
 * name and the reel's metadata beside it.
 */
@Composable
internal fun FlowSubscribeButton(
    subscribed: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val containerHeight =
        if (compact) ButtonDefaults.ExtraSmallContainerHeight else ButtonDefaults.MinHeight
    val contentPadding =
        if (compact) ButtonDefaults.ExtraSmallContentPadding else ButtonDefaults.ContentPadding

    Box(modifier = modifier) {
        if (subscribed) {
            FilledTonalButton(
                onClick = { menuOpen = true },
                contentPadding = contentPadding,
                modifier = Modifier.heightIn(min = containerHeight),
            ) {
                Text(text = stringResource(Res.string.subscribed), maxLines = 1)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = FlowIcons.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.unsubscribe)) },
                    leadingIcon = { Icon(FlowIcons.PersonRemove, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onUnsubscribe()
                    },
                )
            }
        } else {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSubscribe()
                },
                shape = CircleShape,
                contentPadding = contentPadding,
                modifier = Modifier.heightIn(min = containerHeight),
            ) {
                Text(text = stringResource(Res.string.subscribe), maxLines = 1)
            }
        }
    }
}
