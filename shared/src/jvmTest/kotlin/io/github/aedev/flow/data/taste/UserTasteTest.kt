package io.github.aedev.flow.data.taste

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTasteTest {
    private val taste =
        UserTaste(
            topics = listOf("Gaming", "Minecraft", "Music"),
            channels =
                listOf(
                    TasteChannel("UC-mrbeast", "MrBeast", "https://example.test/mrbeast.jpg"),
                    TasteChannel("", "Blank id"),
                ),
            artists = listOf("Arijit Singh"),
        )

    @Test
    fun `every picked topic weighs the same in the interest vector`() {
        assertEquals(
            mapOf("Gaming" to 1.0, "Minecraft" to 1.0, "Music" to 1.0),
            taste.toMixState().interests,
        )
    }

    @Test
    fun `picked channels become the mix state subscriptions`() {
        assertEquals(setOf("UC-mrbeast"), taste.toMixState().userSubs)
    }

    @Test
    fun `an empty taste still ranks`() {
        val state = UserTaste().toMixState()
        assertTrue(state.interests.isEmpty())
        assertTrue(state.userSubs.isEmpty())
        assertTrue(UserTaste().isEmpty)
    }

    @Test
    fun `chips search the picked topics, one discovery variant each`() {
        val queries = taste.chipQueries()
        assertEquals(listOf("Gaming", "Minecraft", "Music"), queries.interests)
        assertEquals(listOf("Gaming new", "Minecraft new", "Music new"), queries.exploration)
        assertEquals(listOf("Gaming", "Minecraft"), queries.live)
    }

    @Test
    fun `chips ask for at most three topics each`() {
        val many = UserTaste(topics = listOf("a", "b", "c", "d", "e"))
        assertEquals(listOf("a", "b", "c"), many.chipQueries().interests)
    }

    @Test
    fun `duplicate and blank picks do not repeat a query`() {
        val messy = UserTaste(topics = listOf("Gaming", "Gaming", "", "Music"))
        assertEquals(listOf("Gaming", "Music"), messy.chipQueries().interests)
    }

    @Test
    fun `the taste survives the round trip through storage`() {
        assertEquals(taste, UserTaste.fromJson(UserTaste.toJson(taste)))
    }

    @Test
    fun `missing or unreadable storage answers nothing, not a crash`() {
        assertNull(UserTaste.fromJson(null))
        assertNull(UserTaste.fromJson(""))
        assertNull(UserTaste.fromJson("{not json"))
    }
}
