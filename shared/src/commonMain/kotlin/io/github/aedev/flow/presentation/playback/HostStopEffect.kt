package io.github.aedev.flow.presentation.playback

import androidx.compose.runtime.Composable

/**
 * Runs [onStop] when the host goes to the background, which is the last guaranteed moment to write
 * the player down. Targets without a composition lifecycle simply never call it.
 */
@Composable
expect fun HostStopEffect(onStop: () -> Unit)
