package io.github.aedev.flow.data.search

import java.io.File

actual fun loadSearchSessionJson(): String? = runCatching { searchSessionFile().takeIf { it.isFile }?.readText() }.getOrNull()

actual fun saveSearchSessionJson(json: String) {
    runCatching {
        val file = searchSessionFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

actual fun loadSearchHistoryJson(): String? = runCatching { searchHistoryFile().takeIf { it.isFile }?.readText() }.getOrNull()

actual fun saveSearchHistoryJson(json: String) {
    runCatching {
        val file = searchHistoryFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun searchSessionFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-search-session.json")

private fun searchHistoryFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-search-history.json")
