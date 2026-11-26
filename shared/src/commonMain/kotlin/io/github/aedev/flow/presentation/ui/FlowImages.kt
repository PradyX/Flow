package io.github.aedev.flow.presentation.ui

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.aedev.flow.innertube.network.NetworkConfig
import io.github.aedev.flow.innertube.network.createPlatformHttpEngine
import io.ktor.client.HttpClient

/**
 * The client artwork rides on, with the platform's own engine (OkHttp on Android and the desktop,
 * NSURLSession on iOS). It is separate from the InnerTube client because Coil keeps its own disk
 * cache above it, and it is shared by every image on screen rather than built per card.
 */
private val imageHttpClient: HttpClient by lazy {
    HttpClient(createPlatformHttpEngine(NetworkConfig()))
}

/**
 * Installs the app's single image loader. Without this Coil has no network fetcher on the shared
 * targets — the plain string URLs from InnerTube would decode as nothing.
 */
@Composable
internal fun InstallFlowImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { imageHttpClient }))
            }.build()
    }
}
