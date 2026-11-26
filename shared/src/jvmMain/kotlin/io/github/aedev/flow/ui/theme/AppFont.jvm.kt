package io.github.aedev.flow.ui.theme

import androidx.compose.ui.text.font.FontFamily

/** Desktop draws in the system font until a bundled family is added. */
actual fun appFontFamily(
    font: AppFont,
    customFontPath: String?,
): FontFamily =
    when (font) {
        AppFont.SERIF -> FontFamily.Serif
        else -> FlowFontFamily
    }
