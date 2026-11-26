package io.github.aedev.flow.data.notes

/** Where the viewer's notes are kept between runs, as text any host can write. */
expect fun loadVideoNotesJson(): String?

expect fun saveVideoNotesJson(json: String)
