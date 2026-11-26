package io.github.aedev.flow.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The desktop has no notification prompt or battery policy to ask about, and no picker yet. */
@Composable
internal actual fun rememberOnboardingPlatform(): OnboardingPlatform =
    remember {
        object : OnboardingPlatform {
            override val notificationsAllowed: Boolean = true

            override val backgroundUnrestricted: Boolean = true

            override fun requestNotifications() = Unit

            override fun openNotificationSettings() = Unit

            override fun requestUnrestrictedBackground() = Unit

            override fun pickFiles(
                mimeTypes: List<String>,
                several: Boolean,
                onPicked: (List<String>) -> Unit,
            ) = Unit
        }
    }
