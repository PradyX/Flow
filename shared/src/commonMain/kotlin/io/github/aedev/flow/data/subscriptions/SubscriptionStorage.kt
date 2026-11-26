package io.github.aedev.flow.data.subscriptions

/**
 * Where the subscription set lives between launches: beside the setup answers on Android, in
 * `~/.flow` on the desktop and in the user defaults on iOS.
 */
expect fun loadSubscriptions(): SubscriptionBook?

expect fun saveSubscriptions(book: SubscriptionBook)
