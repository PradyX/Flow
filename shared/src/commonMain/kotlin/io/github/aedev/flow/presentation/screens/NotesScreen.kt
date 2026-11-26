package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.notes.VideoNote
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.notes_at
import io.github.aedev.flow.resources.notes_delete
import io.github.aedev.flow.resources.notes_empty
import io.github.aedev.flow.resources.notes_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/**
 * Every note the viewer wrote, newest first, each naming the video and the moment it is about.
 * Tapping one reopens that video where the note points; the bin beside it deletes the note.
 */
@Composable
internal fun NotesScreen(
    notes: List<VideoNote>,
    onOpen: (VideoNote) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.notes_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FlowIcons.Back,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
            },
        )
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.notes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items = notes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onOpen = { onOpen(note) },
                        onDelete = { onDelete(note.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: VideoNote,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = note.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text =
                    if (note.timestampMs > 0) {
                        "${note.videoTitle} · " +
                            stringResource(Res.string.notes_at, durationText((note.timestampMs / 1000).toInt()))
                    } else {
                        note.videoTitle
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = FlowIcons.Delete,
                    contentDescription = stringResource(Res.string.notes_delete),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    )
}
