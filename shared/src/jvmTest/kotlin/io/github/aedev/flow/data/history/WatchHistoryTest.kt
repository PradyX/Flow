package io.github.aedev.flow.data.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchHistoryTest {
    private fun entry(
        id: String,
        at: Long = 0,
        position: Long = 0,
    ) = WatchHistoryEntry(videoId = id, title = "Title $id", positionMs = position, watchedAtMillis = at)

    @Test
    fun theNewestVideoIsTheFirstRow() {
        val history = WatchHistory().with(entry("a", at = 10)).with(entry("b", at = 20))
        assertEquals(listOf("b", "a"), history.entries.map { it.videoId })
    }

    @Test
    fun rewatchingAVideoMovesItUpInsteadOfAddingARow() {
        val history = WatchHistory().with(entry("a", at = 10)).with(entry("b", at = 20)).with(entry("a", at = 30))
        assertEquals(listOf("a", "b"), history.entries.map { it.videoId })
        assertEquals(2, history.entries.size)
    }

    @Test
    fun aVideoWithoutAnIdIsNotRecorded() {
        assertTrue(WatchHistory().with(entry("")).entries.isEmpty())
    }

    @Test
    fun theRecordIsCappedAtTheOldestEnd() {
        val full = (1..WatchHistory.MAX_ENTRIES + 5).fold(WatchHistory()) { acc, i -> acc.with(entry("v$i", at = i.toLong())) }
        assertEquals(WatchHistory.MAX_ENTRIES, full.entries.size)
        assertEquals("v${WatchHistory.MAX_ENTRIES + 5}", full.entries.first().videoId)
        assertNull(full.entry("v5"))
    }

    @Test
    fun removingAndClearingKeepTheRestOfTheRecord() {
        val history = WatchHistory().with(entry("a", at = 10)).with(entry("b", at = 20))
        assertEquals(listOf("b"), history.without("a").entries.map { it.videoId })
        assertTrue(history.cleared().entries.isEmpty())
    }

    @Test
    fun aPositionIsNeverNegativeAfterARoundTrip() {
        val decoded = WatchHistory.decode(WatchHistory().with(entry("a", at = 1, position = -50)).encode())
        assertEquals(0, decoded.entry("a")?.positionMs)
    }

    @Test
    fun unreadableTextReadsAsAnEmptyRecord() {
        assertTrue(WatchHistory.decode(null).entries.isEmpty())
        assertTrue(WatchHistory.decode("not json").entries.isEmpty())
        assertTrue(WatchHistory.decode("{}").entries.isEmpty())
    }
}
