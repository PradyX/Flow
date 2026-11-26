package io.github.aedev.flow.presentation.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Nothing to share with, on a host that has not provided a share affordance. */
private val NoShare: (title: String, url: String) -> Unit = { _, _ -> }

/**
 * Hands a title and a link to the platform's share affordance. The hosts provide it — Android's
 * chooser, the desktop browser — because sharing is the one thing here that has no common API.
 */
val LocalShareAction = staticCompositionLocalOf { NoShare }
