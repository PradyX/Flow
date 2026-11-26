package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.models.BrowseEndpoint
import io.github.aedev.flow.innertube.pages.MoodAndGenres
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoodLoadersTest {
    private fun button(
        title: String,
        browseId: String,
        params: String? = null,
    ) = MoodAndGenres.Item(
        title = title,
        stripeColor = 0L,
        endpoint = BrowseEndpoint(browseId = browseId, params = params),
    )

    private fun category(
        title: String,
        vararg items: MoodAndGenres.Item,
    ) = MoodAndGenres(title = title, items = items.toList())

    @Test
    fun `categories become groups in the order YouTube Music sends them`() {
        val groups =
            listOf(
                category("Moods", button("Chill", "FEmusic_moods_and_genres_category_chill")),
                category("Genres", button("Pop", "FEmusic_moods_and_genres_category_pop")),
            ).toMoodGroups()

        assertEquals(listOf("Moods", "Genres"), groups.map { it.title })
        assertEquals(listOf("Chill"), groups.first().genres.map { it.title })
    }

    @Test
    fun `a category without a title is not a group`() {
        val groups =
            listOf(
                category("  ", button("Chill", "browse-chill")),
                category("Genres", button("Pop", "browse-pop")),
            ).toMoodGroups()

        assertEquals(listOf("Genres"), groups.map { it.title })
    }

    @Test
    fun `a category whose buttons all lack an endpoint is dropped, not drawn as a heading`() {
        val groups =
            listOf(
                category("Moods", button("Chill", " ")),
                category("Genres", button("Pop", "browse-pop")),
            ).toMoodGroups()

        assertEquals(1, groups.size)
        assertEquals("Genres", groups.single().title)
    }

    @Test
    fun `a button keeps the token its collection needs`() {
        val genre =
            listOf(category("Moods", button("Chill", "browse-chill", params = "CgsKB2NoaWxs")))
                .toMoodGroups()
                .single()
                .genres
                .single()

        assertEquals("browse-chill", genre.browseId)
        assertEquals("CgsKB2NoaWxs", genre.params)
        assertEquals("Chill", genre.title)
    }

    @Test
    fun `a button repeated inside its category opens its collection once`() {
        val genres =
            listOf(
                category(
                    "Moods",
                    button("Chill", "browse-chill"),
                    button("Chill", "browse-chill"),
                    button("Focus", "browse-focus"),
                ),
            ).toMoodGroups()
                .single()
                .genres

        assertEquals(listOf("browse-chill", "browse-focus"), genres.map { it.browseId })
    }

    @Test
    fun `two buttons on the same category id are two collections, told apart by their token`() {
        val genres =
            listOf(
                category(
                    "Moods & moments",
                    button("Chill", "FEmusic_moods_and_genres_category", params = "token-chill"),
                    button("Focus", "FEmusic_moods_and_genres_category", params = "token-focus"),
                ),
            ).toMoodGroups()
                .single()
                .genres

        assertEquals(2, genres.size)
        assertEquals(listOf("Chill", "Focus"), genres.map { it.title })
    }

    @Test
    fun `the next page lands under the shelves already shown`() {
        val first =
            MoodPage(
                sections = listOf(MusicSection(title = "Trending", entries = emptyList())),
                continuation = "token-1",
            )
        val next =
            MoodPage(
                sections = listOf(MusicSection(title = "New releases", entries = emptyList())),
                continuation = null,
            )

        val merged = first.more(next)

        assertEquals(listOf("Trending", "New releases"), merged.sections.map { it.title })
        assertNull(merged.continuation)
    }

    @Test
    fun `a page with nothing playable is the empty case`() {
        assertTrue(MoodPage().isEmpty)
        assertTrue(MoodPage(sections = listOf(MusicSection(title = "Trending", entries = emptyList()))).isEmpty)
        assertFalse(
            MoodPage(
                sections =
                    listOf(
                        MusicSection(
                            title = "Trending",
                            entries =
                                listOf(
                                    FeedEntry(
                                        id = "abc",
                                        title = "Song",
                                        subtitle = null,
                                        videoId = "abc",
                                    ),
                                ),
                        ),
                    ),
            ).isEmpty,
        )
    }
}
