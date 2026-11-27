package io.github.aedev.flow.data.settings

import io.github.aedev.flow.ui.theme.ThemeMode
import io.github.aedev.flow.ui.theme.ThemeVariant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The appearance the settings page offers, each one a palette and style pair the theme knows. */
enum class Appearance {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
    MATERIAL_YOU,
    ;

    /** The mode and style this choice resolves to; [SYSTEM] takes both from the device. */
    val themeMode: ThemeMode
        get() =
            when (this) {
                SYSTEM -> ThemeMode.SYSTEM
                MATERIAL_YOU -> ThemeMode.MATERIAL_YOU
                else -> ThemeMode.DARK
            }

    val themeVariant: ThemeVariant
        get() =
            when (this) {
                LIGHT -> ThemeVariant.LIGHT
                AMOLED -> ThemeVariant.AMOLED
                else -> ThemeVariant.DARK
            }
}

/** The delays the settings page offers for the countdown, the reference's own range in seconds. */
val AUTOPLAY_DELAYS = listOf(0, 5, 10)

/**
 * The settings the app keeps for itself: what the appearance page writes and the shell reads. It is
 * a record with an encoder so an unreadable value falls back to the defaults instead of stopping
 * the app.
 */
@Serializable
data class AppSettings(
    val appearance: Appearance = Appearance.SYSTEM,
    /**
     * The reference's "Auto-play Next Video": whether a queue that runs out rolls into the related
     * rail. On by default, as it is in the app.
     */
    val autoplayNext: Boolean = true,
    /**
     * The reference's "Autoplay delay", which starts at 0: the next video switches at once, and the
     * countdown card only appears for a delay the viewer chose.
     */
    val autoplayDelaySeconds: Int = 0,
) {
    fun withAppearance(next: Appearance): AppSettings = copy(appearance = next)

    fun withAutoplay(next: Boolean): AppSettings = copy(autoplayNext = next)

    fun withAutoplayDelay(seconds: Int): AppSettings =
        copy(autoplayDelaySeconds = seconds.coerceIn(AUTOPLAY_DELAYS.first(), AUTOPLAY_DELAYS.last()))

    fun encode(): String = SettingsJson.encodeToString(serializer(), this)

    companion object {
        fun decode(json: String?): AppSettings {
            val text = json?.takeIf { it.isNotBlank() } ?: return AppSettings()
            return runCatching { SettingsJson.decodeFromString(serializer(), text) }.getOrNull() ?: AppSettings()
        }
    }
}

private val SettingsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
