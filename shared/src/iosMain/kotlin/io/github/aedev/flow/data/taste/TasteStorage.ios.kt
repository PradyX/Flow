package io.github.aedev.flow.data.taste

import platform.Foundation.NSUserDefaults

actual fun loadUserTaste(): UserTaste? = UserTaste.fromJson(NSUserDefaults.standardUserDefaults.stringForKey(KEY_TASTE))

actual fun saveUserTaste(taste: UserTaste) {
    NSUserDefaults.standardUserDefaults.setObject(UserTaste.toJson(taste), KEY_TASTE)
}

actual fun loadHomeFeedback(): HomeFeedback? = HomeFeedback.fromJson(NSUserDefaults.standardUserDefaults.stringForKey(KEY_FEEDBACK))

actual fun saveHomeFeedback(feedback: HomeFeedback) {
    NSUserDefaults.standardUserDefaults.setObject(HomeFeedback.toJson(feedback), KEY_FEEDBACK)
}

private const val KEY_TASTE = "flow_user_taste"
private const val KEY_FEEDBACK = "flow_home_feedback"
