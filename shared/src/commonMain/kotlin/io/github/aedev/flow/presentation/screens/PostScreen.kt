package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.CommunityPostDetail
import io.github.aedev.flow.presentation.feed.FeedComment
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadCommunityPost
import io.github.aedev.flow.presentation.feed.loadCommunityPostComments
import io.github.aedev.flow.presentation.feed.loadCommunityPostReplies
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.comments_empty
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * One community post's own page: the post as its card draws it, with its gallery's pager and its
 * author's channel link, followed by its comment section, which YouTube serves on the post page's
 * own token. Comments page in place through [LoadMoreRow], and a comment's replies are fetched
 * once, on first expand, the way the player's comments do.
 */
@Composable
internal fun PostScreen(
    postId: String,
    params: String,
    fallbackAuthor: String,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onBack: () -> Unit,
) {
    var state by remember(postId) { mutableStateOf<LoadState<CommunityPostDetail>>(LoadState.Loading) }
    var attempt by remember(postId) { mutableIntStateOf(0) }
    var loadingMore by remember(postId) { mutableStateOf(false) }
    var repliesByComment by remember(postId) { mutableStateOf<Map<String, List<FeedComment>>>(emptyMap()) }
    var expandedReplies by remember(postId) { mutableStateOf<Set<String>>(emptySet()) }
    var loadingReplies by remember(postId) { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId, attempt) {
        state = LoadState.Loading
        state =
            loadCommunityPost(params).fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    fun loadMore() {
        val current = state as? LoadState.Ready ?: return
        val token = current.value.commentsContinuation ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            loadCommunityPostComments(token).fold(
                onSuccess = { next ->
                    state = LoadState.Ready(current.value.more(next))
                },
                onFailure = { Unit },
            )
            loadingMore = false
        }
    }

    val onToggleReplies: (FeedComment) -> Unit = { comment ->
        if (comment.id in expandedReplies) {
            expandedReplies = expandedReplies - comment.id
        } else {
            expandedReplies = expandedReplies + comment.id
            val token = comment.replyToken
            if (token != null && comment.id !in repliesByComment && comment.id !in loadingReplies) {
                loadingReplies = loadingReplies + comment.id
                scope.launch {
                    loadCommunityPostReplies(token).onSuccess { fetched ->
                        repliesByComment = repliesByComment + (comment.id to fetched)
                    }
                    loadingReplies = loadingReplies - comment.id
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = fallbackAuthor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

        when (val current = state) {
            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.feed_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { attempt++ }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            is LoadState.Ready -> {
                val detail = current.value
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item("post") {
                        val authorId = detail.post.authorChannelId
                        ChannelPostCard(
                            post = detail.post,
                            onOpenAuthor =
                                authorId?.let { id ->
                                    { onOpenChannel(id, detail.post.authorName) }
                                },
                        )
                    }
                    if (detail.commentsFailed) {
                        item("comments_failed") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(Res.string.feed_failed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = { attempt++ }) {
                                    Text(stringResource(Res.string.action_retry))
                                }
                            }
                        }
                    } else if (detail.comments.isEmpty()) {
                        item("comments_empty") {
                            Text(
                                text = stringResource(Res.string.comments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                            )
                        }
                    } else {
                        commentsSection(
                            comments = detail.comments,
                            countText = detail.commentCountText,
                            replies = repliesByComment,
                            expanded = expandedReplies,
                            loading = loadingReplies,
                            onToggleReplies = onToggleReplies,
                        )
                    }
                    if (detail.commentsContinuation != null) {
                        item("comments_more") {
                            LoadMoreRow(loading = loadingMore, onLoadMore = ::loadMore)
                        }
                    }
                }
            }
        }
    }
}
