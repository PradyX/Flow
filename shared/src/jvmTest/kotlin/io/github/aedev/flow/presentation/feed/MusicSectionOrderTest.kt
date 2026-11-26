package io.github.aedev.flow.presentation.feed

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The music page opens with the app's named shelves — New releases, Trending community playlists,
 * then Artists for you — whatever position the response put them in, and keeps every other shelf in
 * the order the response sent it.
 */
class MusicSectionOrderTest {
    private fun shelf(title: String) =
        MusicSection(
            title = title,
            entries = emptyList(),
        )

    @Test
    fun theLeadShelvesMoveToTheFrontInTheirFixedOrder() {
        val ordered =
            musicSectionsInAppOrder(
                listOf(
                    shelf("Quick picks"),
                    shelf("Trending community playlists"),
                    shelf("India's biggest hits"),
                    shelf("New releases"),
                    shelf("Artists for you"),
                ),
            )

        assertEquals(
            listOf(
                "New releases",
                "Trending community playlists",
                "Artists for you",
                "Quick picks",
                "India's biggest hits",
            ),
            ordered.map { it.title },
        )
    }

    @Test
    fun aPageWithoutTheLeadShelvesKeepsItsOwnOrder() {
        val ordered = musicSectionsInAppOrder(listOf(shelf("Romance Right Now"), shelf("Quick picks")))

        assertEquals(listOf("Romance Right Now", "Quick picks"), ordered.map { it.title })
    }

    @Test
    fun aLeadShelfIsNotDuplicated() {
        val ordered = musicSectionsInAppOrder(listOf(shelf("New releases"), shelf("New releases")))

        assertEquals(listOf("New releases"), ordered.map { it.title })
    }
}
