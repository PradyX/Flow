package io.github.aedev.flow.innertube.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Desktop mirrors the Android engine so a desktop build behaves like the phone: same pool,
 * timeouts, protocols, retry policy, dispatcher and cache.
 */
internal actual fun createPlatformHttpEngine(config: NetworkConfig): HttpClientEngine =
    OkHttp.create {
        preconfigured =
            OkHttpClient
                .Builder()
                .connectionPool(ConnectionPool(15, 5, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .retryOnConnectionFailure(true)
                .dispatcher(
                    Dispatcher().apply {
                        maxRequests = 48
                        maxRequestsPerHost = 8
                    },
                ).cache(
                    Cache(
                        directory =
                            File(
                                config.cacheDirectory
                                    ?: File(System.getProperty("java.io.tmpdir"), "http_cache").absolutePath,
                            ),
                        50L * 1024L * 1024L,
                    ),
                ).apply {
                    config.proxy?.let { proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(it.host, it.port)) }
                    config.proxyAuth?.let { auth ->
                        proxyAuthenticator { _, response ->
                            response.request
                                .newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        }
                    }
                }.build()
    }

internal actual fun platformDefaultLocale(): PlatformLocale {
    val locale = Locale.getDefault()
    return PlatformLocale(languageTag = locale.toLanguageTag(), country = locale.country)
}
