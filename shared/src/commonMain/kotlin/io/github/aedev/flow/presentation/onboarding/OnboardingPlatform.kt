package io.github.aedev.flow.presentation.onboarding

import androidx.compose.runtime.Composable

/**
 * The parts of the setup that only a platform can answer: whether notifications are allowed,
 * whether background work is unrestricted, and a file picker for the imports. Android asks the
 * system; the desktop and iOS targets answer from their defaults until those patterns are ported.
 */
internal interface OnboardingPlatform {
    val notificationsAllowed: Boolean

    val backgroundUnrestricted: Boolean

    fun requestNotifications()

    fun openNotificationSettings()

    fun requestUnrestrictedBackground()

    /** Opens the system picker and answers with the chosen files' text. */
    fun pickFiles(
        mimeTypes: List<String>,
        several: Boolean,
        onPicked: (List<String>) -> Unit,
    )
}

@Composable
internal expect fun rememberOnboardingPlatform(): OnboardingPlatform
