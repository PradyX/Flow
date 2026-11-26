package io.github.aedev.flow.data.subscriptions

import java.io.File

actual fun loadSubscriptions(): SubscriptionBook? =
    runCatching { SubscriptionBook.fromJson(subscriptionsFile().takeIf { it.isFile }?.readText()) }.getOrNull()

actual fun saveSubscriptions(book: SubscriptionBook) {
    runCatching {
        val file = subscriptionsFile()
        file.parentFile?.mkdirs()
        file.writeText(SubscriptionBook.toJson(book))
    }
}

private fun subscriptionsFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-subscriptions.json")
