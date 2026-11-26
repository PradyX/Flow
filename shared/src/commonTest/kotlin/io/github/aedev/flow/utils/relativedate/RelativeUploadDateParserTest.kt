/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.utils.relativedate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelativeUploadDateParserTest {
    private val now = 1_700_000_000_000L
    private val minuteMs = 60_000L
    private val hourMs = 3_600_000L
    private val dayMs = 86_400_000L
    private val weekMs = 7 * dayMs
    private val monthMs = 30 * dayMs
    private val yearMs = 365 * dayMs

    private fun parse(
        text: String?,
        hl: String? = "en",
    ) = RelativeUploadDateParser.parse(text, hl, now)

    @Test
    fun `parses common relative dates`() {
        assertEquals(now - 3 * dayMs, parse("3 days ago"))
        assertEquals(now - 2 * weekMs, parse("2 weeks ago"))
        assertEquals(now - monthMs, parse("1 month ago"))
        assertEquals(now - 4 * yearMs, parse("4 years ago"))
        assertEquals(now - 45 * minuteMs, parse("45 mins ago"))
        assertEquals(now - 2 * hourMs, parse("2 hrs ago"))
    }

    @Test
    fun `an article reads as one`() {
        assertEquals(now - hourMs, parse("an hour ago"))
        assertEquals(now - yearMs, parse("a year ago"))
    }

    @Test
    fun `compact english shorthand reads only as the whole text`() {
        assertEquals(now - 3 * dayMs, parse("3d"))
        assertEquals(now - 5 * hourMs, parse("5h ago"))
        assertEquals(now - 2 * monthMs, parse("2mo ago"))
        assertEquals(now - 30_000L, parse("30s"))
    }

    @Test
    fun `strips streamed and premiered prefixes`() {
        assertEquals(now - 2 * dayMs, parse("Streamed 2 days ago"))
        assertEquals(now - 5 * hourMs, parse("Premiered 5 hours ago"))
    }

    @Test
    fun `unknown text returns null - never now`() {
        assertNull(parse(null))
        assertNull(parse(""))
        assertNull(parse("some random text"))
    }

    @Test
    fun `today and yesterday resolve`() {
        assertEquals(now, parse("today"))
        assertEquals(now - dayMs, parse("yesterday"))
    }

    @Test
    fun `a localized age is not read as seconds when the host language is unknown`() {
        assertNull(parse("hace 3 semanas", hl = "en"))
        assertNull(parse("il y a 2 mois", hl = null))
        assertNull(parse("há 5 anos", hl = "xx"))
    }

    @Test
    fun `the top content languages parse in their own words`() {
        val cases =
            listOf(
                Triple("es", "hace 3 semanas", 3 * weekMs),
                Triple("fr", "il y a 2 mois", 2 * monthMs),
                Triple("pt", "há 5 anos", 5 * yearMs),
                Triple("de", "vor 4 Stunden", 4 * hourMs),
                Triple("ru", "3 недели назад", 3 * weekMs),
                Triple("ar", "منذ 3 أسابيع", 3 * weekMs),
                Triple("ja", "3 日前", 3 * dayMs),
                Triple("it", "2 settimane fa", 2 * weekMs),
                Triple("tr", "5 gün önce", 5 * dayMs),
                Triple("hi", "2 घंटे पहले", 2 * hourMs),
                Triple("id", "3 minggu yang lalu", 3 * weekMs),
            )
        for ((hl, text, age) in cases) {
            assertEquals(now - age, parse(text, hl), "$hl: $text")
        }
    }

    @Test
    fun `every language Flow ships a translation for parses`() {
        val cases =
            listOf(
                Triple("ar", "منذ 11 شهرًا", 11 * monthMs),
                Triple("az", "2 həftə əvvəl", 2 * weekMs),
                Triple("yue", "3 週前", 3 * weekMs),
                Triple("bs", "prije 2 dana", 2 * dayMs),
                Triple("cs", "před 3 dny", 3 * dayMs),
                Triple("de", "vor 1 Jahr", yearMs),
                Triple("es", "hace 10 minutos", 10 * minuteMs),
                Triple("et", "5 päeva tagasi", 5 * dayMs),
                Triple("fa", "۳ هفته پیش", 3 * weekMs),
                Triple("fr", "il y a 4 jours", 4 * dayMs),
                Triple("hi", "1 महीना पहले", monthMs),
                Triple("in", "2 bulan yang lalu", 2 * monthMs),
                Triple("it", "3 anni fa", 3 * yearMs),
                Triple("ja", "2 か月前", 2 * monthMs),
                Triple("ko", "2일 전", 2 * dayMs),
                Triple("lo", "3 ມື້ກ່ອນນີ້", 3 * dayMs),
                Triple("pa", "2 ਹਫ਼ਤੇ ਪਹਿਲਾਂ", 2 * weekMs),
                Triple("pl", "2 tygodnie temu", 2 * weekMs),
                Triple("pt", "há 1 mês", monthMs),
                Triple("pt-BR", "há 6 horas", 6 * hourMs),
                Triple("ru", "1 год назад", yearMs),
                Triple("si", "දින 3කට පෙර", 3 * dayMs),
                Triple("sr", "пре 2 дана", 2 * dayMs),
                Triple("tr", "1 yıl önce", yearMs),
                Triple("uk", "3 тижні тому", 3 * weekMs),
                Triple("vi", "2 tuần trước", 2 * weekMs),
                Triple("zh-CN", "3天前", 3 * dayMs),
                Triple("zh-TW", "2 個月前", 2 * monthMs),
                Triple("en", "7 hours ago", 7 * hourMs),
            )
        for ((hl, text, age) in cases) {
            assertEquals(now - age, parse(text, hl), "$hl: $text")
        }
    }

    @Test
    fun `duals without a numeral read as two`() {
        assertEquals(now - 2 * dayMs, parse("منذ يومين", hl = "ar"))
        assertEquals(now - hourMs, parse("منذ ساعة", hl = "ar"))
        assertEquals(now - 2 * weekMs, parse("לפני שבועיים", hl = "he"))
    }

    @Test
    fun `region variants fall back to their language`() {
        assertEquals(now - 2 * dayMs, parse("hace 2 días", hl = "es-419"))
        assertEquals(now - yearMs, parse("il y a 1 an", hl = "fr_CA"))
        assertEquals(now - 3 * weekMs, parse("3 週前", hl = "zh-Hant-TW"))
        assertEquals(now - 3 * weekMs, parse("3 週前", hl = "zh-HK"))
    }

    @Test
    fun `a localized streamed prefix does not hide the age`() {
        assertEquals(now - 2 * dayMs, parse("Emitido hace 2 días", hl = "es"))
        assertEquals(now - 3 * weekMs, parse("Diffusé il y a 3 semaines", hl = "fr"))
    }

    @Test
    fun `view counts and scheduled starts are not ages`() {
        assertNull(parse("1,2 M de visualizaciones", hl = "es"))
        assertNull(parse("Se estrena el 12/10/26, 18:00", hl = "es"))
        assertNull(parse("Première le 12 oct. à 18 h", hl = "fr"))
        assertNull(parse("1.2M views", hl = "en"))
    }

    @Test
    fun `an age keeps the unit it was given in`() {
        assertEquals(RelativeDateUnit.YEAR, RelativeUploadDateParser.read("1 year ago", "en", now)?.unit)
        assertEquals(RelativeDateUnit.WEEK, RelativeUploadDateParser.read("hace 3 semanas", "es", now)?.unit)
        assertEquals(RelativeDateUnit.HOUR, RelativeUploadDateParser.read("5h", "en", now)?.unit)
        assertEquals(RelativeDateUnit.DAY, RelativeUploadDateParser.read("Streamed 2 days ago", "en", now)?.unit)
    }

    @Test
    fun `only an age under a day places the calendar date`() {
        assertTrue(RelativeUploadDateParser.read("just now", "en", now)?.placesCalendarDay == true)
        assertTrue(RelativeUploadDateParser.read("45 minutes ago", "en", now)?.placesCalendarDay == true)
        assertTrue(RelativeUploadDateParser.read("hace 5 horas", "es", now)?.placesCalendarDay == true)
        assertFalse(RelativeUploadDateParser.read("yesterday", "en", now)?.placesCalendarDay == true)
        assertFalse(RelativeUploadDateParser.read("3 days ago", "en", now)?.placesCalendarDay == true)
        assertFalse(RelativeUploadDateParser.read("1 year ago", "en", now)?.placesCalendarDay == true)
    }

    @Test
    fun `reads the abbreviated ages of the related list`() {
        val cases =
            listOf(
                Triple("vor 7 Std.", "de", 7 * hourMs),
                Triple("hace 7 h", "es", 7 * hourMs),
                Triple("hace 6 d", "es", 6 * dayMs),
                Triple("Emitido hace 7 d", "es", 7 * dayMs),
                Triple("hace 2 sem.", "es", 2 * weekMs),
                Triple("hace 3 m", "es", 3 * monthMs),
                Triple("Emitido hace 1 m", "es", monthMs),
                Triple("hace 15 a", "es", 15 * yearMs),
                Triple("il y a 7 h", "fr", 7 * hourMs),
                Triple("Diffusé il y a 7 j", "fr", 7 * dayMs),
                Triple("il y a 3 sem.", "fr", 3 * weekMs),
                Triple("il y a 2 m.", "fr", 2 * monthMs),
                Triple("il y a 10 a", "fr", 10 * yearMs),
                Triple("13 h fa", "it", 13 * hourMs),
                Triple("1 g fa", "it", dayMs),
                Triple("4 gg fa", "it", 4 * dayMs),
                Triple("2 sett. fa", "it", 2 * weekMs),
                Triple("há 2 sem.", "pt", 2 * weekMs),
                Triple("21 ч назад", "ru", 21 * hourMs),
                Triple("Трансляция закончилась 10 ч назад", "ru", 10 * hourMs),
                Triple("5 дн. назад", "ru", 5 * dayMs),
                Triple("4 нед. назад", "ru", 4 * weekMs),
                Triple("2 мес. назад", "ru", 2 * monthMs),
                Triple("2 г. назад", "ru", 2 * yearMs),
                Triple("15 л. назад", "ru", 15 * yearMs),
            )
        cases.forEach { (text, hl, age) ->
            assertEquals(now - age, parse(text, hl), "$hl: $text")
        }
    }

    @Test
    fun `an abbreviated unit counts only right after the one number in the text`() {
        assertNull(parse("1,3 M", hl = "es"))
        assertNull(parse("il y a", hl = "fr"))
        assertNull(parse("Première le 12 oct. à 18 h", hl = "fr"))
        assertNotEquals(now - 5 * monthMs, parse("hace 5 min", hl = "es"))
    }

    @Test
    fun `read and parse agree on the timestamp`() {
        listOf("today", "yesterday", "just now", "3 days ago", "2 hrs ago", "1 month ago").forEach { text ->
            assertEquals(parse(text), RelativeUploadDateParser.read(text, "en", now)?.timestamp, text)
        }
    }
}
