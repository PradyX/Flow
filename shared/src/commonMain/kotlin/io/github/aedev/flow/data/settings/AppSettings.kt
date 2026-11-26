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

/**
 * The settings the app keeps for itself: what the appearance page writes and the shell reads. It is
 * a record with an encoder so an unreadable value falls back to the defaults instead of stopping
 * the app.
 */
@Serializable
data class AppSettings(
    val appearance: Appearance = Appearance.SYSTEM,
) {
    fun withAppearance(next: Appearance): AppSettings = copy(appearance = next)

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
