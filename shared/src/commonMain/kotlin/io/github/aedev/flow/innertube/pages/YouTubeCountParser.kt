package io.github.aedev.flow.innertube.pages

/**
 * Reads a count as YouTube renders it in the request's host language: an exact one
 * ("1.013.511 visualizaciones", "334 435 просмотров") or an abbreviated one ("2,4 M de
 * visualizaciones", "33万回視聴", "3.3 लाख व्यूज़"). The suffix is looked up in [hl]'s own table and
 * then as English K/M/B, which YouTube also uses in some languages ("334 K visualizaciones").
 */
internal object YouTubeCountParser {
    fun parse(
        text: String?,
        hl: String?,
        suffixSource: CompactSuffixSource = PlatformCompactSuffixSource,
    ): Long? {
        val normalized = normalizeCountText(text ?: return null)
        val number = NUMBER.find(normalized) ?: return null
        val (digits, scale) = readNumber(number.value) ?: return null
        val rest = normalized.substring(number.range.last + 1).trimStart().lowercase()
        val multiplier =
            hl
                ?.takeIf(String::isNotBlank)
                ?.let { matchSuffix(rest, suffixSource.suffixes(it)) }
                ?: matchSuffix(rest, ENGLISH_SUFFIXES)
                ?: 1L
        return scaledCount(digits, scale, multiplier)
    }

    private val NUMBER = Regex("""[0-9](?:[0-9.,'’ ٫٬]*[0-9])?""")

    private val ENGLISH_SUFFIXES = mapOf("k" to 1_000L, "m" to 1_000_000L, "b" to 1_000_000_000L)

    /**
     * The token as its digits and the power of ten to divide by. A grouping separator is always
     * followed by three digits and a compact decimal never is, so the text itself tells "2,4 M"
     * from "334.435" without trusting the locale's symbols, which differ from what YouTube prints
     * in some languages (Arabic's U+066B against YouTube's ".").
     */
    private fun readNumber(token: String): Pair<Long, Int>? {
        val lastSeparator = token.indexOfLast { !it.isDigit() }
        val digitsAfter = token.length - lastSeparator - 1
        val isDecimal = lastSeparator >= 0 && token[lastSeparator] in DECIMAL_MARKS && digitsAfter != 3
        val integerPart: String
        val fractionPart: String
        if (isDecimal) {
            integerPart = token.substring(0, lastSeparator).filter(Char::isDigit)
            fractionPart = token.substring(lastSeparator + 1).filter(Char::isDigit)
        } else {
            integerPart = token.filter(Char::isDigit)
            fractionPart = ""
        }
        val digits = (integerPart + fractionPart).trimStart('0').ifEmpty { "0" }
        if (digits.length > MAX_DIGITS) return null
        return digits.toLong() to fractionPart.length
    }

    // BigDecimal truncated toward zero, which integer division does the same way for the positive
    // values these counts always are. The guard replaces BigDecimal's arbitrary precision.
    private fun scaledCount(
        digits: Long,
        scale: Int,
        multiplier: Long,
    ): Long? {
        if (multiplier != 0L && digits > Long.MAX_VALUE / multiplier) return null
        val product = digits * multiplier
        var divisor = 1L
        repeat(scale) { divisor *= 10L }
        return product / divisor
    }

    private fun matchSuffix(
        rest: String,
        suffixes: Map<String, Long>,
    ): Long? =
        suffixes.entries
            .filter { (suffix, _) -> rest.startsWith(suffix) && endsWord(rest, suffix) }
            .maxByOrNull { (suffix, _) -> suffix.length }
            ?.value

    // Han and Hangul suffixes run straight into the next word ("33万回視聴", "33만회"); alphabetic
    // ones must end where a word does, or "mil" would read as English "m".
    private fun endsWord(
        rest: String,
        suffix: String,
    ): Boolean {
        val next = rest.getOrNull(suffix.length) ?: return true
        val last = suffix.last()
        return !last.isLetter() || isUnspacedScript(last.code) || !next.isLetter()
    }

    private fun isUnspacedScript(code: Int): Boolean = HAN_RANGES.any { code in it } || HANGUL_RANGES.any { code in it }

    private val HAN_RANGES = listOf(0x4E00..0x9FFF, 0x3400..0x4DBF, 0xF900..0xFAFF)
    private val HANGUL_RANGES = listOf(0xAC00..0xD7AF, 0x1100..0x11FF, 0x3130..0x318F)

    private const val DECIMAL_MARKS = ".,٫"
    private const val MAX_DIGITS = 18
}
