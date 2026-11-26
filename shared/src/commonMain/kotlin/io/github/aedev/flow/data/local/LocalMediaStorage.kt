package io.github.aedev.flow.data.local

/** Where the record of imported files is kept between runs, as text any host can write. */
expect fun loadLocalMediaJson(): String?

expect fun saveLocalMediaJson(json: String)
