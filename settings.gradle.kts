pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Flow"
include(":app")
include(":benchmark")

// Compose Multiplatform port (`cmp` branch): shared UI + the per-platform shells.
include(":shared")
include(":androidApp")
include(":desktopApp")
