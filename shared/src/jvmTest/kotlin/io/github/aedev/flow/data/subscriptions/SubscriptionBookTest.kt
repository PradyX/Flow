package io.github.aedev.flow.data.subscriptions

import io.github.aedev.flow.data.taste.TasteChannel
import io.github.aedev.flow.data.taste.UserTaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The one subscription store: setup seeds it, the channel page writes it, the tab reads it. */
class SubscriptionBookTest {
    private val queen = Subscription("UCQueen", "Queen Official", "https://example.com/queen.jpg")
    private val bowie = Subscription("UCBowie", "David Bowie")

    @Test
    fun setupPicksSeedTheStore() {
        val taste =
            UserTaste(
                topics = listOf("Music"),
                channels =
                    listOf(
                        TasteChannel("UCQueen", "Queen Official", "https://example.com/queen.jpg"),
                        TasteChannel("UCBowie", "David Bowie"),
                    ),
            )

        val book = taste.seedSubscriptions()

        assertEquals(listOf("UCQueen", "UCBowie"), book.channels.map { it.id })
        assertEquals("Queen Official", book.channels.first().name)
        assertEquals("https://example.com/queen.jpg", book.channels.first().thumbnailUrl)
        assertTrue(book.isSubscribed("UCQueen"))
        assertFalse(book.isSubscribed("UCUnknown"))
    }

    @Test
    fun anEmptySetupSeedsAnEmptyStore() {
        val book = UserTaste().seedSubscriptions()

        assertTrue(book.channels.isEmpty())
        assertFalse(book.isSubscribed("UCQueen"))
        assertNull(book.channel("UCQueen"))
    }

    @Test
    fun subscribingAddsAndUnsubscribingRemoves() {
        val one = SubscriptionBook().with(queen)
        assertTrue(one.isSubscribed("UCQueen"))
        assertEquals(queen, one.channel("UCQueen"))

        val two = one.with(bowie)
        assertEquals(listOf("UCQueen", "UCBowie"), two.channels.map { it.id })

        val backToOne = two.without("UCQueen")
        assertEquals(listOf("UCBowie"), backToOne.channels.map { it.id })
        assertFalse(backToOne.isSubscribed("UCQueen"))

        val empty = backToOne.without("UCBowie")
        assertTrue(empty.channels.isEmpty())
    }

    @Test
    fun subscribingAgainKeepsOneEntryWithTheNewestDetails() {
        val renamed = queen.copy(name = "Queen Official (Remastered)")

        val book = SubscriptionBook().with(queen).with(bowie).with(renamed)

        // One entry, carrying the newest details, moved to the end as a fresh subscribe would.
        assertEquals(listOf("UCBowie", "UCQueen"), book.channels.map { it.id })
        assertEquals("Queen Official (Remastered)", book.channel("UCQueen")?.name)
    }

    @Test
    fun undoPutsTheChannelBackWhereItWas() {
        val carol = Subscription("UCCarol", "Carol")
        val book = SubscriptionBook(listOf(queen, bowie, carol))

        val removed = book.without("UCBowie")
        assertEquals(listOf("UCQueen", "UCCarol"), removed.channels.map { it.id })

        val undone = removed.restoring(bowie, 1)
        assertEquals(book, undone)
    }

    @Test
    fun anOutOfRangeUndoIndexStaysInsideTheList() {
        val book = SubscriptionBook(listOf(queen))

        assertEquals(listOf("UCQueen", "UCBowie"), book.restoring(bowie, 99).channels.map { it.id })
        assertEquals(listOf("UCBowie", "UCQueen"), book.restoring(bowie, -3).channels.map { it.id })
    }

    @Test
    fun restoringAChannelThatWasNeverRemovedKeepsOneEntry() {
        val book = SubscriptionBook(listOf(queen, bowie))

        val restored = book.restoring(bowie, 1)

        assertEquals(listOf("UCQueen", "UCBowie"), restored.channels.map { it.id })
    }

    @Test
    fun unsubscribingSomethingAbsentChangesNothing() {
        val book = SubscriptionBook(listOf(queen))

        assertEquals(book, book.without("UCUnknown"))
    }

    @Test
    fun theStoreRoundTripsThroughJson() {
        val book = SubscriptionBook(listOf(queen, bowie))

        val restored = SubscriptionBook.fromJson(SubscriptionBook.toJson(book))

        assertEquals(book, restored)
    }

    /** A newer build's extra fields must not wipe the subscription set on an older one. */
    @Test
    fun unknownJsonFieldsAreIgnored() {
        val restored = SubscriptionBook.fromJson("""{"channels":[{"id":"UCQueen","name":"Queen","future":true}],"v":2}""")

        assertEquals(listOf("UCQueen"), restored?.channels?.map { it.id })
    }

    @Test
    fun missingOrBrokenJsonReadsAsNoStore() {
        assertNull(SubscriptionBook.fromJson(null))
        assertNull(SubscriptionBook.fromJson("  "))
        assertNull(SubscriptionBook.fromJson("not json"))
    }
}
