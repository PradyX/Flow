package io.github.aedev.flow.ui.theme

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File

private val CondensedWeights =
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)

actual fun appFontFamily(
    font: AppFont,
    customFontPath: String?,
): FontFamily =
    when (font) {
        AppFont.SYSTEM -> FlowFontFamily
        AppFont.CONDENSED -> FontFamily(CondensedWeights.map { Font(DeviceFontFamilyName("sans-serif-condensed"), it) })
        AppFont.SERIF -> FontFamily.Serif
        AppFont.CUSTOM -> customFontPath?.let { File(it) }?.takeIf { it.isFile }?.let { loadAndroidFont(it) } ?: FlowFontFamily
    }

private fun loadAndroidFont(file: File): FontFamily? =
    runCatching {
        FontFamily(Font(file))
    }.getOrNull()
