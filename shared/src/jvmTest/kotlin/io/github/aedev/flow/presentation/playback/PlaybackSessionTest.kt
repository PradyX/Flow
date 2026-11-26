package io.github.aedev.flow.presentation.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Repeat decides only what an ended track leads to; the session codec must survive a restart. */
class PlaybackSessionTest {
    private val tracks =
        listOf(
            QueueTrack("a", "Alpha"),
            QueueTrack("b", "Bravo", "https://example.test/b.jpg"),
            QueueTrack("c", "Charlie"),
        )

    private fun queue(
        shuffled: Boolean = false,
        repeat: RepeatMode = RepeatMode.OFF,
    ): PlaybackQueue = assertNotNull(PlaybackQueue.of(tracks, shuffled = shuffled)).withRepeat(repeat)

    @Test
    fun roundingTheSessionKeepsQueueIndexModesAndPosition() {
        val original = queue(shuffled = true, repeat = RepeatMode.ALL).copy(index = 1)
        val restored = assertNotNull(PlaybackSession.decode(PlaybackSession.of(original, 42_000L).encode()))

        assertEquals(original.tracks, restored.toQueue()?.tracks)
        assertEquals(1, restored.toQueue()?.index)
        assertEquals(true, restored.toQueue()?.shuffled)
        assertEquals(RepeatMode.ALL, restored.toQueue()?.repeat)
        assertEquals(42_000L, restored.position())
        assertEquals(
            "https://example.test/b.jpg",
            restored
                .toQueue()
                ?.tracks
                ?.first { it.videoId == "b" }
                ?.thumbnailUrl,
        )
    }

    @Test
    fun anAbsentOrUnreadableSessionRestoresNothing() {
        assertNull(PlaybackSession.decode(null))
        assertNull(PlaybackSession.decode(""))
        assertNull(PlaybackSession.decode("not json at all"))
        assertNull(PlaybackSession.decode("""{"tracks":[],"index":0}"""))
    }

    @Test
    fun aStaleIndexComesBackInsideTheQueue() {
        val encoded =
            """
            {"tracks":[{"videoId":"a","title":"Alpha"},{"videoId":"b","title":"Bravo"}],"index":9,"positionMs":1000}
            """.trimIndent()
        val restored = assertNotNull(PlaybackSession.decode(encoded))
        assertEquals(1, restored.toQueue()?.index)
    }

    @Test
    fun tracksWithoutAnIdAreDroppedAndDoNotCountAsAQueue() {
        val encoded =
            """
            {"tracks":[{"videoId":"","title":"Gone"},{"videoId":"a","title":"Alpha"}],"index":1}
            """.trimIndent()
        val restored = assertNotNull(PlaybackSession.decode(encoded))
        assertEquals(listOf("a"), restored.toQueue()?.tracks?.map { it.videoId })
        assertEquals(0, restored.toQueue()?.index)
        assertNull(PlaybackSession.decode("""{"tracks":[{"videoId":"","title":"Gone"}],"index":0}"""))
    }

    @Test
    fun aNegativePositionIsReadAsTheStartOfTheTrack() {
        assertEquals(0L, PlaybackSession.of(queue(), -5L).position())
    }

    @Test
    fun repeatAllWrapsTheEndOfTheQueueBackToItsStart() {
        val atEnd = queue(repeat = RepeatMode.ALL).copy(index = 2)
        assertEquals(0, atEnd.afterEnded()?.index)
    }

    @Test
    fun repeatOneStaysOnTheSameTrack() {
        val queue = queue(repeat = RepeatMode.ONE).copy(index = 1)
        assertEquals(1, queue.afterEnded()?.index)
    }

    @Test
    fun repeatOffStopsAfterTheLastTrack() {
        assertNull(queue().copy(index = 2).afterEnded())
        assertEquals(1, queue().afterEnded()?.index)
    }

    @Test
    fun thePreResolveWindowIsThisTrackAndTheNextOnes() {
        assertEquals(listOf(0, 1, 2), queue().preResolveWindow(ahead = 2))
        assertEquals(listOf(1, 2), queue().copy(index = 1).preResolveWindow(ahead = 2))
        assertEquals(listOf(2), queue().copy(index = 2).preResolveWindow(ahead = 2))
        assertTrue(queue().preResolveWindow(ahead = 0) == listOf(0))
    }

    @Test
    fun theWindowWrapsOnlyWhenRepeatAllWould() {
        assertEquals(listOf(2, 0, 1), queue(repeat = RepeatMode.ALL).copy(index = 2).preResolveWindow(ahead = 2))
        assertEquals(listOf(2), queue(repeat = RepeatMode.ONE).copy(index = 2).preResolveWindow(ahead = 2))
    }

    @Test
    fun repeatCyclesOffAllOne() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }
}
