package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Artwork for an item, in the shape and size the app's cards give it. The monogram underneath is
 * what shows while the image downloads and if it fails; a loaded image covers it.
 *
 * YouTube's channel pages name a `hq720` poster even for uploads that never had one, so a failed
 * request retries once at the always-present `hqdefault` size before the monogram is left showing.
 */
@Composable
internal fun MediaArtwork(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val fallbackUrl = remember(imageUrl) { imageUrl?.let(::smallerThumbnail) }
    var failed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram(title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = if (failed) fallbackUrl else imageUrl,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
                onError = {
                    if (!failed && fallbackUrl != null) failed = true
                },
            )
        }
    }
}

/** The same poster at the size YouTube keeps for every video: `hq720`/`maxres` can be missing. */
private val LargerThumbnail = Regex("/(hq720|maxresdefault|sddefault)\\.jpg$")

internal fun smallerThumbnail(url: String): String? {
    val fallback = url.replace(LargerThumbnail, "/hqdefault.jpg")
    return fallback.takeIf { it != url }
}

/** A channel's avatar in the app's circle, falling back to its initial until the image arrives. */
@Composable
internal fun ChannelAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    diameter: Int = 40,
) {
    Box(
        modifier =
            modifier
                .size(diameter.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram(name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** The duration pill YouTube draws over the bottom-right of a thumbnail. */
@Composable
internal fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

private fun monogram(title: String): String =
    title
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        .orEmpty()

/** `754` seconds as `12:34`, and anything past an hour as `1:02:03`. */
internal fun durationText(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val rest = safe % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${rest.toString().padStart(2, '0')}"
    } else {
        "$minutes:${rest.toString().padStart(2, '0')}"
    }
}
