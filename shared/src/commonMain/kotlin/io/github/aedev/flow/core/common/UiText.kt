package io.github.aedev.flow.core.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Multiplatform UI strings. Domain/data layers emit [UiText]; only composables
 * resolve resources. Strings live in `commonMain/composeResources/values/`.
 */
sealed interface UiText {
    data class Dynamic(
        val value: String,
    ) : UiText

    data class Resource(
        val id: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    @Composable
    fun asString(): String =
        when (this) {
            is Dynamic -> {
                value
            }

            is Resource -> {
                if (args.isEmpty()) {
                    stringResource(id)
                } else {
                    stringResource(id, *args.toTypedArray())
                }
            }
        }
}
