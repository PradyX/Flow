package io.github.aedev.flow.data.navigation

import platform.Foundation.NSUserDefaults

actual fun loadNavigationStateJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY_NAVIGATION_STATE)

actual fun saveNavigationStateJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY_NAVIGATION_STATE)
}

private const val KEY_NAVIGATION_STATE = "flow_navigation_state"
