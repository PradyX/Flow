package io.github.aedev.flow.data.settings

import java.io.File

actual fun loadAppSettingsJson(): String? =
    runCatching { settingsFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveAppSettingsJson(json: String) {
    runCatching {
        val file = settingsFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun settingsFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-settings.json")
