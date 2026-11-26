package io.github.aedev.flow.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Injectable dispatchers — swap with TestDispatchers in unit tests. */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/** `Dispatchers.IO` is JVM/Android-only — iOS uses `Dispatchers.Default`. */
expect fun platformIODispatcher(): CoroutineDispatcher

class DefaultDispatcherProvider : DispatcherProvider {
    override val main = Dispatchers.Main
    override val io: CoroutineDispatcher get() = platformIODispatcher()
    override val default = Dispatchers.Default
    override val unconfined = Dispatchers.Unconfined
}
