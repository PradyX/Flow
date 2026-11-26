package io.github.aedev.flow.data.taste

import java.io.File

actual fun loadUserTaste(): UserTaste? = runCatching { UserTaste.fromJson(tasteFile().takeIf { it.isFile }?.readText()) }.getOrNull()

actual fun saveUserTaste(taste: UserTaste) {
    runCatching {
        val file = tasteFile()
        file.parentFile?.mkdirs()
        file.writeText(UserTaste.toJson(taste))
    }
}

actual fun loadHomeFeedback(): HomeFeedback? =
    runCatching { HomeFeedback.fromJson(feedbackFile().takeIf { it.isFile }?.readText()) }.getOrNull()

actual fun saveHomeFeedback(feedback: HomeFeedback) {
    runCatching {
        val file = feedbackFile()
        file.parentFile?.mkdirs()
        file.writeText(HomeFeedback.toJson(feedback))
    }
}

private fun tasteFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-taste.json")

private fun feedbackFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-feedback.json")
