package io.github.aedev.flow.presentation.screens

import io.github.aedev.flow.presentation.feed.FeedEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/** The album page's sort chip actually reorders rows, and never loses or duplicates one. */
class PlaylistSortingTest {
    private val rows =
        listOf(
            entry("a", "Rhyno", 180),
            entry("b", "AHH YEA", 121),
            entry("c", "Go Easy", 192),
            entry("d", "Unknown length", null),
        )

    @Test
    fun collectionOrderIsTheResponseOrder() {
        assertEquals(listOf("a", "b", "c", "d"), rows.sortedForPlaylist(PlaylistSortOrder.COLLECTION).map { it.id })
    }

    @Test
    fun titleSortIgnoresCase() {
        assertEquals(listOf("b", "c", "a", "d"), rows.sortedForPlaylist(PlaylistSortOrder.TITLE).map { it.id })
    }

    @Test
    fun longestFirstSortsByLength() {
        assertEquals(listOf("c", "a", "b", "d"), rows.sortedForPlaylist(PlaylistSortOrder.LONGEST).map { it.id })
    }

    @Test
    fun shortestFirstPutsUnknownLengthsLast() {
        assertEquals(listOf("b", "a", "c", "d"), rows.sortedForPlaylist(PlaylistSortOrder.SHORTEST).map { it.id })
    }

    private fun entry(
        id: String,
        title: String,
        seconds: Int?,
    ) = FeedEntry(
        id = id,
        title = title,
        subtitle = "Grand Theft Auto VI",
        videoId = id,
        durationSeconds = seconds,
    )
}
