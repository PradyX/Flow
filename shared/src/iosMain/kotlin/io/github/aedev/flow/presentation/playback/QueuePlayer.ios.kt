package io.github.aedev.flow.presentation.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun rememberQueuePlayer(
    onTrackStarted: (videoId: String) -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    onCheckpoint: (videoId: String, positionMs: Long) -> Unit,
): QueuePlayer = NoQueuePlayer

@Composable
actual fun QueuePlayerSurface(
    player: QueuePlayer,
    modifier: Modifier,
) {
    Box(modifier = modifier)
}
