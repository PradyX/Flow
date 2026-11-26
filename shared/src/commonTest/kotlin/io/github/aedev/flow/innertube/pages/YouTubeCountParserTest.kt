package io.github.aedev.flow.innertube.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The number reading and suffix matching, against a hand-kept table so it runs on every target. */
class YouTubeCountParserTest {
    private val fakeSuffixes =
        CompactSuffixSource { hl ->
            when (hl) {
                "es" -> mapOf("mil" to 1_000L, "m" to 1_000_000L)
                "de" -> mapOf("mio." to 1_000_000L, "mrd." to 1_000_000_000L)
                "tr" -> mapOf("b" to 1_000L, "mn" to 1_000_000L)
                "ja" -> mapOf("万" to 10_000L, "億" to 100_000_000L)
                "ko" -> mapOf("천" to 1_000L, "만" to 10_000L)
                else -> emptyMap()
            }
        }

    private fun parse(
        text: String?,
        hl: String?,
    ) = YouTubeCountParser.parse(text, hl, fakeSuffixes)

    @Test
    fun `exact counts read the same in every language`() {
        assertEquals(1_013_511L, parse("1,013,511 views", "en"))
        assertEquals(1_013_511L, parse("1.013.511 visualizaciones", "es"))
        assertEquals(1_013_511L, parse("1 013 511 vues", "fr"))
        assertEquals(334_435L, parse("334.435 Aufrufe", "de"))
        assertEquals(2_488_417L, parse("24,88,417 व्यूज़", "hi"))
        assertEquals(1_013_511L, parse("조회수 1,013,511회", "ko"))
        assertEquals(1L, parse("1 view", "en"))
    }

    @Test
    fun `a decimal comma is not a thousands separator`() {
        assertEquals(2_400_000L, parse("2,4 M de visualizaciones", "es"))
        assertEquals(2_400_000L, parse("2,4 Mio. Aufrufe", "de"))
        assertEquals(2_400_000L, parse("2.4M views", "en"))
    }

    @Test
    fun `the host language's suffix wins over the english letter`() {
        assertEquals(334_000L, parse("334 B görüntüleme", "tr"))
        assertEquals(1_200_000_000L, parse("1.2B views", "en"))
    }

    @Test
    fun `english letters still read where YouTube uses them in another language`() {
        assertEquals(334_000L, parse("334 K visualizaciones", "es"))
        assertEquals(334_000L, parse("334K visualizzazioni", "it"))
    }

    @Test
    fun `an alphabetic suffix must end its word`() {
        assertEquals(334_000L, parse("334 mil visualizaciones", "es"))
        assertEquals(334L, parse("334 mil visualizações", "xx"))
    }

    @Test
    fun `han and hangul suffixes run into the next word`() {
        assertEquals(330_000L, parse("33万回視聴", "ja"))
        assertEquals(330_000L, parse("조회수 33만회", "ko"))
    }

    @Test
    fun `a digit in any script counts`() {
        assertEquals(334L, parse("٣٣٤ views", "en"))
    }

    @Test
    fun `text without a number is no count`() {
        assertNull(parse(null, "en"))
        assertNull(parse("No views", "en"))
        assertEquals(0L, parseYouTubeViewCount("Sin visualizaciones", "es"))
    }
}
