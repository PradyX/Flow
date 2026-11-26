package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedComment
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.cd_close
import io.github.aedev.flow.resources.comments_title
import org.jetbrains.compose.resources.stringResource

/** How much of the reel a sheet may take; the reel keeps the rest, as the reference allows. */
internal const val COMMENTS_SHEET_HEIGHT_FRACTION = 0.62f

/** Material's drag handle and the room its row takes at the head of a sheet. */
internal val CommentsSheetHandleHeight = 52.dp

/**
 * The head of the reference's Shorts page that its sheet fraction measures from: over its whole
 * window the reference's sheet comes out at 0.62 of the reel area less this band, measured at
 * window heights 1340, 1100 and 1600 px and again at 2x density (719, 570, 880 and 673 px), all
 * inside 1px.
 */
internal val CommentsSheetTopChrome = 39.dp

private val SheetStartPadding = 16.dp
private val SheetTitleGap = 8.dp
private val SheetHeaderBottomPadding = 6.dp
private val CloseButtonSize = 40.dp

/** The sheet's drag handle row, sized so the sheet's height can account for it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsSheetDragHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CommentsSheetHandleHeight)
                .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/**
 * The reel's comment sheet body at the reference's frame: the bold "Comments" row with the count
 * and a close mark, then the shared comment rows. It sits inside the reel's own layout, above the
 * bar, the way the reference's sheet leaves the reel and its bar visible.
 */
@Composable
internal fun ShortsCommentsSheet(
    comments: List<FeedComment>?,
    countText: String?,
    loading: Boolean,
    replies: Map<String, List<FeedComment>>,
    expanded: Set<String>,
    loadingReplies: Set<String>,
    onToggleReplies: (FeedComment) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CommentsSheetDragHandle()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetStartPadding)
                    .padding(bottom = SheetHeaderBottomPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.comments_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!countText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(SheetTitleGap))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(CloseButtonSize),
            ) {
                Icon(
                    imageVector = FlowIcons.Close,
                    contentDescription = stringResource(Res.string.cd_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (comments != null) {
                commentsSection(
                    comments = comments,
                    countText = countText,
                    replies = replies,
                    expanded = expanded,
                    loading = loadingReplies,
                    onToggleReplies = onToggleReplies,
                    showHeader = false,
                )
            } else if (loading) {
                item("comments-loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
