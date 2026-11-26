package io.github.aedev.flow.innertube.pages

/** The suffixes a host language abbreviates counts with ("M", "Mio.", "万", "लाख"), each to its multiplier. */
internal fun interface CompactSuffixSource {
    /** Lowercased suffix to multiplier for [hl]; empty when the language is unknown. */
    fun suffixes(hl: String): Map<String, Long>
}

/**
 * Read off the platform's own CLDR data by formatting powers of ten, so every language YouTube
 * renders counts in is covered without a hand-kept table. Android answers from ICU; desktop and iOS
 * currently answer empty, which leaves the English K/M/B fallback in [YouTubeCountParser].
 */
internal object PlatformCompactSuffixSource : CompactSuffixSource {
    private val cache = mutableMapOf<String, Map<String, Long>>()

    override fun suffixes(hl: String): Map<String, Long> {
        val key = hl.lowercase()
        return cache.getOrPut(key) { platformCompactSuffixes(hl) }
    }
}

internal expect fun platformCompactSuffixes(hl: String): Map<String, Long>

private const val BIDI_MARKS = "\u200E\u200F\u061C"

// isSpaceChar covers the no-break spaces isWhitespace does not, and those are what number formats
// group with in several languages.
private const val NO_BREAK_SPACES = "\u00A0\u2007\u202F"

/**
 * Plain spaces for the no-break and thin ones number formats use, ASCII digits for every script's,
 * and no direction marks.
 */
internal fun normalizeCountText(raw: String): String =
    buildString(raw.length) {
        for (char in raw) {
            when {
                char in BIDI_MARKS -> Unit
                char.isDigit() -> append('0' + char.digitToInt())
                char.isWhitespace() || char in NO_BREAK_SPACES -> append(' ')
                else -> append(char)
            }
        }
    }
