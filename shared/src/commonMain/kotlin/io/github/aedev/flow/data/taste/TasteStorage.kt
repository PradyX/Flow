package io.github.aedev.flow.data.taste

/**
 * Where the setup's answers live between launches. Android keeps them in the app's own preferences,
 * the desktop in `~/.flow`, iOS in the user defaults; [UserTaste.toMixState] is what Home ranks with.
 */
expect fun loadUserTaste(): UserTaste?

expect fun saveUserTaste(taste: UserTaste)

/** Home's negative feedback, beside the taste and through the same platform stores. */
expect fun loadHomeFeedback(): HomeFeedback?

expect fun saveHomeFeedback(feedback: HomeFeedback)
