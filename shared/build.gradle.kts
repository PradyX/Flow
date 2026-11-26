import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "io.github.aedev.flow.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        // The Android-KMP plugin disables resource processing by default;
        // Compose Multiplatform resources need it enabled.
        androidResources { enable = true }
    }

    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FlowShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            // Artwork: the same Coil the app uses, over Ktor so one ImageLoader serves every target.
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // The Innertube client: request building, URL param encoding and response decoding.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.encoding)
            // SHA-1 for the SAPISIDHASH header; multiplatform, and already on the classpath via OkHttp.
            implementation(libs.okio.core)
            // Room's KMP runtime and its bundled SQLite driver: the saved/downloaded library
            // survives a restart on every target, not just Android.
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // The player's fullscreen effect hides the system bars through the compat controller,
            // and routes the system back gesture to leaving fullscreen.
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            // The player surface: ExoPlayer plus the view that draws and drives it.
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.common)
            // media3-datasource also carries the cache data source the queue prefetches into.
            implementation(libs.androidx.media3.datasource)
            // The media session the notification, lock screen and hardware media keys drive.
            implementation(libs.androidx.media3.session)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        // The screens are rendered for real in jvmTest: Compose's own UI test API and the
        // desktop runtime that draws it offscreen.
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

// Room generates the database implementation per target, so the compiler is a KSP processor on
// each one rather than a plain dependency.
dependencies {
    add("kspJvm", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

compose.resources {
    packageOfResClass = "io.github.aedev.flow.resources"
}
