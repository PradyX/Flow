package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.innertube.pages.renderer.PostAttachment
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.post_gallery_position
import org.jetbrains.compose.resources.stringResource

private const val POST_AVATAR_DIAMETER = 36
private val PostMargin = 12.dp

/** The post's image gallery, so a test can swipe the pages the capture actually carries. */
internal const val POST_GALLERY_TEST_TAG = "post_gallery"

/** The tap that opens a post's page, offered only for a post that carries the page's token. */
internal fun CommunityPost.openAction(onOpenPost: (CommunityPost) -> Unit): (() -> Unit)? =
    commentEndpointParams?.takeIf(String::isNotBlank)?.let { { onOpenPost(this) } }

/**
 * A community post as its page and the Posts tab draw it: the author line, the text, the attachment
 * YouTube shipped with it, and the like/comment counts. A gallery is the swipeable pager YouTube
 * serves every picture in, with its own position badge; [onOpen] opens the post's page when the post
 * carries the token that page needs, and [onOpenAuthor] opens the author's channel.
 */
@Composable
internal fun ChannelPostCard(
    post: CommunityPost,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    onOpenAuthor: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
                .padding(horizontal = PostMargin, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (onOpenAuthor != null) Modifier.clickable(onClick = onOpenAuthor) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChannelAvatar(
                name = post.authorName,
                imageUrl = post.authorAvatarUrl,
                diameter = POST_AVATAR_DIAMETER,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                post.publishedTimeText.takeIf { it.isNotBlank() }?.let { published ->
                    Text(
                        text = published,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (post.text.isNotBlank()) {
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when (val attachment = post.attachment) {
            is PostAttachment.Images -> {
                if (attachment.urls.isNotEmpty()) {
                    PostGallery(urls = attachment.urls)
                }
            }

            is PostAttachment.SharedVideo -> {
                AsyncImage(
                    model = attachment.video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                Text(
                    text = attachment.video.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            is PostAttachment.Poll -> {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    attachment.choices.forEach { choice ->
                        Text(
                            text = choice.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            null -> {
                Unit
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (post.likeCountText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = FlowIcons.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = post.likeCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (post.commentCountText.isNotBlank()) {
                Text(
                    text = post.commentCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Every picture the post's renderer served, one per page, the way YouTube shows a multi-image
 * post. A single-image post is the same pager with one page and no position badge.
 */
@Composable
private fun PostGallery(urls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { urls.size })
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().testTag(POST_GALLERY_TEST_TAG),
        ) { page ->
            SubcomposeAsyncImage(
                model = urls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                error = { GalleryPlaceholder() },
            )
        }
        if (urls.size > 1) {
            Text(
                text = stringResource(Res.string.post_gallery_position, pagerState.currentPage + 1, urls.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** A picture that could not be fetched: the card's surface with the app's video glyph. */
@Composable
private fun GalleryPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = FlowIcons.MotionVideo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
