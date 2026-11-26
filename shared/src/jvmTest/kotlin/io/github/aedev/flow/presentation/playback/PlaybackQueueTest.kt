package io.github.aedev.flow.presentation.playback

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The queue steps one track at a time and stops at both ends; shuffle keeps every track once. */
class PlaybackQueueTest {
    private val tracks =
        listOf(
            QueueTrack("a", "Alpha"),
            QueueTrack("b", "Bravo"),
            QueueTrack("c", "Charlie"),
        )

    @Test
    fun startsOnTheFirstTrackInCollectionOrder() {
        val queue = assertNotNull(PlaybackQueue.of(tracks))
        assertEquals("a", queue.current?.videoId)
        assertTrue(queue.hasNext)
        assertEquals(false, queue.hasPrevious)
    }

    @Test
    fun nextWalksForwardAndStopsAtTheEnd() {
        val second = assertNotNull(assertNotNull(PlaybackQueue.of(tracks)).next())
        assertEquals("b", second.current?.videoId)
        val last = assertNotNull(second.next())
        assertEquals("c", last.current?.videoId)
        assertNull(last.next())
    }

    @Test
    fun previousWalksBackAndStopsAtTheStart() {
        val second = assertNotNull(assertNotNull(PlaybackQueue.of(tracks)).next())
        assertEquals("a", assertNotNull(second.previous()).current?.videoId)
        assertNull(assertNotNull(PlaybackQueue.of(tracks)).previous())
    }

    @Test
    fun shuffleKeepsEveryTrackOnce() {
        val queue = assertNotNull(PlaybackQueue.of(tracks, shuffled = true, random = Random(7)))
        assertEquals(tracks.toSet(), queue.tracks.toSet())
        assertEquals(3, queue.size)
        assertTrue(queue.shuffled)
    }

    @Test
    fun shuffleChangesTheStartingTrack() {
        val queue = assertNotNull(PlaybackQueue.of(tracks, shuffled = true, random = Random(1)))
        assertTrue(queue.current?.videoId != "a")
    }

    @Test
    fun anEmptyCollectionHasNoQueue() {
        assertNull(PlaybackQueue.of(emptyList()))
    }

    @Test
    fun playNextInsertsDirectlyAfterThePlayingTrack() {
        val queue = assertNotNull(PlaybackQueue.of(tracks)).withNext(listOf(QueueTrack("x", "Xray")))
        assertEquals(listOf("a", "x", "b", "c"), queue.tracks.map { it.videoId })
        assertEquals("a", queue.current?.videoId)
        assertEquals("x", queue.next()?.current?.videoId)
    }

    @Test
    fun steppingWalksInsertedTracksInOrder() {
        var queue: PlaybackQueue? =
            assertNotNull(PlaybackQueue.of(tracks))
                .withNext(listOf(QueueTrack("x", "Xray"), QueueTrack("y", "Yankee")))
        val visited = mutableListOf<String>()
        while (queue != null) {
            queue.current?.videoId?.let(visited::add)
            queue = queue.next()
        }
        assertEquals(listOf("a", "x", "y", "b", "c"), visited)
    }

    @Test
    fun addToQueueAppendsAfterTheLastTrack() {
        val queue = assertNotNull(PlaybackQueue.of(tracks)).withLast(listOf(QueueTrack("x", "Xray")))
        assertEquals(listOf("a", "b", "c", "x"), queue.tracks.map { it.videoId })
        assertEquals("a", queue.current?.videoId)
    }

    @Test
    fun queueingNothingLeavesTheQueueAlone() {
        val queue = assertNotNull(PlaybackQueue.of(tracks))
        assertEquals(queue, queue.withNext(emptyList()))
        assertEquals(queue, queue.withLast(emptyList()))
    }
}
