package io.github.aedev.flow.innertube.models

import io.github.aedev.flow.innertube.network.platformDefaultLocale
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeLocale(
    val gl: String, // geolocation
    val hl: String, // host language
) {
    companion object {
        val EXTRACTION = YouTubeLocale(gl = "US", hl = "en")
    }
}

/**
 * The `hl` the client sends, normalized the way the app's did.
 *
 * `Locale.forLanguageTag` has no common equivalent, so the tag is split into its subtags: the first
 * is the language, a four-letter subtag is a script and a two-letter or three-digit one is a region.
 * That is all this function ever read from the parsed locale.
 */
internal fun normalizeYouTubeHostLanguage(
    languageTag: String,
    fallbackLanguageTag: String = platformDefaultLocale().languageTag,
): String {
    val candidate =
        languageTag
            .trim()
            .takeUnless { it.isBlank() || it.equals("system", ignoreCase = true) }
            ?: fallbackLanguageTag
    val subtags = candidate.replace('_', '-').split('-').filter { it.isNotEmpty() }
    val language =
        subtags
            .firstOrNull()
            ?.lowercase()
            ?.takeUnless { it.isBlank() || it == "und" }
            ?: "en"
    val rest = subtags.drop(1)
    val script = rest.firstOrNull { it.length == 4 && it.all { c -> c.isLetter() } }
    val region =
        rest.firstOrNull {
            (it.length == 2 && it.all { c -> c.isLetter() }) ||
                (it.length == 3 && it.all { c -> c.isDigit() })
        }

    return when (language) {
        "kab" -> {
            "en"
        }

        "pt" -> {
            if (region.equals("BR", ignoreCase = true)) "pt-BR" else "pt"
        }

        "zh" -> {
            when {
                region.equals("HK", ignoreCase = true) -> "zh-HK"

                script.equals("Hant", ignoreCase = true) ||
                    region.equals("TW", ignoreCase = true) ||
                    region.equals("MO", ignoreCase = true) -> "zh-TW"

                else -> "zh-CN"
            }
        }

        else -> {
            language
        }
    }
}
