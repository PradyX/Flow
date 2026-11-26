package io.github.aedev.flow.innertube.pages.renderer

import io.github.aedev.flow.data.model.Comment
import io.github.aedev.flow.innertube.pages.accessibilityLabel
import io.github.aedev.flow.innertube.pages.arrayOrNull
import io.github.aedev.flow.innertube.pages.bestThumbnailUrl
import io.github.aedev.flow.innertube.pages.commentMutations
import io.github.aedev.flow.innertube.pages.continuationToken
import io.github.aedev.flow.innertube.pages.countTextFromAccessibilityLabel
import io.github.aedev.flow.innertube.pages.findReplyContinuation
import io.github.aedev.flow.innertube.pages.normalizeImageUrl
import io.github.aedev.flow.innertube.pages.objectOrNull
import io.github.aedev.flow.innertube.pages.renderer.findRenderers
import io.github.aedev.flow.innertube.pages.stringOrNull
import io.github.aedev.flow.innertube.pages.toLegacyComment
import io.github.aedev.flow.innertube.pages.toModernComment
import io.github.aedev.flow.innertube.pages.youtubeText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class CommunityPost(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val text: String,
    val attachment: PostAttachment?,
    val likeCountText: String,
    val commentCountText: String,
    val commentEndpointParams: String?,
    val publishedTimeText: String,
    /** The author's channel, from the renderer's `authorEndpoint`; null when it carries none. */
    val authorChannelId: String? = null,
)

data class CommunityPostsPage(
    val posts: List<CommunityPost>,
    val continuation: String?,
)

data class CommunityCommentsPage(
    val comments: List<Comment>,
    val continuation: String?,
    val commentCountText: String? = null,
)

/**
 * One community post's own page: the post, and the token its comment section opens from. The page
 * itself carries no comments — WEB answers the post section and a `comment-item-section` holding
 * only a `continuationItemRenderer`, whose token is what the first comment request uses.
 */
data class CommunityPostPage(
    val post: CommunityPost?,
    val commentsContinuation: String?,
)

internal fun JsonElement.toCommunityPostPage(owner: FeedItemOwner): CommunityPostPage {
    val sections =
        objectOrNull()
            ?.get("contents")
            .objectOrNull()
            ?.get("twoColumnBrowseResultsRenderer")
            .objectOrNull()
            ?.get("tabs")
            .arrayOrNull()
            .orEmpty()
            .firstNotNullOfOrNull { tab ->
                tab
                    .objectOrNull()
                    ?.get("tabRenderer")
                    .objectOrNull()
                    ?.get("content")
                    .objectOrNull()
                    ?.get("sectionListRenderer")
                    .objectOrNull()
                    ?.get("contents")
                    .arrayOrNull()
            }.orEmpty()

    var post: CommunityPost? = null
    var commentsContinuation: String? = null
    sections.forEach { section ->
        val node = section.objectOrNull()?.get("itemSectionRenderer").objectOrNull() ?: return@forEach
        val isCommentSection = node["sectionIdentifier"].stringOrNull() == "comment-item-section"
        node["contents"].arrayOrNull().orEmpty().forEach { item ->
            if (post == null) {
                val thread = item.objectOrNull()?.get("backstagePostThreadRenderer").objectOrNull()
                val renderer =
                    thread
                        ?.get("post")
                        .objectOrNull()
                        ?.get("backstagePostRenderer")
                        .objectOrNull()
                        ?: item.objectOrNull()?.get("postRenderer").objectOrNull()
                renderer?.toCommunityPost("", "", owner)?.let { post = it }
            }
            if (isCommentSection && commentsContinuation == null) {
                commentsContinuation =
                    item
                        .objectOrNull()
                        ?.get("continuationItemRenderer")
                        .objectOrNull()
                        ?.continuationToken()
            }
        }
    }
    return CommunityPostPage(post = post, commentsContinuation = commentsContinuation)
}

internal fun JsonElement.toCommunityPostsPage(
    fallbackAuthorName: String,
    fallbackAuthorAvatarUrl: String,
    owner: FeedItemOwner = FeedItemOwner(name = fallbackAuthorName, avatarUrl = fallbackAuthorAvatarUrl),
): CommunityPostsPage {
    val posts = mutableListOf<CommunityPost>()
    var continuation: String? = null

    fun collect(element: JsonElement?) {
        when (element) {
            is JsonArray -> {
                element.forEach(::collect)
            }

            is JsonObject -> {
                val thread = element["backstagePostThreadRenderer"].objectOrNull()
                val renderer =
                    thread
                        ?.get("post")
                        .objectOrNull()
                        ?.get("backstagePostRenderer")
                        .objectOrNull()
                        ?: element["postRenderer"].objectOrNull()

                if (renderer != null) {
                    renderer
                        .toCommunityPost(fallbackAuthorName, fallbackAuthorAvatarUrl, owner)
                        ?.let(posts::add)
                    return
                }

                if (continuation == null) {
                    continuation =
                        element["continuationItemRenderer"]
                            .objectOrNull()
                            ?.continuationToken()
                }
                element.values.forEach(::collect)
            }

            else -> {
                Unit
            }
        }
    }

    collect(this)
    return CommunityPostsPage(
        posts = posts.distinctBy(CommunityPost::id),
        continuation = continuation,
    )
}

internal fun JsonElement.toCommunityCommentsPage(): CommunityCommentsPage {
    val root = objectOrNull()
    val mutations = commentMutations()

    val comments = mutableListOf<Comment>()
    var continuation: String? = null

    fun collect(element: JsonElement?) {
        when (element) {
            is JsonArray -> {
                element.forEach(::collect)
            }

            is JsonObject -> {
                val thread = element["commentThreadRenderer"].objectOrNull()
                if (thread != null) {
                    val rawViewModel = thread["commentViewModel"].objectOrNull()
                    val viewModel =
                        rawViewModel
                            ?.get("commentViewModel")
                            .objectOrNull()
                            ?: rawViewModel
                    val repliesRenderer =
                        thread["replies"]
                            .objectOrNull()
                            ?.get("commentRepliesRenderer")
                            .objectOrNull()
                    val comment =
                        viewModel?.toModernComment(mutations, repliesRenderer)
                            ?: thread["comment"]
                                .objectOrNull()
                                ?.get("commentRenderer")
                                .objectOrNull()
                                ?.toLegacyComment(repliesRenderer)
                    comment?.let(comments::add)
                    return
                }

                element["commentViewModel"]
                    .objectOrNull()
                    ?.toModernComment(mutations, null)
                    ?.let {
                        comments.add(it)
                        return
                    }

                element["commentRenderer"]
                    .objectOrNull()
                    ?.toLegacyComment(null)
                    ?.let {
                        comments.add(it)
                        return
                    }

                if (continuation == null) {
                    continuation =
                        element["continuationItemRenderer"]
                            .objectOrNull()
                            ?.continuationToken()
                }
                element.values.forEach(::collect)
            }

            else -> {
                Unit
            }
        }
    }

    collect(this)
    return CommunityCommentsPage(
        comments = comments.distinctBy(Comment::id),
        continuation = continuation,
        commentCountText = findCommentCountText(root),
    )
}

internal fun JsonObject.toCommunityPost(
    fallbackAuthorName: String,
    fallbackAuthorAvatarUrl: String,
    owner: FeedItemOwner,
): CommunityPost? {
    val id = this["postId"].stringOrNull()?.takeIf(String::isNotBlank) ?: return null
    val replyButton =
        this["actionButtons"]
            .objectOrNull()
            ?.get("commentActionButtonsRenderer")
            .objectOrNull()
            ?.get("replyButton")
            .objectOrNull()
            ?.get("buttonRenderer")
            .objectOrNull()
    val navigationEndpoint = replyButton?.get("navigationEndpoint").objectOrNull()
    // The page's copy of the post names the author's channel in `authorEndpoint`; the tab's copy
    // repeats it on the author text's own run, so either path is the same target.
    val authorChannelId =
        this["authorEndpoint"]
            .objectOrNull()
            ?.get("browseEndpoint")
            .objectOrNull()
            ?.get("browseId")
            .stringOrNull()
            ?.takeIf(String::isNotBlank)
            ?: this["authorText"]
                .objectOrNull()
                ?.get("runs")
                .arrayOrNull()
                .orEmpty()
                .firstNotNullOfOrNull { run ->
                    run
                        .objectOrNull()
                        ?.get("navigationEndpoint")
                        .objectOrNull()
                        ?.get("browseEndpoint")
                        .objectOrNull()
                        ?.get("browseId")
                        .stringOrNull()
                        ?.takeIf(String::isNotBlank)
                }
    val authorAvatar =
        this["authorThumbnail"].bestThumbnailUrl()
            ?: fallbackAuthorAvatarUrl
    return CommunityPost(
        id = id,
        authorName =
            this["authorText"]
                .youtubeText()
                ?.takeIf(String::isNotBlank)
                ?: fallbackAuthorName,
        authorAvatarUrl = normalizeImageUrl(authorAvatar),
        text = this["contentText"].youtubeText().orEmpty(),
        attachment = this["backstageAttachment"].toPostAttachment(owner),
        likeCountText = this["voteCount"].youtubeText().orEmpty(),
        commentCountText =
            replyButton?.get("text").youtubeText()
                ?: replyButton?.accessibilityLabel()?.countTextFromAccessibilityLabel()
                ?: "",
        commentEndpointParams =
            navigationEndpoint
                ?.get("browseEndpoint")
                .objectOrNull()
                ?.get("params")
                .stringOrNull()
                ?: navigationEndpoint
                    ?.get("signInEndpoint")
                    .objectOrNull()
                    ?.get("nextEndpoint")
                    .objectOrNull()
                    ?.get("browseEndpoint")
                    .objectOrNull()
                    ?.get("params")
                    .stringOrNull(),
        publishedTimeText = this["publishedTimeText"].youtubeText().orEmpty(),
        authorChannelId = authorChannelId,
    )
}

private fun findCommentCountText(root: JsonObject?): String? {
    val panels = root?.get("engagementPanels").arrayOrNull().orEmpty()
    panels.forEach { panel ->
        val section =
            panel
                .objectOrNull()
                ?.get("engagementPanelSectionListRenderer")
                .objectOrNull()
                ?: return@forEach
        val identifier = section["panelIdentifier"].stringOrNull()
        if (identifier == "comment-item-section" || identifier == "engagement-panel-comments-section") {
            section["header"]
                .objectOrNull()
                ?.get("engagementPanelTitleHeaderRenderer")
                .objectOrNull()
                ?.get("contextualInfo")
                .youtubeText()
                ?.let { return it }
        }
    }
    // A post's comment section answers with a `commentsHeaderRenderer` instead of a panel: its
    // "22 Comments" line is the only total the page carries.
    return root
        ?.findRenderers("commentsHeaderRenderer")
        ?.get("commentsHeaderRenderer")
        ?.get("countText")
        .youtubeText()
        ?.takeIf(String::isNotBlank)
}
