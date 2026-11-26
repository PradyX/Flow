// AGP 9 ships built-in Kotlin: no kotlin-android plugin. The compose
// compiler plugin hooks AGP's built-in Kotlin compilation.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.aedev.flow.cmp"
    compileSdk = 37

    defaultConfig {
        // Coexists with the shipping `:app` (io.github.aedev.flow) so both install side by side.
        applicationId = "io.github.aedev.flow.cmp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-cmp"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
