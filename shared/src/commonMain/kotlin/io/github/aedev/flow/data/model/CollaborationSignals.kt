package io.github.aedev.flow.data.model

internal fun Video.needsCollaboratorResolution(): Boolean =
    id.isNotBlank() &&
        collaborators.size < 2 &&
        (channelThumbnailUrls.size > 1 || channelName.hasLikelyCollaborationByline())

internal fun String.hasLikelyCollaborationByline(): Boolean {
    val normalized = " ${trim().lowercase()} "
    return normalized.contains(" & ") ||
        normalized.contains(" x ") ||
        normalized.contains(" feat. ") ||
        normalized.contains(" ft. ")
}
