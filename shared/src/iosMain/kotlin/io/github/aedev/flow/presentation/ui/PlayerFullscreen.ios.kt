package io.github.aedev.flow.presentation.ui

import androidx.compose.runtime.Composable

/** iOS has no playback surface yet, so there is no immersive mode to enter. */
@Composable
actual fun PlayerFullscreenEffect(fullscreen: Boolean) = Unit

@Composable
actual fun PlayerFullscreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
