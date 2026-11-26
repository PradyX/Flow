package io.github.aedev.flow.data.downloads

/** Where the record of downloaded videos is kept between runs, as text any host can write. */
expect fun loadDownloadsJson(): String?

expect fun saveDownloadsJson(json: String)
