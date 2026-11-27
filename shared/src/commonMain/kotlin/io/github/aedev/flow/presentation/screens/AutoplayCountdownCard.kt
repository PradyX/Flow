package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.presentation.playback.AutoplayCountdown
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.autoplay_cancel
import io.github.aedev.flow.resources.autoplay_countdown_up_next
import io.github.aedev.flow.resources.autoplay_next_video
import io.github.aedev.flow.resources.autoplay_play_now
import io.github.aedev.flow.resources.autoplay_restart
import org.jetbrains.compose.resources.stringResource

/**
 * The next-video card the reference lays over a finished video: a scrim, the seconds left in a ring,
 * the next video's name and channel, its frame, and the three things a viewer can do about it. The
 * port draws it over the player surface, so the countdown never leaves the page the video ended on.
 */
@Composable
internal fun AutoplayCountdownCard(
    countdown: AutoplayCountdown,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onPlayNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(COUNTDOWN_RING)) {
                        CircularProgressIndicator(
                            progress = {
                                if (countdown.totalSeconds > 0) {
                                    countdown.secondsRemaining.toFloat() / countdown.totalSeconds
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = countdown.secondsRemaining.coerceAtLeast(0).toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.autoplay_countdown_up_next),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text =
                                countdown.track.title.takeIf { it.isNotBlank() }
                                    ?: stringResource(Res.string.autoplay_next_video),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        countdown.channel?.takeIf { it.isNotBlank() }?.let { channel ->
                            Text(
                                text = channel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    countdown.track.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { thumb ->
                        Spacer(modifier = Modifier.width(12.dp))
                        AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .width(THUMBNAIL_WIDTH)
                                    .height(THUMBNAIL_HEIGHT)
                                    .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(Res.string.autoplay_cancel))
                    }
                    OutlinedButton(onClick = onRestart) {
                        Icon(
                            imageVector = FlowIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.autoplay_restart))
                    }
                    Button(onClick = onPlayNow) {
                        Icon(
                            imageVector = FlowIcons.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.autoplay_play_now))
                    }
                }
            }
        }
    }
}

/** The scrim the reference lays between the card and the finished video. */
private const val SCRIM_ALPHA = 0.55f

private val COUNTDOWN_RING = 44.dp
private val THUMBNAIL_WIDTH = 72.dp
private val THUMBNAIL_HEIGHT = 40.dp
