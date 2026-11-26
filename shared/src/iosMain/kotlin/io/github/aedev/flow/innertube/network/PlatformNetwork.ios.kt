package io.github.aedev.flow.innertube.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSLocale

/**
 * iOS uses Ktor's Darwin engine (NSURLSession). It has no connection-pool, dispatcher or protocol
 * knobs, so the OkHttp tuning has no counterpart here; the shared `HttpTimeout` and retry wrapper
 * still apply. `config.proxy` / `config.proxyAuth` are not applied: iOS has no equivalent to
 * per-session proxy authentication, and the app only offers a proxy on Android.
 */
internal actual fun createPlatformHttpEngine(config: NetworkConfig): HttpClientEngine = Darwin.create {}

internal actual fun platformDefaultLocale(): PlatformLocale {
    val locale = NSLocale.currentLocale
    val language = locale.languageCode ?: "en"
    val country = locale.countryCode ?: "US"
    val languageTag = if (country.isEmpty()) language else "$language-$country"
    return PlatformLocale(languageTag = languageTag, country = country)
}
