package io.github.aedev.flow.presentation.local

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.aedev.flow.data.local.LocalMediaItem

actual val isMediaImportSupported: Boolean = true

@Composable
actual fun rememberMediaImporter(onPicked: (List<LocalMediaItem>) -> Unit): () -> Unit {
    val context = LocalContext.current
    // The callback is remembered through the launcher, which outlives a recomposition.
    val currentOnPicked = rememberUpdatedState(onPicked)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            val resolver = context.contentResolver
            val picked =
                uris.mapNotNull { uri ->
                    // The record outlives this process, so its permission has to as well.
                    runCatching {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    localMediaItem(uri, resolver)
                }
            if (picked.isNotEmpty()) currentOnPicked.value(picked)
        }
    return remember(launcher) { { launcher.launch(MEDIA_MIME_TYPES) } }
}

/**
 * What the library row needs about a picked document: the name the viewer sees, the type the player
 * dispatches on, and the size the row prints. A provider that answers nothing still yields a row,
 * named after the URI's last segment, rather than losing the pick.
 */
private fun localMediaItem(
    uri: Uri,
    resolver: ContentResolver,
): LocalMediaItem? {
    var displayName: String? = null
    var size = 0L
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
    }
    val name = displayName ?: uri.lastPathSegment ?: return null
    val title = name.substringBeforeLast('.', name).trim().ifBlank { name }
    return LocalMediaItem(
        uri = uri.toString(),
        title = title,
        mimeType = runCatching { resolver.getType(uri) }.getOrNull(),
        sizeBytes = size,
        addedAtMillis = System.currentTimeMillis(),
    )
}

private val MEDIA_MIME_TYPES = arrayOf("audio/*", "video/*")
