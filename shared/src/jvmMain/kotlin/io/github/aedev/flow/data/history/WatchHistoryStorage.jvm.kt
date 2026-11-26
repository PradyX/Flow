package io.github.aedev.flow.data.history

import java.io.File

actual fun loadWatchHistoryJson(): String? =
    runCatching { historyFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveWatchHistoryJson(json: String) {
    runCatching {
        val file = historyFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun historyFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-watch-history.json")
