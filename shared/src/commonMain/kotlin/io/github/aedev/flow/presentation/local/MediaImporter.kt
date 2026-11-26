package io.github.aedev.flow.presentation.local

import androidx.compose.runtime.Composable
import io.github.aedev.flow.data.local.LocalMediaItem

/**
 * Whether this target can import files from the device. Android opens the system document picker;
 * the other targets have no picker yet, so their library shows no import control rather than one
 * that does nothing.
 */
expect val isMediaImportSupported: Boolean

/**
 * The import action: calling the returned lambda opens the platform's picker for audio and video
 * files, and [onPicked] receives what the viewer chose. A picked document's read access is taken for
 * as long as the row lives, so a file stays playable after a restart without being copied.
 */
@Composable
expect fun rememberMediaImporter(onPicked: (List<LocalMediaItem>) -> Unit): () -> Unit
