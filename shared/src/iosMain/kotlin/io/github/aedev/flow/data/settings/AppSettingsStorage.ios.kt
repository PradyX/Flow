package io.github.aedev.flow.data.settings

import platform.Foundation.NSUserDefaults

actual fun loadAppSettingsJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveAppSettingsJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_app_settings"
