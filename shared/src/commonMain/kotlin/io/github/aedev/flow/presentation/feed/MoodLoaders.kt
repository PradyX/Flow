package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.pages.MoodAndGenres

/** One mood button: the collection it opens, and the title the button carries. */
data class MoodGenre(
    val id: String,
    val title: String,
    val browseId: String,
    val params: String? = null,
)

/** One category of mood buttons, titled the way YouTube Music titles it. */
data class MoodGroup(
    val id: String,
    val title: String,
    val genres: List<MoodGenre>,
)

/**
 * The response's categories as groups, in the order YouTube Music sends them. A category with no
 * title or no openable button is dropped rather than drawn as a heading over nothing, and a button
 * repeated inside its category opens its collection once.
 */
internal fun List<MoodAndGenres>.toMoodGroups(): List<MoodGroup> =
    mapIndexedNotNull { index, category ->
        val title = category.title.trim()
        if (title.isEmpty()) return@mapIndexedNotNull null
        val genres = category.items.mapNotNull { it.toMoodGenre() }.distinctBy { it.id }
        if (genres.isEmpty()) return@mapIndexedNotNull null
        MoodGroup(
            id = "group-$index-${title.lowercase().replace(' ', '-')}",
            title = title,
            genres = genres,
        )
    }

private fun MoodAndGenres.Item.toMoodGenre(): MoodGenre? {
    val name = title.trim()
    val browseId = endpoint.browseId
    if (name.isEmpty() || browseId.isBlank()) return null
    val params = endpoint.params?.takeIf { it.isNotBlank() }
    return MoodGenre(
        // Every mood button on the index browses the same category id and differs only by its
        // token, so the token is what tells two collections apart.
        id = params?.let { "$browseId|$it" } ?: browseId,
        title = name,
        browseId = browseId,
        params = params,
    )
}

/** One mood's page: the shelves YouTube Music publishes for it, and the token for the next page. */
data class MoodPage(
    val sections: List<MusicSection> = emptyList(),
    val continuation: String? = null,
) {
    /** The next page's shelves under the ones already shown, with its own token to continue from. */
    fun more(next: MoodPage): MoodPage = copy(sections = sections + next.sections, continuation = next.continuation)

    /** Nothing drew, so the page is the empty case rather than a screen of headings. */
    val isEmpty: Boolean get() = sections.all { it.entries.isEmpty() }
}

/** YouTube Music's moods and genres index. */
suspend fun loadMoodGroups(): Result<List<MoodGroup>> = YouTube.moodsAndGenres().mapCatching { categories -> categories.toMoodGroups() }

/**
 * A mood's own page, browsed through the endpoint its button carried. The response is the same
 * shape YouTube Music's home sends, so its carousels become shelves the music cards already draw.
 */
suspend fun loadMoodPage(
    browseId: String,
    params: String? = null,
): Result<MoodPage> =
    YouTube.home(browseId = browseId, params = params).mapCatching { page ->
        MoodPage(sections = page.toBrowseMusicSections(), continuation = page.continuation)
    }

/** The next page of a mood's shelves, from the token the page was served with. */
suspend fun loadMoodPageMore(continuation: String): Result<MoodPage> =
    YouTube.home(continuation = continuation).mapCatching { page ->
        MoodPage(sections = page.toBrowseMusicSections(), continuation = page.continuation)
    }
