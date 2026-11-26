package io.github.aedev.flow.data.navigation

/**
 * The navigation stack the last run left, kept in the app's preferences so a relaunch reopens it
 * instead of dropping back to Home. The seam carries text: the stack is UI state, and the screens
 * decide what a stored route means when it no longer resolves.
 */
expect fun loadNavigationStateJson(): String?

expect fun saveNavigationStateJson(json: String)
