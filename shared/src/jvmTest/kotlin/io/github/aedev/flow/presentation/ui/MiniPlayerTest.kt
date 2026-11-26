package io.github.aedev.flow.presentation.ui

import io.github.aedev.flow.presentation.playback.NoQueuePlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MiniPlayerTest {
    @Test
    fun `the bar prints a position as minutes and seconds`() {
        assertEquals("0:00", clockText(0))
        assertEquals("0:07", clockText(7_400))
        assertEquals("1:05", clockText(65_000))
        assertEquals("59:59", clockText(3_599_000))
    }

    @Test
    fun `an hour of playback keeps its hours`() {
        assertEquals("1:00:00", clockText(3_600_000))
        assertEquals("2:05:09", clockText(7_509_000))
    }

    @Test
    fun `a position the engine has not advanced past still reads as zero`() {
        assertEquals("0:00", clockText(-5_000))
    }

    @Test
    fun `a target without an engine reports nothing to show`() {
        assertFalse(NoQueuePlayer.isPlaying())
        assertEquals(0, NoQueuePlayer.durationMs())
        assertEquals(0, NoQueuePlayer.positionMs())
    }
}
