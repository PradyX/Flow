package io.github.aedev.flow.core.common

/** Standard result wrapper for repository / use-case calls. */
sealed interface Result<out T> {
    data class Success<T>(
        val data: T,
    ) : Result<T>

    data class Error(
        val message: UiText,
        val throwable: Throwable? = null,
    ) : Result<Nothing>

    data object Loading : Result<Nothing>
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (UiText) -> Unit): Result<T> {
    if (this is Result.Error) action(message)
    return this
}
