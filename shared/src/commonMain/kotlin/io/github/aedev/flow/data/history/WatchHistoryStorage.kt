package io.github.aedev.flow.data.history

/**
 * Where the watch history is kept between runs. It is text so any host can write it, and a value the
 * codec no longer understands reads back as an empty history rather than an error.
 */
expect fun loadWatchHistoryJson(): String?

expect fun saveWatchHistoryJson(json: String)
