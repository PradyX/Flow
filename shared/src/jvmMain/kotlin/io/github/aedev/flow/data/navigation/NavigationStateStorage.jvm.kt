package io.github.aedev.flow.data.navigation

import java.io.File

actual fun loadNavigationStateJson(): String? = runCatching { navigationStateFile().takeIf { it.isFile }?.readText() }.getOrNull()

actual fun saveNavigationStateJson(json: String) {
    runCatching {
        val file = navigationStateFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun navigationStateFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-navigation-state.json")
