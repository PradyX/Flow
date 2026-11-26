package io.github.aedev.flow.data.settings

/** Where the app's settings live between runs, as text any host can write. */
expect fun loadAppSettingsJson(): String?

expect fun saveAppSettingsJson(json: String)
