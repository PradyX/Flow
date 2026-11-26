package io.github.aedev.flow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SponsorBlockSegment(
    @SerialName("category") val category: String,
    @SerialName("segment") val segment: List<Float>,
    @SerialName("UUID") val uuid: String,
    @SerialName("actionType") val actionType: String,
) {
    val startTime: Float get() = if (segment.isNotEmpty()) segment[0] else 0f

    val endTime: Float get() = if (segment.size > 1) segment[1] else 0f

    /** A highlight: one moment, as opposed to a range or a whole-video label at `[0, 0]`. */
    val isPoint: Boolean get() = category == SponsorBlockCategories.HIGHLIGHT
}
