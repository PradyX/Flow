package io.github.aedev.flow.innertube.pages

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The seed token of a live stream's chat, read from the watch page the player already fetched:
 * `contents.twoColumnWatchNextResults.conversationBar.liveChatRenderer.continuations[0]`, whose
 * `reloadContinuationData` (or, on a replay, `invalidationContinuationData`) carries it. Null for
 * anything that is not a live stream, so the player never polls a chat that does not exist.
 */
fun JsonElement.liveChatSeedToken(): String? {
    val renderer =
        this
            .at("contents")
            ?.at("twoColumnWatchNextResults")
            ?.at("conversationBar")
            ?.at("liveChatRenderer")
            ?: return null
    val continuation =
        renderer["continuations"] as? JsonArray
            ?: return null
    return continuation
        .firstOrNull()
        ?.jsonObject
        ?.let { it["reloadContinuationData"]?.jsonObject ?: it["invalidationContinuationData"]?.jsonObject }
        ?.get("continuation")
        ?.jsonPrimitive
        ?.content
        ?.takeIf { it.isNotBlank() }
}

private fun JsonElement.at(key: String): JsonObject? = (this as? JsonObject)?.get(key)?.jsonObject
