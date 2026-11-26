package io.github.aedev.flow.data.search

/**
 * The search page's own JSON, kept in the app's preferences so a relaunch draws the page the user
 * left instead of an empty one. The seam carries text rather than a typed page: the page is UI
 * state, and only restorable data crosses here.
 */
expect fun loadSearchSessionJson(): String?

expect fun saveSearchSessionJson(json: String)
