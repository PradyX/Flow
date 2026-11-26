package io.github.aedev.flow.innertube.pages

/** Foundation exposes no compact-decimal suffix table, so counts parse with the English fallback. */
internal actual fun platformCompactSuffixes(hl: String): Map<String, Long> = emptyMap()
