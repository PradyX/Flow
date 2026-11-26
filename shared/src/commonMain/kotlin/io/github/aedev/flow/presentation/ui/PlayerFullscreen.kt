package io.github.aedev.flow.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Puts the window behind the player into [fullscreen] on targets that own a window: Android hides
 * the system bars and turns the device to landscape until the effect leaves composition. The
 * desktop and iOS targets keep the player inline, so their implementations do nothing.
 */
@Composable
expect fun PlayerFullscreenEffect(fullscreen: Boolean)

/**
 * Routes the platform's back gesture to [onBack] while the player is in fullscreen, so a back press
 * leaves fullscreen before it leaves the player. Targets without a system back affordance do
 * nothing.
 */
@Composable
expect fun PlayerFullscreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
