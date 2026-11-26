package io.github.aedev.flow.data.settings

import io.github.aedev.flow.ui.theme.ThemeMode
import io.github.aedev.flow.ui.theme.ThemeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test
    fun theDefaultFollowsTheDevice() {
        val settings = AppSettings.decode(null)
        assertEquals(Appearance.SYSTEM, settings.appearance)
        assertEquals(ThemeMode.SYSTEM, settings.appearance.themeMode)
    }

    @Test
    fun everyAppearanceSurvivesItsCodec() {
        Appearance.entries.forEach { appearance ->
            val decoded = AppSettings.decode(AppSettings().withAppearance(appearance).encode())
            assertEquals(appearance, decoded.appearance)
        }
    }

    @Test
    fun unreadableTextFallsBackToTheDefaults() {
        assertEquals(Appearance.SYSTEM, AppSettings.decode("nonsense").appearance)
        assertEquals(Appearance.SYSTEM, AppSettings.decode("""{"appearance":"NOT_A_MODE"}""").appearance)
    }

    @Test
    fun lightAndTrueBlackAreTheStyleTheyName() {
        assertEquals(ThemeVariant.LIGHT, Appearance.LIGHT.themeVariant)
        assertEquals(ThemeVariant.AMOLED, Appearance.AMOLED.themeVariant)
        assertEquals(ThemeVariant.DARK, Appearance.DARK.themeVariant)
        assertEquals(ThemeMode.MATERIAL_YOU, Appearance.MATERIAL_YOU.themeMode)
    }
}
