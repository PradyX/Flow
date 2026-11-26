package io.github.aedev.flow.presentation.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The checkpoint carries the exact position, and only for the track the session is actually on. */
class PlaybackCheckpointTest {
    private val queue =
        assertNotNull(
            PlaybackQueue.of(
                listOf(
                    QueueTrack("a", "Alpha"),
                    QueueTrack("b", "Bravo"),
                    QueueTrack("c", "Charlie"),
                ),
            ),
        )

    @Test
    fun theCheckpointKeepsItsMillisecond() {
        val restored = assertNotNull(PlaybackCheckpoint.decode(PlaybackCheckpoint("b", 83_417L).encode()))
        assertEquals("b", restored.videoId)
        assertEquals(83_417L, restored.positionMs)
    }

    @Test
    fun anAbsentOrBrokenCheckpointReadsAsNothing() {
        assertNull(PlaybackCheckpoint.decode(null))
        assertNull(PlaybackCheckpoint.decode(""))
        assertNull(PlaybackCheckpoint.decode("{"))
        assertNull(PlaybackCheckpoint.decode("""{"videoId":"","positionMs":10}"""))
    }

    @Test
    fun theCheckpointOfTheSessionsOwnTrackRestoresThatPosition() {
        val session = PlaybackSession.of(queue.copy(index = 1), 5_000)
        val restored = session.withCheckpoint(PlaybackCheckpoint("b", 41_250L))
        assertEquals(41_250L, restored.position())
        assertEquals(1, restored.toQueue()?.index)
    }

    @Test
    fun aCheckpointFromAnotherTrackIsIgnored() {
        val session = PlaybackSession.of(queue.copy(index = 1), 5_000)
        assertEquals(5_000L, session.withCheckpoint(PlaybackCheckpoint("a", 90_000L)).position())
        assertEquals(5_000L, session.withCheckpoint(PlaybackCheckpoint("gone", 90_000L)).position())
        assertEquals(5_000L, session.withCheckpoint(null).position())
    }

    @Test
    fun aNegativeCheckpointStartsTheTrackInstead() {
        val session = PlaybackSession.of(queue, 0)
        assertEquals(0L, session.withCheckpoint(PlaybackCheckpoint("a", -900L)).position())
    }

    @Test
    fun aCheckpointRestoresTheExactMillisecondAfterTheSessionRoundTrips() {
        val written = PlaybackSession.of(queue.copy(index = 2, repeat = RepeatMode.ALL), 0)
        val decoded = assertNotNull(PlaybackSession.decode(written.encode()))
        val resumed = decoded.withCheckpoint(PlaybackCheckpoint.decode(PlaybackCheckpoint("c", 137_884L).encode()))
        assertEquals(137_884L, resumed.position())
        assertEquals(RepeatMode.ALL, resumed.toQueue()?.repeat)
        assertEquals(2, resumed.toQueue()?.index)
    }
}
