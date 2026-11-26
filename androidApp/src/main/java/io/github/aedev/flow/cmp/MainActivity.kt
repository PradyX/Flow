package io.github.aedev.flow.cmp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.aedev.flow.data.downloads.initializeDownloadsStorage
import io.github.aedev.flow.data.history.initializeWatchHistoryStorage
import io.github.aedev.flow.data.library.initializeLibraryStorage
import io.github.aedev.flow.data.local.initializeLocalMediaStorage
import io.github.aedev.flow.data.music.initializePlaylistSortStorage
import io.github.aedev.flow.data.navigation.initializeNavigationStateStorage
import io.github.aedev.flow.data.notes.initializeVideoNotesStorage
import io.github.aedev.flow.data.playback.initializePlaybackCheckpointStorage
import io.github.aedev.flow.data.playback.initializePlaybackSessionStorage
import io.github.aedev.flow.data.playlists.initializeUserPlaylistsStorage
import io.github.aedev.flow.data.search.initializeSearchSessionStorage
import io.github.aedev.flow.data.settings.initializeAppSettingsStorage
import io.github.aedev.flow.data.subscriptions.initializeArtistSubscriptionStorage
import io.github.aedev.flow.data.subscriptions.initializeSubscriptionStorage
import io.github.aedev.flow.data.taste.initializeTasteStorage
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.presentation.App
import io.github.aedev.flow.presentation.playback.initializeMediaCache
import io.github.aedev.flow.presentation.restoredAppRouter
import io.github.aedev.flow.presentation.ui.LocalShareAction

/** Single-activity host — all UI lives in shared commonMain. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeLibraryStorage(applicationContext)
        initializeTasteStorage(applicationContext)
        initializeSubscriptionStorage(applicationContext)
        initializeArtistSubscriptionStorage(applicationContext)
        initializePlaylistSortStorage(applicationContext)
        initializeSearchSessionStorage(applicationContext)
        initializeNavigationStateStorage(applicationContext)
        initializePlaybackSessionStorage(applicationContext)
        initializePlaybackCheckpointStorage(applicationContext)
        initializeWatchHistoryStorage(applicationContext)
        initializeLocalMediaStorage(applicationContext)
        initializeVideoNotesStorage(applicationContext)
        initializeUserPlaylistsStorage(applicationContext)
        initializeAppSettingsStorage(applicationContext)
        initializeMediaCache(applicationContext)
        initializeDownloadsStorage(applicationContext)
        restoreVisitor()
        setContent {
            // Reopens the stack the last run left, so a relaunch does not drop back to Home.
            val router = remember { restoredAppRouter() }
            // Android's back gesture walks the same stack the player's back control does.
            BackHandler(enabled = router.canGoBack) { router.pop() }
            // Setup runs once per install; the flag lives where the app keeps its own settings.
            var onboardingDone by remember { mutableStateOf(onboardingDone()) }
            CompositionLocalProvider(LocalShareAction provides ::shareVideo) {
                App(
                    router = router,
                    appVersion = versionName(),
                    onboardingDone = onboardingDone,
                    onOnboardingComplete = {
                        markOnboardingDone()
                        onboardingDone = true
                    },
                )
            }
        }
    }

    private fun onboardingDone(): Boolean = onboardingPrefs().getBoolean(KEY_ONBOARDING_DONE, false)

    /** The build the settings page reports. */
    private fun versionName(): String = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "3.0.0"

    /**
     * The visitor YouTube assigned on the first answer: sending it back is what keeps the music
     * home the same page across launches instead of a fresh anonymous one every time.
     */
    private fun restoreVisitor() {
        YouTube.visitorData = onboardingPrefs().getString(KEY_VISITOR, null)
        YouTube.onVisitorDataChanged = { visitor ->
            onboardingPrefs().edit().putString(KEY_VISITOR, visitor).apply()
        }
    }

    private fun markOnboardingDone() {
        onboardingPrefs().edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    private fun onboardingPrefs() = getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)

    /** The player's share action, handed to the platform chooser. */
    private fun shareVideo(
        title: String,
        url: String,
    ) {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, url)
            }
        startActivity(Intent.createChooser(send, title))
    }

    private companion object {
        const val PREFS_ONBOARDING = "flow_onboarding"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_VISITOR = "visitor_data"
    }
}
