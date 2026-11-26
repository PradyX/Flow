package io.github.aedev.flow.data.local

/** What Flow does when playback reaches a SponsorBlock segment of a given category. */
enum class SponsorBlockAction {
    SKIP,
    SKIP_ONCE,
    SKIP_BUTTON,
    SHOW_TOAST,
    MUTE,
    IGNORE,
    DISABLED,
    ;

    companion object {
        fun fromString(name: String): SponsorBlockAction = entries.find { it.name == name } ?: SKIP
    }
}
