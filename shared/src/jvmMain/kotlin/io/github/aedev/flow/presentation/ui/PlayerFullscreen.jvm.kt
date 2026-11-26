package io.github.aedev.flow.presentation.ui

import androidx.compose.runtime.Composable

/** The desktop window is the player's frame already; there is no immersive mode to enter. */
@Composable
actual fun PlayerFullscreenEffect(fullscreen: Boolean) = Unit

@Composable
actual fun PlayerFullscreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
