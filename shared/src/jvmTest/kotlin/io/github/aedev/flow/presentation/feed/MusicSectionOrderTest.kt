package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.models.PlaylistItem
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.innertube.pages.ChartsPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The music page's order is the app's own session plan: three slots anchored by the brain's
 * maturity, every other slot shuffled by the session's seed, with the response's shelves kept in
 * their own order inside the slot the app gives them.
 */
class MusicSectionOrderTest {
    private fun shelf(title: String) = MusicSection(title = title, entries = emptyList())

    @Test
    fun aColdBrainLeadsWithChartsMoodsThenNewReleases() {
        val plan = musicSectionPlan(seed = 7L, maturity = "cold_start")

        assertEquals(
            listOf(MusicHomeSlot.CHARTS, MusicHomeSlot.MOODS_AND_GENRES, MusicHomeSlot.NEW_RELEASES),
            plan.take(3),
        )
        assertEquals(MusicHomeSlot.entries.size, plan.size)
        assertEquals(MusicHomeSlot.entries.toSet(), plan.toSet())
    }

    @Test
    fun aMatureBrainLeadsWithTaste() {
        val plan = musicSectionPlan(seed = 7L, maturity = "mature")

        assertEquals(
            listOf(MusicHomeSlot.QUICK_PICKS, MusicHomeSlot.SIMILAR_TO, MusicHomeSlot.FROM_COMMUNITY),
            plan.take(3),
        )
    }

    @Test
    fun anUnreportedBrainLeadsWithItsDefaultSlots() {
        val plan = musicSectionPlan(seed = 7L, maturity = null)

        assertEquals(
            listOf(MusicHomeSlot.QUICK_PICKS, MusicHomeSlot.FROM_COMMUNITY, MusicHomeSlot.DAILY_DISCOVER),
            plan.take(3),
        )
    }

    @Test
    fun oneSeedGivesOnePlanAndAnotherSeedMovesIt() {
        val plan = musicSectionPlan(seed = 7L, maturity = null)

        assertEquals(plan, musicSectionPlan(seed = 7L, maturity = null))
        assertTrue(
            (1L..20L).any { other -> musicSectionPlan(seed = other, maturity = null) != plan },
            "a session seed has to move the plan",
        )
    }

    @Test
    fun theResponseKeepsItsOwnOrderInsideItsSlot() {
        val response = listOf(shelf("Romance Right Now"), shelf("India's biggest hits"), shelf("Quick picks"))
        val planned =
            plannedMusicSections(
                response = response,
                newReleases = shelf("New releases"),
                communityPlaylists = shelf("Trending community playlists"),
                artistsForYou = null,
                moods = emptyList(),
                seed = 7L,
                maturity = null,
            )

        assertEquals(
            listOf("Romance Right Now", "India's biggest hits", "Quick picks"),
            planned.filter { section -> section in response }.map { it.title },
        )
        assertEquals(5, planned.size, "the response's shelves plus the two named ones")
        assertEquals("New releases", planned.first { it.title == "New releases" }.title)
    }

    @Test
    fun theNamedShelvesLeaveTheResponseBlockAndFollowThePlan() {
        val planned =
            plannedMusicSections(
                response = listOf(shelf("Quick picks"), shelf("New releases"), shelf("Trending community playlists")),
                newReleases = shelf("New releases"),
                communityPlaylists = shelf("Trending community playlists"),
                artistsForYou = shelf("Artists for you"),
                moods = listOf(shelf("Mood and Genres")),
                seed = 7L,
                maturity = null,
            )

        assertEquals("Artists for you", planned.first().title)
        assertEquals(
            listOf("Mood and Genres", "New releases", "Quick picks", "Trending community playlists"),
            planned.drop(1).map { it.title }.sorted(),
        )
        assertEquals(
            1,
            planned.count { it.title == "New releases" },
            "a named shelf must not also stay in the response block",
        )
    }

    @Test
    fun theMoodShelfSitsWhereThePlanPutsIt() {
        val moods = listOf(shelf("Mood and Genres"))
        val response = listOf(shelf("Quick picks"), shelf("India's biggest hits"))
        val seed = 11L
        val plan = musicSectionPlan(seed, maturity = null)
        val planned =
            plannedMusicSections(
                response = response,
                newReleases = null,
                communityPlaylists = null,
                artistsForYou = null,
                moods = moods,
                seed = seed,
                maturity = null,
            )

        val moodsAfterResponse =
            plan.indexOf(MusicHomeSlot.MOODS_AND_GENRES) > plan.indexOf(MusicHomeSlot.DYNAMIC_HOME)
        assertEquals(
            moodsAfterResponse,
            planned.last().title == "Mood and Genres",
            "the mood shelf follows the plan's slot for it",
        )
    }

    @Test
    fun theChartsShelvesAreDrawnInTheChartsSlot() {
        val charts =
            listOf(
                MusicSection(title = "Charts in United States", entries = listOf(entry("chart_playlist"))),
                MusicSection(
                    title = "Top artists in United States",
                    entries = listOf(entry("chart_artist")),
                    shape = MusicSectionShape.ArtistPortraits,
                ),
            )
        val planned =
            plannedMusicSections(
                response = emptyList(),
                newReleases = null,
                communityPlaylists = null,
                artistsForYou = null,
                moods = emptyList(),
                seed = 7L,
                maturity = "cold_start",
                charts = charts,
            )

        assertEquals(charts, planned.take(charts.size), "a cold plan stands the charts first")
    }

    @Test
    fun theChartAnswerDrawsRankedSongsFirstThenItsCards() {
        val songs =
            (1..25).map { rank ->
                SongItem(
                    id = "song_$rank",
                    title = "Song $rank",
                    artists = emptyList(),
                    thumbnail = "https://example.test/$rank.jpg",
                )
            }
        val page =
            ChartsPage(
                sections =
                    listOf(
                        ChartsPage.ChartSection("Trending", songs, ChartsPage.ChartType.SONGS),
                        ChartsPage.ChartSection(
                            "Charts in United States",
                            listOf(
                                PlaylistItem(
                                    id = "PLchart",
                                    title = "Top 100 Music Videos United States",
                                    author = null,
                                    songCountText = null,
                                    thumbnail = "https://example.test/pl.jpg",
                                    playEndpoint = null,
                                    shuffleEndpoint = null,
                                    radioEndpoint = null,
                                ),
                            ),
                            ChartsPage.ChartType.PLAYLISTS,
                        ),
                    ),
                countryCode = "US",
                continuation = null,
            )

        val sections = chartSections(page)

        assertEquals(listOf("Trending", "Charts in United States"), sections.map { it.title })
        assertEquals(MusicSectionShape.RankedLane, sections.first().shape)
        assertEquals(20, sections.first().entries.size, "the lane stops where the app stops")
        assertEquals(
            "song_1",
            sections
                .first()
                .entries
                .first()
                .id,
        )
    }

    @Test
    fun theChartHeadingsNameTheRegionTheyChart() {
        assertEquals("Charts in United States", chartsTitle("US"))
        assertEquals("Top artists in United States", topChartArtistsTitle("US"))
        assertEquals("Global charts", chartsTitle(null))
        assertEquals("Top artists worldwide", topChartArtistsTitle(null))
    }

    private fun entry(id: String) = FeedEntry(id = id, title = id, subtitle = null, videoId = null)
}
