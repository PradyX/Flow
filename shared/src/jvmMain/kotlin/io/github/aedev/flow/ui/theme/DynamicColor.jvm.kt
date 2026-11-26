package io.github.aedev.flow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Desktop has no wallpaper-derived scheme; every palette comes from [FlowPalettes]. */
@Composable
actual fun rememberFlowDynamicScheme(variant: ThemeVariant): ColorScheme? = null
