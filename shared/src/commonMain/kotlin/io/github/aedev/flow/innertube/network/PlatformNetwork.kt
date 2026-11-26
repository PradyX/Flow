package io.github.aedev.flow.innertube.network

import io.ktor.client.engine.HttpClientEngine

/**
 * A proxy in portable form. The Android actual maps it onto `java.net.Proxy`, which is the only
 * place the JVM type appears; `commonMain` never sees it.
 */
data class NetworkProxy(
    val host: String,
    val port: Int,
)

/** Everything the HTTP engine needs from the host platform, stated in common types. */
data class NetworkConfig(
    val proxy: NetworkProxy? = null,
    val proxyAuth: String? = null,
    /** Absolute path of the disk cache, or null for the platform's temp directory. */
    val cacheDirectory: String? = null,
)

/**
 * The platform HTTP engine.
 *
 * Android and the JVM use OkHttp with the exact tuning the shipping app used: a 15-slot connection
 * pool kept for 5 minutes, 15/30/30/45s connect/read/write/call timeouts, HTTP/2 preferred over
 * HTTP/1.1, connection-failure retries, a 48/8 dispatcher and a 50 MB disk cache. Android adds the
 * optional proxy the user configured. iOS uses Ktor's Darwin engine, which has no equivalent
 * connection-pool or dispatcher knobs; its proxy setting is not applied (see the iOS actual).
 */
internal expect fun createPlatformHttpEngine(config: NetworkConfig): HttpClientEngine

/**
 * The host's default locale, as the two fields the client actually uses.
 *
 * `java.util.Locale.getDefault()` has no common equivalent, so this is the seam. [languageTag] is a
 * BCP-47 tag such as `en-US`, exactly what `Locale.toLanguageTag()` returns.
 */
internal data class PlatformLocale(
    val languageTag: String,
    val country: String,
)

internal expect fun platformDefaultLocale(): PlatformLocale
