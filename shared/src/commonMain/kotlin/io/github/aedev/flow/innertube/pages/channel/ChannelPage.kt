package io.github.aedev.flow.innertube.pages.channel

import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner

/** [tabs] is whatever the channel published, in its order — not a fixed set the screen assumes. */
data class ChannelPage(
    val header: ChannelHeader,
    val tabs: List<ChannelTabDescriptor>,
    val initialTab: ChannelTabContent?,
)

data class ChannelHeader(
    val id: String,
    val title: String,
    val handle: String? = null,
    val avatarUrl: String = "",
    val bannerUrl: String? = null,
    /** YouTube's own localised text; [subscriberCount] re-parses it only for rows that store a number. */
    val subscriberCountText: String? = null,
    val subscriberCount: Long? = null,
    val videoCountText: String? = null,
    val description: String? = null,
    val links: List<ChannelLink> = emptyList(),
    val isVerified: Boolean = false,
    val joinedDateText: String? = null,
    val viewCountText: String? = null,
    val countryText: String? = null,
    val canonicalUrl: String? = null,
)

data class ChannelLink(
    val title: String,
    val displayText: String,
    val url: String,
    val iconUrl: String? = null,
)

/** [params] comes from the response, so a token rotation cannot strand a tab. */
data class ChannelTabDescriptor(
    val kind: ChannelTabKind,
    val title: String,
    val params: String?,
    val selected: Boolean = false,
)

enum class ChannelTabKind {
    Home,
    Videos,
    Shorts,
    Live,
    Shows,
    Podcasts,
    Playlists,
    Courses,
    Posts,
    Releases,
    Store,
    Search,
    Unknown,
    ;

    /** Only for a deep link opening a tab before the landing browse returns its descriptor. */
    val defaultParams: String?
        get() =
            when (this) {
                Home -> "EghmZWF0dXJlZPIGBAoCMgA="
                Videos -> "EgZ2aWRlb3PyBgQKAjoA"
                Shorts -> "EgZzaG9ydHPyBgUKA5oBAA=="
                Live -> "EgdzdHJlYW1z8gYECgJ6AA=="
                Shows -> "EgVzaG93c_IGBAoCYgA="
                Podcasts -> "Eghwb2RjYXN0c_IGBQoDugEA"
                Playlists -> "EglwbGF5bGlzdHPyBgoKCEIGCgIQaCIA"
                Courses -> "Egdjb3Vyc2Vz8gYFCgPCAQA="
                Posts -> "EgVwb3N0c_IGBAoCSgA="
                Store -> "EgVzdG9yZfIGBAoCGgA="
                Search -> "EgZzZWFyY2jyBgQKAloA"
                Releases, Unknown -> null
            }
}

/**
 * The landing header with whatever the About panel adds. The landing response carries no
 * description, links or join date, so those arrive from a second request.
 */
internal fun ChannelHeader.mergedWith(about: ChannelAbout?): ChannelHeader =
    if (about == null) {
        this
    } else {
        copy(
            description = about.description ?: description,
            subscriberCountText = about.subscriberCountText ?: subscriberCountText,
            videoCountText = about.videoCountText ?: videoCountText,
            joinedDateText = about.joinedDateText,
            viewCountText = about.viewCountText,
            countryText = about.countryText,
            canonicalUrl = about.canonicalUrl ?: canonicalUrl,
            links = about.links,
        )
    }

internal fun ChannelHeader.toOwner() = FeedItemOwner(id = id, name = title, avatarUrl = avatarUrl)
