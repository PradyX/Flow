package io.github.aedev.flow.data.subscriptions

import platform.Foundation.NSUserDefaults

actual fun loadSubscriptions(): SubscriptionBook? =
    SubscriptionBook.fromJson(NSUserDefaults.standardUserDefaults.stringForKey(KEY_SUBSCRIPTIONS))

actual fun saveSubscriptions(book: SubscriptionBook) {
    NSUserDefaults.standardUserDefaults.setObject(SubscriptionBook.toJson(book), KEY_SUBSCRIPTIONS)
}

private const val KEY_SUBSCRIPTIONS = "flow_subscriptions"
