package io.github.aedev.flow.presentation.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aedev.flow.data.local.LocalMediaItem

actual val isMediaImportSupported: Boolean = false

/** Unreachable while the library hides its import control on this target. */
@Composable
actual fun rememberMediaImporter(onPicked: (List<LocalMediaItem>) -> Unit): () -> Unit = remember { { } }
