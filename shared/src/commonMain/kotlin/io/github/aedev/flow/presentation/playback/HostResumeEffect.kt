package io.github.aedev.flow.presentation.playback

import androidx.compose.runtime.Composable

/**
 * Runs [onResume] each time the host comes back to the foreground, which is when a feed decides
 * whether what it holds is still fresh. Targets without a composition lifecycle never call it.
 */
@Composable
expect fun HostResumeEffect(onResume: () -> Unit)
