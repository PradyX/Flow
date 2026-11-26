package io.github.aedev.flow.presentation.onboarding

import androidx.compose.runtime.Immutable
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.onboarding_step_alerts
import io.github.aedev.flow.resources.onboarding_step_artists
import io.github.aedev.flow.resources.onboarding_step_channels
import io.github.aedev.flow.resources.onboarding_step_import
import io.github.aedev.flow.resources.onboarding_step_interests
import org.jetbrains.compose.resources.StringResource

/** How many interests the setup asks for before the feed can be personalised. */
internal const val MIN_TOPICS = 3

/** The wait between two interest sections appearing, so the cloud arrives in a wave. */
internal const val STAGGER_DELAY_MS = 50L

/** How long a keystroke waits before it becomes a search. */
internal const val SEARCH_DEBOUNCE_MS = 400L

/** A channel the setup can offer, as the search returns it. */
@Immutable
internal data class OnboardingChannel(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberCountText: String? = null,
)

/** An artist the setup can offer, as the music search returns it. */
@Immutable
internal data class OnboardingArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
)

/** The setup steps in order; Welcome and Ready bookend the five the progress bar counts. */
internal enum class OnboardingStep(
    val label: StringResource?,
) {
    Welcome(null),
    Interests(Res.string.onboarding_step_interests),
    Channels(Res.string.onboarding_step_channels),
    Artists(Res.string.onboarding_step_artists),
    Alerts(Res.string.onboarding_step_alerts),
    Import(Res.string.onboarding_step_import),
    Ready(null),
    ;

    val index: Int get() = ordinal

    val showsProgress: Boolean get() = label != null

    companion object {
        val progressSteps: List<OnboardingStep> = entries.filter { it.showsProgress }
    }
}

@Immutable
internal data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val topics: Set<String> = emptySet(),
    val query: String = "",
    val results: List<OnboardingChannel> = emptyList(),
    val searching: Boolean = false,
    val subscribed: List<OnboardingChannel> = emptyList(),
    val notifying: Set<String> = emptySet(),
    val artistQuery: String = "",
    val artistResults: List<OnboardingArtist> = emptyList(),
    val artistCandidates: List<OnboardingArtist> = emptyList(),
    val artistSearching: Boolean = false,
    val pickedArtists: List<OnboardingArtist> = emptyList(),
    val newVideoAlerts: Boolean = true,
    val importedSources: Set<ImportSource> = emptySet(),
    val importedCount: Int = 0,
) {
    val topicsLeft: Int get() = (MIN_TOPICS - topics.size).coerceAtLeast(0)

    val canAdvance: Boolean get() = step != OnboardingStep.Interests || topicsLeft == 0

    fun isSubscribed(channelId: String): Boolean = subscribed.any { it.id == channelId }

    fun isPicked(artistId: String): Boolean = pickedArtists.any { it.id == artistId }
}
