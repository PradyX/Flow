package io.github.aedev.flow.data.taste

import io.github.aedev.flow.data.recommendation.HomeMixState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A channel the viewer chose during setup: the id Home ranks with, the name it draws. */
@Serializable
data class TasteChannel(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
)

/**
 * What setup learned about the viewer: the topics they picked, the channels they subscribed to and
 * the artists they chose. It is the port's stand-in for the app's brain, and the only viewer state
 * the shared client has to rank Home with.
 */
@Serializable
data class UserTaste(
    val topics: List<String> = emptyList(),
    val channels: List<TasteChannel> = emptyList(),
    val artists: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = topics.isEmpty() && channels.isEmpty() && artists.isEmpty()

    /** The topics as the mixer's interest vector: every pick weighs the same, as a cold-start brain. */
    fun toMixState(): HomeMixState =
        HomeMixState(
            interests = topics.distinct().associateWith { 1.0 },
            userSubs = channels.map { it.id }.filter { it.isNotBlank() }.toSet(),
        )

    /** The searches the chips run: each picked topic in turn, and a discovery variant of each. */
    fun chipQueries(): ChipQueries {
        val picks = topics.distinct().filter { it.isNotBlank() }
        return ChipQueries(
            interests = picks.take(QUERIES_PER_CHIP),
            exploration = picks.map { topic -> "$topic new" }.take(QUERIES_PER_CHIP),
            live = picks.take(2),
        )
    }

    companion object {
        const val QUERIES_PER_CHIP = 3

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): UserTaste? =
            text
                ?.takeIf {
                    it.isNotBlank()
                }?.let { runCatching { json.decodeFromString<UserTaste>(it) }.getOrNull() }

        fun toJson(taste: UserTaste): String = json.encodeToString(taste)
    }
}

/** The queries New to you, Recently uploaded and Live search with. */
data class ChipQueries(
    val interests: List<String> = emptyList(),
    val exploration: List<String> = emptyList(),
    val live: List<String> = emptyList(),
)
