package io.github.aedev.flow.data.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHistoryTest {
    @Test
    fun `a new term goes to the front`() {
        val history = SearchHistory().with("cats").with("dogs")
        assertEquals(listOf("dogs", "cats"), history.queries)
    }

    @Test
    fun `the same term twice moves it to the front instead of repeating it`() {
        val history = SearchHistory().with("cats").with("dogs").with("Cats")
        assertEquals(listOf("Cats", "dogs"), history.queries)
    }

    @Test
    fun `a blank term is not a search`() {
        val history = SearchHistory(listOf("cats"))
        assertEquals(history, history.with("   "))
        assertEquals(history, history.with(""))
    }

    @Test
    fun `the list is capped so the panel never grows without bound`() {
        val history = (1..30).fold(SearchHistory()) { acc, index -> acc.with("term $index") }
        assertEquals(SearchHistory.MAX_QUERIES, history.queries.size)
        assertEquals("term 30", history.queries.first())
    }

    @Test
    fun `a term is removed by the row that draws it, whatever its casing`() {
        val history = SearchHistory(listOf("cats", "dogs")).without("CATS")
        assertEquals(listOf("dogs"), history.queries)
    }

    @Test
    fun `clearing leaves nothing`() {
        assertTrue(SearchHistory(listOf("cats", "dogs")).cleared().isEmpty)
    }

    @Test
    fun `a stored history is tidied on the way in`() {
        val decoded = SearchHistory.decode("""{"queries":["  cats ","cats","","dogs"]}""")
        assertEquals(listOf("cats", "dogs"), decoded.queries)
    }

    @Test
    fun `an absent or unreadable store is an empty history`() {
        assertTrue(SearchHistory.decode(null).isEmpty)
        assertTrue(SearchHistory.decode("").isEmpty)
        assertTrue(SearchHistory.decode("not json").isEmpty)
    }

    @Test
    fun `a history round-trips through its stored text`() {
        val history = SearchHistory(listOf("cats", "dogs"))
        assertEquals(history, SearchHistory.decode(history.encode()))
    }
}
