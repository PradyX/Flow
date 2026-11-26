package io.github.aedev.flow.presentation.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A back stack over [AppDestination]. Compose state, so any composable reading [current] recomposes
 * on navigation, but there is nothing Android- or Compose-only about the transitions themselves.
 *
 * The two tabs reset the stack ([openTab]); a detail screen is pushed on top of whichever tab opened
 * it ([push]), and [pop] returns to that tab.
 */
@Stable
class AppRouter(
    start: AppDestination = AppDestination.Home,
    /** Detail pages that were open over [start] when the last run ended, deepest last. */
    restoredDetail: List<AppDestination> = emptyList(),
) {
    var stack: List<AppDestination> by mutableStateOf(listOf(start) + restoredDetail)
        private set

    val current: AppDestination
        get() = stack.last()

    val canGoBack: Boolean
        get() = stack.size > 1

    val depth: Int
        get() = stack.size

    fun openTab(destination: AppDestination) {
        stack = listOf(destination)
    }

    fun push(destination: AppDestination) {
        if (stack.last() == destination) return
        stack = stack + destination
    }

    /** Swaps the top page for [destination]: the next track of the queue the player is on. */
    fun replaceTop(destination: AppDestination) {
        if (stack.isEmpty()) {
            stack = listOf(destination)
        } else {
            stack = stack.dropLast(1) + destination
        }
    }

    /** Returns false when there was nothing to go back to, so a host can fall through to its own exit. */
    fun pop(): Boolean {
        if (!canGoBack) return false
        stack = stack.dropLast(1)
        return true
    }

    /** Drops the detail pages a relaunch reopened, leaving the tab they were opened from. */
    fun dropToTabRoot() {
        if (canGoBack) stack = listOf(stack.first())
    }
}
