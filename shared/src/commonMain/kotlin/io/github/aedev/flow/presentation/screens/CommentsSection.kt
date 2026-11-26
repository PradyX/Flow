package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedComment
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_like
import io.github.aedev.flow.resources.comment_creator
import io.github.aedev.flow.resources.comment_hide_replies
import io.github.aedev.flow.resources.comment_pinned
import io.github.aedev.flow.resources.comment_replies
import io.github.aedev.flow.resources.comment_reply_one
import io.github.aedev.flow.resources.comments_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/** The app's comment item clamps its body at four lines until it is expanded. */
private const val COMMENT_MAX_LINES = 4

private val CommentIconSize = 14.dp
private val ReplyAvatarDiameter = 28
private val ReplyIndent = 52.dp
private val ReplyIconSize = 12.dp
private const val REPLY_MAX_LINES = 3

/**
 * The watch page's comments under the related rail: a heading carrying YouTube's own total, then
 * one row per comment laid out the way the app's comment item is — avatar, author and age, the
 * text, then the like count and the reply count.
 *
 * A comment YouTube offers replies for carries a toggle; expanding it shows the replies already
 * fetched for it, and leaves the top-level rows in YouTube's order.
 */
internal fun LazyListScope.commentsSection(
    comments: List<FeedComment>,
    countText: String?,
    replies: Map<String, List<FeedComment>>,
    expanded: Set<String>,
    loading: Set<String>,
    onToggleReplies: (FeedComment) -> Unit,
    showHeader: Boolean = true,
) {
    if (comments.isEmpty()) return
    if (showHeader) item("comments-header") { CommentsHeader(countText) }
    items(comments, key = { "comment-${it.id}" }) { comment ->
        val isExpanded = comment.id in expanded
        Column {
            CommentRow(
                comment = comment,
                expanded = isExpanded,
                loading = comment.id in loading,
                onToggleReplies = onToggleReplies,
            )
            if (isExpanded) {
                replies[comment.id].orEmpty().forEach { reply -> ReplyRow(reply) }
                if (comment.id in loading) LoadingReplies()
            }
        }
    }
}

@Composable
private fun CommentsHeader(countText: String?) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerInfoPadding)
                .padding(top = Dimensions.SectionSpacing, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.comments_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        countText?.takeIf { it.isNotBlank() }?.let { count ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: FeedComment,
    expanded: Boolean,
    loading: Boolean,
    onToggleReplies: (FeedComment) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerInfoPadding)
                .padding(vertical = 8.dp),
    ) {
        ChannelAvatar(
            name = comment.author,
            imageUrl = comment.authorThumbnailUrl,
            diameter = 40,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (comment.isPinned) {
                Text(
                    text = comment.pinnedBy ?: stringResource(Res.string.comment_pinned),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentAuthor(comment)
                comment.published?.let { published ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = published,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = COMMENT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                comment.likes?.let { likes ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = FlowIcons.ThumbUp,
                            contentDescription = stringResource(Res.string.action_like),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(CommentIconSize),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = likes,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (comment.replyCount > 0 && comment.replyToken != null) {
                    TextButton(
                        onClick = { onToggleReplies(comment) },
                        enabled = !loading,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text =
                                if (expanded) {
                                    stringResource(Res.string.comment_hide_replies)
                                } else {
                                    stringResource(
                                        if (comment.replyCount == 1) {
                                            Res.string.comment_reply_one
                                        } else {
                                            Res.string.comment_replies
                                        },
                                        comment.replyCount,
                                    )
                                },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** One reply under an expanded comment, indented to the comment's text and one size smaller. */
@Composable
private fun ReplyRow(reply: FeedComment) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = ReplyIndent, end = PlayerInfoPadding))
                .padding(vertical = 4.dp),
    ) {
        ChannelAvatar(
            name = reply.author,
            imageUrl = reply.authorThumbnailUrl,
            diameter = ReplyAvatarDiameter,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentAuthor(reply)
                reply.published?.let { published ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = published,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reply.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = REPLY_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            reply.likes?.let { likes ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FlowIcons.ThumbUp,
                        contentDescription = stringResource(Res.string.action_like),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(ReplyIconSize),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = likes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingReplies() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = ReplyIndent, top = 8.dp, bottom = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
    }
}

/**
 * The author name. The channel that owns the video wears the app's creator pill, so an uploader
 * comment is recognisable at a glance; the pill states that as its accessibility label.
 */
@Composable
private fun RowScope.CommentAuthor(comment: FeedComment) {
    if (comment.isUploader) {
        val creatorLabel = stringResource(Res.string.comment_creator)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = Modifier.semantics { contentDescription = creatorLabel },
        ) {
            Text(
                text = comment.author,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        return
    }
    Text(
        text = comment.author,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
    )
}
