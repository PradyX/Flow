package io.github.aedev.flow.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ResponseContext(
    // Both are omitted by some clients' answers (VISIONOS ships no serviceTrackingParams), and a
    // required-but-nullable field fails the whole decode when the key is absent.
    val visitorData: String? = null,
    val serviceTrackingParams: List<ServiceTrackingParam>? = null,
) {
    @Serializable
    data class ServiceTrackingParam(
        val params: List<Param>,
        val service: String,
    ) {
        @Serializable
        data class Param(
            val key: String,
            val value: String,
        )
    }
}
