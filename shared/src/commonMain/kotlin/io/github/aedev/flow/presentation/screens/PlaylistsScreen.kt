package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.playlists.UserPlaylist
import io.github.aedev.flow.data.playlists.UserPlaylists
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.PlaylistNameDialog
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.playlist_create_action
import io.github.aedev.flow.resources.playlist_delete
import io.github.aedev.flow.resources.playlist_delete_text
import io.github.aedev.flow.resources.playlist_delete_title
import io.github.aedev.flow.resources.playlist_more_options
import io.github.aedev.flow.resources.playlist_name_hint
import io.github.aedev.flow.resources.playlist_rename
import io.github.aedev.flow.resources.playlist_video_count
import io.github.aedev.flow.resources.playlists_create
import io.github.aedev.flow.resources.playlists_empty
import io.github.aedev.flow.resources.playlists_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val PlaylistRowArtwork = 56.dp
private const val PLAYLIST_ROW_SPACING = 4

/**
 * The playlists this device made: each one's name, how many videos it holds, and a cover taken from
 * its first video. Creating, renaming and deleting happen here; opening one pushes its page.
 */
@Composable
internal fun PlaylistsScreen(
    playlists: UserPlaylists,
    onOpen: (playlistId: String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (playlistId: String, String) -> Unit,
    onDelete: (playlistId: String) -> Unit,
    onBack: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<UserPlaylist?>(null) }
    var deleting by remember { mutableStateOf<UserPlaylist?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.playlists_title),
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
            actions = {
                IconButton(onClick = { creating = true }) {
                    Icon(
                        imageVector = FlowIcons.Add,
                        contentDescription = stringResource(Res.string.playlists_create),
                    )
                }
            },
        )
        if (playlists.playlists.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.playlists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(PLAYLIST_ROW_SPACING.dp),
            ) {
                items(playlists.playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onOpen = { onOpen(playlist.id) },
                        onRename = { renaming = playlist },
                        onDelete = { deleting = playlist },
                    )
                }
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(Res.string.playlists_create),
            initialName = "",
            confirmLabel = stringResource(Res.string.playlist_create_action),
            placeholder = stringResource(Res.string.playlist_name_hint),
            cancelLabel = stringResource(Res.string.action_cancel),
            onConfirm = { name ->
                onCreate(name)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            initialName = playlist.name,
            confirmLabel = stringResource(Res.string.playlist_rename),
            placeholder = stringResource(Res.string.playlist_name_hint),
            cancelLabel = stringResource(Res.string.action_cancel),
            onConfirm = { name ->
                onRename(playlist.id, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.playlist_delete_title)) },
            text = { Text(stringResource(Res.string.playlist_delete_text, playlist.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(playlist.id)
                        deleting = null
                    },
                ) {
                    Text(stringResource(Res.string.playlist_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: UserPlaylist,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = {
            Text(
                text = playlist.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text =
                    pluralStringResource(
                        Res.plurals.playlist_video_count,
                        playlist.videos.size,
                        playlist.videos.size,
                    ),
            )
        },
        leadingContent = {
            MediaArtwork(
                title = playlist.name,
                imageUrl = playlist.videos.firstOrNull()?.thumbnailUrl,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(PlaylistRowArtwork),
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        imageVector = FlowIcons.MoreVert,
                        contentDescription = stringResource(Res.string.playlist_more_options),
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.playlist_rename)) },
                        onClick = {
                            menu = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.playlist_delete)) },
                        leadingIcon = { Icon(imageVector = FlowIcons.Delete, contentDescription = null) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    )
}
