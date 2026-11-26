package io.github.aedev.flow.innertube.pages

import android.icu.text.CompactDecimalFormat
import android.icu.util.ULocale

internal actual fun platformCompactSuffixes(hl: String): Map<String, Long> =
    runCatching {
        val locale = ULocale.forLanguageTag(hl.replace('_', '-'))
        val table = LinkedHashMap<String, Long>()
        for (style in listOf(CompactDecimalFormat.CompactStyle.SHORT, CompactDecimalFormat.CompactStyle.LONG)) {
            val format = CompactDecimalFormat.getInstance(locale, style)
            for (exponent in 3..12) {
                val magnitude = POWERS_OF_TEN[exponent]
                for (lead in PLURAL_LEADS) {
                    val value = lead * magnitude
                    val (shown, suffix) = splitFormatted(format.format(value)) ?: continue
                    if (suffix.isEmpty() || shown <= 0L || value % shown != 0L) continue
                    table.putIfAbsent(suffix, value / shown)
                }
            }
        }
        table.toMap()
    }.getOrElse { emptyMap() }

private fun splitFormatted(formatted: String?): Pair<Long, String>? {
    val text = normalizeCountText(formatted ?: return null)
    val digits = text.filter(Char::isDigit)
    if (digits.isEmpty() || digits.length > 15) return null
    val suffix =
        text
            .filterNot { it.isDigit() || it in NUMBER_PUNCTUATION }
            .trim()
            .lowercase()
    return digits.toLong() to suffix
}

private val POWERS_OF_TEN = LongArray(13) { exponent -> (1..exponent).fold(1L) { acc, _ -> acc * 10L } }

private val PLURAL_LEADS = longArrayOf(1L, 2L, 5L)

private const val NUMBER_PUNCTUATION = ".,'\u2019\u066B\u066C"
