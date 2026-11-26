package io.github.aedev.flow.data.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoNotesTest {
    private val notes =
        VideoNotes()
            .add("v1", "First", 90_000, "  the chorus  ", 1_000)
            .add("v1", "First", 5_000, "intro", 2_000)
            .add("v2", "Second", 0, "whole clip", 3_000)

    @Test
    fun aNoteKeepsItsVideoMomentAndTrimmedText() {
        val newest = notes.notes.first()
        assertEquals("v2", newest.videoId)
        assertEquals(0, newest.timestampMs)
        val chorus = notes.notes.first { it.text == "the chorus" }
        assertEquals(90_000, chorus.timestampMs)
    }

    @Test
    fun blankTextOrAVideoWithoutAnIdIsNotNoted() {
        assertEquals(notes, notes.add("v1", "First", 1_000, "   ", 4_000))
        assertEquals(notes, notes.add("", "First", 1_000, "text", 5_000))
    }

    @Test
    fun aVideosNotesAreOrderedByMomentInIt() {
        assertEquals(listOf(5_000L, 90_000L), notes.forVideo("v1").map { it.timestampMs })
        assertEquals(1, notes.forVideo("v2").size)
        assertTrue(notes.forVideo("v3").isEmpty())
    }

    @Test
    fun deletingOneNoteLeavesTheOthers() {
        val removed = notes.without(notes.notes.first().id)
        assertEquals(2, removed.notes.size)
        assertTrue(removed.notes.none { it.videoId == "v2" })
    }

    @Test
    fun theRecordSurvivesItsCodec() {
        val decoded = VideoNotes.decode(notes.encode())
        assertEquals(notes.notes.map { it.id }, decoded.notes.map { it.id })
        assertEquals(notes.notes.map { it.timestampMs }, decoded.notes.map { it.timestampMs })
    }

    @Test
    fun unreadableTextReadsAsNoNotes() {
        assertTrue(VideoNotes.decode(null).notes.isEmpty())
        assertTrue(VideoNotes.decode("[broken").notes.isEmpty())
    }
}
