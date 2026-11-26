package io.github.aedev.flow.presentation.screens

import io.github.aedev.flow.data.subscriptions.Subscription
import kotlin.test.Test
import kotlin.test.assertEquals

/** Manage mode's list: a blank query lists everything, otherwise names that contain the query. */
class ManageSubscriptionsTest {
    private val queen = Subscription("UCQueen", "Queen Official")
    private val bowie = Subscription("UCBowie", "David Bowie")
    private val queenLive = Subscription("UCQueenLive", "Queen Live Archive")
    private val channels = listOf(queen, bowie, queenLive)

    @Test
    fun anUntouchedQueryListsEverySubscriptionInOrder() {
        assertEquals(channels, manageChannels(channels, ""))
        assertEquals(channels, manageChannels(channels, "   "))
    }

    @Test
    fun theQueryMatchesNamesWithoutCase() {
        assertEquals(listOf(queen, queenLive), manageChannels(channels, "queen"))
        assertEquals(listOf(queen, queenLive), manageChannels(channels, "QUEEN"))
    }

    @Test
    fun aPartialNameMatches() {
        assertEquals(listOf(bowie), manageChannels(channels, "bowie"))
    }

    @Test
    fun aQueryWithNoMatchListsNothing() {
        assertEquals(emptyList(), manageChannels(channels, "zzz"))
        assertEquals(emptyList(), manageChannels(emptyList(), "queen"))
    }

    @Test
    fun surroundingSpaceIsIgnored() {
        assertEquals(listOf(bowie), manageChannels(channels, "  bowie  "))
    }
}
