package io.github.aedev.flow.ui.theme

import androidx.compose.ui.text.font.FontFamily

/** The font the app draws its text in. Stored by [storageId], so entries can be renamed freely. */
enum class AppFont(
    val storageId: String,
) {
    SYSTEM("system"),
    CONDENSED("condensed"),
    SERIF("serif"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromStorage(id: String?): AppFont = entries.firstOrNull { it.storageId == id } ?: SYSTEM
    }
}

/**
 * The family for [font]. A custom font comes from [customFontPath] through the platform's file
 * loader; anything missing or unreadable is the system font.
 */
expect fun appFontFamily(
    font: AppFont,
    customFontPath: String? = null,
): FontFamily
