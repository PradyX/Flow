package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.presentation.playback.QueuePlayer
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.mini_player_close
import io.github.aedev.flow.resources.mini_player_pause
import io.github.aedev.flow.resources.mini_player_play
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The now-playing bar above the navigation: the same session the full player and the notification
 * show, in one row. It holds no player of its own — the position, the play state and the actions all
 * come from the queue engine, so pausing here pauses the notification and the full screen too.
 */
@Composable
internal fun MiniPlayer(
    player: QueuePlayer,
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember { mutableLongStateOf(player.positionMs()) }
    var durationMs by remember { mutableLongStateOf(player.durationMs()) }
    var playing by remember { mutableStateOf(player.isPlaying()) }
    // The bar is only composed on a tab, so this tick stops the moment the full player, another app
    // or the screen takes over, and it never runs while nothing is playing.
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.positionMs()
            durationMs = player.durationMs()
            playing = player.isPlaying()
            delay(MINI_PLAYER_TICK_MS)
        }
    }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column {
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = MiniPlayerPadding, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only the track itself opens the player: the two actions beside it are siblings,
                // so pressing close cannot also navigate.
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onOpen)
                            .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(MiniPlayerArtwork).clip(MaterialTheme.shapes.small),
                    )
                    Spacer(modifier = Modifier.width(MiniPlayerPadding))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        subtitle?.takeIf { it.isNotBlank() }?.let { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = clockText(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        if (playing) player.pause() else player.play()
                        // The engine reports the change on the next tick; the glyph follows at once
                        // so the bar never lags the press.
                        playing = !playing
                    },
                ) {
                    Icon(
                        imageVector = if (playing) FlowIcons.Pause else FlowIcons.Play,
                        contentDescription =
                            stringResource(
                                if (playing) Res.string.mini_player_pause else Res.string.mini_player_play,
                            ),
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = FlowIcons.Close,
                        contentDescription = stringResource(Res.string.mini_player_close),
                    )
                }
            }
        }
    }
}

/** A position as the bar prints it: minutes and seconds, hours only when there are any. */
internal fun clockText(millis: Long): String {
    val total = (millis.coerceAtLeast(0) / 1_000).toInt()
    val hours = total / 3_600
    val minutes = (total % 3_600) / 60
    val seconds = total % 60
    val secondsText = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$secondsText"
    } else {
        "$minutes:$secondsText"
    }
}

private val MiniPlayerArtwork = 44.dp
private val MiniPlayerPadding = 12.dp

/** Fast enough that the position reads as live, slow enough to cost nothing while it runs. */
private const val MINI_PLAYER_TICK_MS = 500L
