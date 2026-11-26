package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * The window properties the note editor opens with. Android has to draw the dialog outside the
 * system window insets, or the keyboard covers its buttons; the other targets take the default.
 */
@Composable
internal expect fun noteEditorDialogProperties(): DialogProperties

/**
 * Writes a note at a moment in the video. The moment is shown above the field so the note is
 * anchored to what the viewer was watching, and an empty field cannot be saved.
 */
@Composable
internal fun NoteEditorDialog(
    title: String,
    momentLabel: String?,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    saveLabel: String,
    cancelLabel: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = noteEditorDialogProperties(),
        modifier = Modifier.imePadding(),
        title = { Text(title) },
        text = {
            Column {
                if (momentLabel != null) {
                    Text(
                        text = momentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(placeholder) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = text.isNotBlank()) {
                Text(saveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
    )
}
