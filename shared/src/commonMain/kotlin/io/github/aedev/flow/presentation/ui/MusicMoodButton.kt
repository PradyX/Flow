package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

/**
 * The tones a mood button cycles through, the way the app's surfaces alternate their containers so
 * a run of buttons reads as one shelf rather than a wall of the same colour.
 */
internal enum class MusicMoodTone {
    Primary,
    Secondary,
    Tertiary,
    ;

    companion object {
        fun forIndex(index: Int): MusicMoodTone = entries[index % entries.size]
    }
}

/**
 * A mood or genre destination, the app's `MusicMoodButton`: one shape family with the music
 * surface's buttons, at the medium button height so a row of them is tappable without dominating.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MusicMoodButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    tone: MusicMoodTone = MusicMoodTone.Secondary,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) =
        when (tone) {
            MusicMoodTone.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
            MusicMoodTone.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
            MusicMoodTone.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        }
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        modifier =
            modifier
                .then(if (width != null) Modifier.width(width) else Modifier)
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
