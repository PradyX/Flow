package io.github.aedev.flow.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFlowDynamicScheme(variant: ThemeVariant): ColorScheme? = androidDynamicScheme(LocalContext.current, variant)

/**
 * Material You on API 31+, null below it. Exposed for the home-screen widgets, which resolve the
 * scheme outside a composition and must match the in-app palette exactly.
 */
fun androidDynamicScheme(
    context: Context,
    variant: ThemeVariant,
): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return when (variant) {
        ThemeVariant.LIGHT -> dynamicLightColorScheme(context)
        ThemeVariant.DARK -> dynamicDarkColorScheme(context)
        ThemeVariant.AMOLED -> dynamicDarkColorScheme(context).withBlackSurfaces()
    }
}
