package io.github.aedev.flow.presentation.feed

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aedev.flow.innertube.pages.channel.ChannelFilterGroup
import io.github.aedev.flow.innertube.pages.channel.ChannelFilterOption
import io.github.aedev.flow.innertube.pages.channel.ChannelHeader
import io.github.aedev.flow.innertube.pages.channel.ChannelTabDescriptor
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind

/** A tab the channel page opens: a grid the port can request, or the About panel. */
data class ChannelTabSpec(
    val kind: ChannelTabKind,
    val title: String,
    val params: String?,
    val isAbout: Boolean = false,
)

/**
 * The tabs the port draws: Home's featured shelves, the grid tabs, the Posts feed, and — only where
 * the channel publishes one — the Releases wall, which is one shelf per release. Search and Store
 * are left out: the reference's magnifier covers Search while the app has no shop.
 */
private val GridTabs =
    setOf(
        ChannelTabKind.Home,
        ChannelTabKind.Videos,
        ChannelTabKind.Shorts,
        ChannelTabKind.Live,
        ChannelTabKind.Playlists,
        ChannelTabKind.Posts,
        ChannelTabKind.Releases,
    )

/**
 * The tab row for a channel: its grid tabs in the response's order, then About when the header has
 * anything to put in it. Nothing is invented for a channel that published no such tab.
 */
fun channelTabs(
    descriptors: List<ChannelTabDescriptor>,
    header: ChannelHeader?,
    aboutTitle: String,
): List<ChannelTabSpec> {
    val tabs =
        descriptors.mapNotNull { descriptor ->
            if (descriptor.kind !in GridTabs) return@mapNotNull null
            val params = descriptor.params ?: descriptor.kind.defaultParams ?: return@mapNotNull null
            if (descriptor.title.isBlank()) return@mapNotNull null
            ChannelTabSpec(kind = descriptor.kind, title = descriptor.title, params = params)
        }
    return if (header.hasAboutContent()) {
        tabs + ChannelTabSpec(kind = ChannelTabKind.Unknown, title = aboutTitle, params = null, isAbout = true)
    } else {
        tabs
    }
}

/** About is appended only when at least one of its rows would have a value. */
internal fun ChannelHeader?.hasAboutContent(): Boolean =
    this != null &&
        (
            !description.isNullOrBlank() ||
                links.isNotEmpty() ||
                !handle.isNullOrBlank() ||
                !subscriberCountText.isNullOrBlank() ||
                !videoCountText.isNullOrBlank() ||
                !viewCountText.isNullOrBlank() ||
                !joinedDateText.isNullOrBlank() ||
                !countryText.isNullOrBlank()
        )

/**
 * A tab's filter choice as the request it makes. Chips and menu entries carry a continuation, while
 * some menus carry browse params instead; a chosen option with neither cannot be requested.
 */
sealed interface ChannelFilterRequest {
    data class Continuation(
        val token: String,
    ) : ChannelFilterRequest

    data class Params(
        val token: String,
    ) : ChannelFilterRequest
}

/** The request the viewer's picks make: the first chosen option in the bar that names one. */
fun channelTabFilterRequest(
    filters: List<ChannelFilterGroup>,
    picks: List<Int>,
): ChannelFilterRequest? {
    filters.forEachIndexed { groupIndex, group ->
        val option = group.options.getOrNull(picks.getOrNull(groupIndex) ?: -1) ?: return@forEachIndexed
        option.request()?.let { return it }
    }
    return null
}

/** A group's showing option: the viewer's pick when it is in range, else YouTube's own selection. */
fun ChannelFilterGroup.chosenIndex(pick: Int?): Int = if (pick != null && pick in options.indices) pick else selectedIndex.coerceAtLeast(0)

private fun ChannelFilterOption.request(): ChannelFilterRequest? =
    when {
        !continuation.isNullOrBlank() -> ChannelFilterRequest.Continuation(continuation)
        !params.isNullOrBlank() -> ChannelFilterRequest.Params(params)
        else -> null
    }

/** Identifies what a tab is asking for, so a recomposition cannot re-run the same request. */
internal fun channelTabRequestKey(
    kind: ChannelTabKind,
    picks: List<Int>,
): String = "${kind.name}:${picks.joinToString(separator = ",")}"

/**
 * The channel page's tab state, owned by the screen: which tab is open, each tab's loaded page and
 * filter picks, and the request that produced each page. Pages are kept per tab, so going back to a
 * tab restores its grid and its sort rather than refetching it.
 */
@Stable
class ChannelTabState {
    var selected: ChannelTabKind? by mutableStateOf(null)
        private set

    /** True when the open tab draws one card per row; the choice is kept across tabs and rotation. */
    var isList: Boolean by mutableStateOf(false)
        private set

    fun toggleList() {
        isList = !isList
    }

    private val pages = mutableStateMapOf<ChannelTabKind, LoadState<ChannelTabPage>>()
    private val picks = mutableStateMapOf<ChannelTabKind, List<Int>>()
    private val requests = mutableStateMapOf<ChannelTabKind, String>()
    private val paging = mutableStateMapOf<ChannelTabKind, Boolean>()

    fun page(kind: ChannelTabKind?): LoadState<ChannelTabPage>? = kind?.let(pages::get)

    fun picks(kind: ChannelTabKind?): List<Int> = kind?.let { picks[it] }.orEmpty()

    fun select(kind: ChannelTabKind) {
        selected = kind
    }

    fun pick(
        kind: ChannelTabKind,
        group: Int,
        option: Int,
    ) {
        val next = picks[kind].orEmpty().toMutableList()
        while (next.size <= group) next += -1
        next[group] = option
        picks[kind] = next
    }

    fun putPage(
        kind: ChannelTabKind,
        page: LoadState<ChannelTabPage>,
    ) {
        pages[kind] = page
    }

    fun loadingMore(kind: ChannelTabKind?): Boolean = kind?.let { paging[it] } == true

    /** The token to page with, or null when a page is already on its way or there is no next page. */
    fun beginMore(kind: ChannelTabKind): String? {
        if (loadingMore(kind)) return null
        val token = (pages[kind] as? LoadState.Ready)?.value?.continuation ?: return null
        paging[kind] = true
        return token
    }

    /** Appends the next page under the tab's grid; the tab keeps its filters, as the server does. */
    fun appendPage(
        kind: ChannelTabKind,
        next: ChannelTabPage,
    ) {
        val ready = pages[kind] as? LoadState.Ready ?: return
        pages[kind] = LoadState.Ready(ready.value.more(next))
        paging[kind] = false
    }

    fun failMore(kind: ChannelTabKind) {
        paging[kind] = false
    }

    /** Drops the remembered request, so a retry asks again with the same key. */
    fun forgetRequest(kind: ChannelTabKind) {
        requests.remove(kind)
    }

    /** True only when [key] differs from the request already made for [kind]. */
    fun beginRequest(
        kind: ChannelTabKind,
        key: String,
    ): Boolean {
        if (requests[kind] == key) return false
        requests[kind] = key
        return true
    }

    /** Opens [kind] on an already-loaded page, such as the Videos tab the landing fetch returned. */
    fun seed(
        kind: ChannelTabKind,
        page: LoadState<ChannelTabPage>,
        key: String,
    ) {
        putPage(kind, page)
        beginRequest(kind, key)
        select(kind)
    }

    fun reset() {
        selected = null
        pages.clear()
        picks.clear()
        requests.clear()
        paging.clear()
    }
}
