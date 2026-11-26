package io.github.aedev.flow.data.recommendation

import io.github.aedev.flow.data.model.Video
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The Home mixer: the app's FlowNeuroEngine rank and its smart-diversity pass, over the viewer
 * state a signed-out shared client can hold.
 *
 * The app reads a trained brain (topic interests, channel EMAs, time-of-day vectors, impressions,
 * rejection patterns, watch history). None of that exists in the port, so [HomeMixState] carries
 * the subset the port can know and every term the app computes from the rest is present here with
 * its cold-start value: the absence is explicit and the result is deterministic, not a skipped
 * mixer. The formulas, weights, thresholds and phase order are the app's own
 * (`NeuroScoring.scoreCandidate`, `NeuroScoring.applySmartDiversity`).
 */
internal object HomeScoring {
    // ── The app's weights and thresholds, unchanged ──
    const val SUBSCRIPTION_BOOST = 0.15
    const val SUBSCRIPTION_BOOST_MAX = 0.30
    const val COLD_START_THRESHOLD = 30
    const val NOVELTY_RELEVANCE_GATE = 0.08
    const val CLASSIC_VIEW_THRESHOLD = 5_000_000L
    const val TITLE_SIMILARITY_STRICT = 0.55
    const val TITLE_SIMILARITY_RELAXED = 0.60
    const val EXPLORATION_MIN_SCORE_RATIO = 0.30
    const val DIVERSITY_PHASE1_TARGET = 20

    /** `rank()`: tasteWeight 0.8 split with time context, which is 0 without time vectors. */
    const val TASTE_WEIGHT = 0.8
    const val NOVELTY_WEIGHT = 0.2

    /** Time-decay multipliers from the app's TimeDecay. */
    const val MULTIPLIER_LIVE = 1.15
    const val MULTIPLIER_FRESH = 1.15
    const val MULTIPLIER_DAYS = 1.12
    const val MULTIPLIER_WEEKS = 1.08

    /** Cold-start popularity term in `scoreCandidate`. */
    const val POPULARITY_WEIGHT = 0.05

    /**
     * The port's positive-feedback boost: the app's brain learns a channel EMA from likes, and that
     * input does not exist here, so an explicit "more like this" mark lifts the row's score instead.
     */
    const val LIKED_VIDEO_BOOST = 0.35
    const val LIKED_CHANNEL_BOOST = 0.20

    /** `applySmartDiversity`: per-topic caps and the exploration slots that go with them. */
    fun maxPerTopic(topicDiversity: Int): Int =
        when {
            topicDiversity <= 2 -> 6
            topicDiversity <= 4 -> 4
            else -> 3
        }

    fun explorationSlots(topicDiversity: Int): Int = if (topicDiversity <= 4) 2 else 1
}

/**
 * What the mixer reads. The port fills only what it has: nothing yet for a fresh install. Each
 * field names the app's brain field it stands in for, so wiring history in later is a data change,
 * not a formula change.
 */
data class HomeMixState(
    /** The app's `brain.interests`: topic term weights the viewer leans towards. */
    val interests: Map<String, Double> = emptyMap(),
    /** The app's `brain.channelScores`: channel EMA affinity, 0.5-neutral. */
    val channelScores: Map<String, Double> = emptyMap(),
    /** The app's `userSubs`. */
    val userSubs: Set<String> = emptySet(),
    /** Videos the viewer marked "more like this": lifted before diversity runs. */
    val boostedVideoIds: Set<String> = emptySet(),
    /** Channels the viewer marked "more like this". */
    val boostedChannels: Set<String> = emptySet(),
    /** The app's `brain.suppressedVideoIds`. */
    val suppressedVideoIds: Set<String> = emptySet(),
    /** The app's `brain.suppressedChannels` and `blockedChannels`. */
    val suppressedChannels: Set<String> = emptySet(),
    /** The app's `brain.blockedTopics`. */
    val blockedTopics: Set<String> = emptySet(),
    /** The app's `brain.feedHistory`: ids already put on screen, gated out of the next page. */
    val feedHistory: Set<String> = emptySet(),
    /** The app's `brain.totalInteractions`: the cold-start gate. */
    val totalInteractions: Int = 0,
) {
    val isColdStart: Boolean get() = totalInteractions < HomeScoring.COLD_START_THRESHOLD
}

/** One candidate with its score and its topic vector, as the diversity pass reads them. */
internal data class ScoredHomeVideo(
    val video: Video,
    val vector: Map<String, Double>,
    val score: Double,
    val index: Int,
)

/**
 * Runs [videos] through the app's rank: hard exclusions, the seen gate, the score formula, then the
 * smart-diversity phases. Order is score descending with the source position as the tie-break, so a
 * candidate list always ranks the same way.
 */
fun rankHomeVideos(
    videos: List<Video>,
    state: HomeMixState,
): List<Video> {
    if (videos.isEmpty()) return emptyList()
    val blocked =
        state.blockedTopics
            .map(::homeTokens)
            .flatten()
            .toSet()
    val filtered =
        videos.filter { video ->
            video.id !in state.suppressedVideoIds &&
                video.channelId !in state.suppressedChannels &&
                video.id !in state.feedHistory &&
                !homeTokens("${video.title} ${video.channelName}").any { it in blocked }
        }
    if (filtered.isEmpty()) return emptyList()

    val scored =
        filtered.mapIndexed { index, video ->
            val vector = homeTopicVector(video)
            ScoredHomeVideo(video, vector, scoreHomeVideo(video, vector, state) + likeBoost(video, state), index)
        }
    return diversify(scored)
}

/**
 * The positive-feedback lift, applied after the score so a mark actually raises the row in the
 * ranked output. Suppression is decided earlier in the pipeline, so a video both liked and not
 * interested never reaches this point.
 */
private fun likeBoost(
    video: Video,
    state: HomeMixState,
): Double {
    var boost = 0.0
    if (video.id in state.boostedVideoIds) boost += HomeScoring.LIKED_VIDEO_BOOST
    if (video.channelId in state.boostedChannels) boost += HomeScoring.LIKED_CHANNEL_BOOST
    return boost
}

/** The app's `scoreCandidate`, term by term, with the cold-start branch of every signal. */
internal fun scoreHomeVideo(
    video: Video,
    vector: Map<String, Double>,
    state: HomeMixState,
): Double {
    val personalityScore = cosine(state.interests, vector)
    // No time-of-day vectors in the port: `wContext` is 0 and the whole taste weight is personality,
    // exactly what rank() does when timeBucketConfidence is 0.
    val wPersonality = HomeScoring.TASTE_WEIGHT
    val contextScore = 0.0
    val wContext = 0.0
    val noveltyScore =
        if (state.isColdStart) {
            1.0 - personalityScore
        } else {
            smoothstep(HomeScoring.NOVELTY_RELEVANCE_GATE, HomeScoring.NOVELTY_RELEVANCE_GATE + 0.1, personalityScore) *
                (1.0 - personalityScore)
        }

    var total =
        (personalityScore * wPersonality) +
            (contextScore * wContext) +
            (noveltyScore * HomeScoring.NOVELTY_WEIGHT)

    total += channelSignal(video, state)
    if (state.isColdStart && video.viewCount > 0) {
        total += ln(1.0 + video.viewCount.toDouble()) / ln(10.0) / 10.0 * HomeScoring.POPULARITY_WEIGHT
    }
    total *= ageFactor(video, state)
    return total
}

/**
 * `calculateChannelSignal` without a learned channel EMA: the subscription boost with its freshness
 * amplifier, capped the way the app caps it. A channel the viewer never subscribed to earns nothing,
 * which is the neutral value the app's unlearned EMA starts from.
 */
private fun channelSignal(
    video: Video,
    state: HomeMixState,
): Double {
    if (video.channelId !in state.userSubs) return 0.0
    val freshness =
        when {
            isRecent(video.uploadDate, "hour") || isRecent(video.uploadDate, "minute") || isRecent(video.uploadDate, "second") -> 2.0
            isRecent(video.uploadDate, "day") && ageNumber(video.uploadDate) <= 2 -> 1.5
            isRecent(video.uploadDate, "week") && ageNumber(video.uploadDate) <= 1 -> 1.2
            else -> 1.0
        }
    return (HomeScoring.SUBSCRIPTION_BOOST * freshness).coerceAtMost(HomeScoring.SUBSCRIPTION_BOOST_MAX)
}

/** The app's `TimeDecay.calculateMultiplier`, with its classic/subscription smoothing. */
internal fun ageFactor(
    video: Video,
    state: HomeMixState,
): Double {
    val multiplier = uploadAgeMultiplier(video.uploadDate, video.isLive)
    val isClassic = video.viewCount >= HomeScoring.CLASSIC_VIEW_THRESHOLD
    val isSub = video.channelId in state.userSubs
    return if (isClassic || isSub) (multiplier + 1.0) / 2.0 else multiplier
}

internal fun uploadAgeMultiplier(
    dateText: String,
    isLive: Boolean,
): Double {
    if (isLive) return HomeScoring.MULTIPLIER_LIVE
    val text = dateText.lowercase()
    return when {
        text.contains("second") || text.contains("minute") || text.contains("hour") -> {
            HomeScoring.MULTIPLIER_FRESH
        }

        text.contains("day") -> {
            HomeScoring.MULTIPLIER_DAYS
        }

        text.contains("week") -> {
            HomeScoring.MULTIPLIER_WEEKS
        }

        text.contains("month") -> {
            val months = ageNumber(text).coerceAtLeast(1)
            max(1.0 / (1.0 + 0.08 * months), 0.75)
        }

        text.contains("year") -> {
            val years = ageNumber(text).coerceAtLeast(1)
            1.0 / (1.0 + 0.35 * years)
        }

        else -> {
            1.0
        }
    }
}

/**
 * `applySmartDiversity`: score order first, then the strict phase (a channel once, a topic up to its
 * cap, exploration slots for topics the viewer's top three do not cover), then the deferred
 * high-quality rows, then the relaxed fill. Titles are compared by Jaccard over their tokens, the
 * measure the app's tokenizer uses for the same gate.
 */
internal fun diversify(candidates: List<ScoredHomeVideo>): List<Video> {
    if (candidates.isEmpty()) return emptyList()
    val sorted = candidates.sortedWith(compareByDescending<ScoredHomeVideo> { it.score }.thenBy { it.index })
    val channelWindow = mutableListOf<String>()
    val topicWindow = mutableListOf<String>()
    val finalPlaylist = mutableListOf<Video>()
    val deferred = mutableListOf<ScoredHomeVideo>()

    val uniqueTopics = sorted.mapNotNull { primaryTopic(it.vector) }.distinct()
    val maxPerTopic = HomeScoring.maxPerTopic(uniqueTopics.size)
    val explorationSlots = HomeScoring.explorationSlots(uniqueTopics.size)
    val userTopTopics =
        sorted
            .flatMap { it.vector.entries }
            .groupBy { it.key }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .take(3)
            .map { it.key }
            .toSet()
    val topScore = sorted.firstOrNull()?.score ?: 0.0
    var explorationCount = 0

    val phase1 = sorted.toMutableList()
    val iterator = phase1.iterator()
    while (iterator.hasNext() && finalPlaylist.size < HomeScoring.DIVERSITY_PHASE1_TARGET) {
        val current = iterator.next()
        val topic = primaryTopic(current.vector).orEmpty()
        val channelCount = channelWindow.count { it == current.video.channelId }
        val topicCount = topicWindow.count { it == topic }
        val isTitleSimilar = closeTitle(finalPlaylist, current.video.title, HomeScoring.TITLE_SIMILARITY_STRICT)
        val isNovelTopic = topic.isNotEmpty() && topic !in userTopTopics
        val qualifiesForExploration =
            isNovelTopic &&
                explorationCount < explorationSlots &&
                topScore > 0 &&
                current.score >= topScore * HomeScoring.EXPLORATION_MIN_SCORE_RATIO
        val effectiveTopicCap = if (qualifiesForExploration) maxPerTopic + 1 else maxPerTopic

        if (channelCount == 0 && topicCount < effectiveTopicCap && !isTitleSimilar) {
            finalPlaylist += current.video
            channelWindow += current.video.channelId
            if (topic.isNotEmpty()) topicWindow += topic
            if (qualifiesForExploration) explorationCount++
            iterator.remove()
        } else if (topScore > 0 && current.score > topScore * 0.8) {
            deferred += current
            iterator.remove()
        }
    }

    val deferredSorted = deferred.sortedWith(compareByDescending<ScoredHomeVideo> { it.score }.thenBy { it.index })
    for (scored in deferredSorted) {
        val recentChannels = finalPlaylist.takeLast(7).map { it.channelId }
        val channelOk = recentChannels.count { it == scored.video.channelId } < 2
        val titleOk = !closeTitle(finalPlaylist, scored.video.title, HomeScoring.TITLE_SIMILARITY_RELAXED)
        if (channelOk && titleOk) finalPlaylist += scored.video
    }

    phase1.sortWith(compareByDescending<ScoredHomeVideo> { it.score }.thenBy { it.index })
    for (scored in phase1) {
        val recentChannels = finalPlaylist.takeLast(5).map { it.channelId }
        val channelSpam = recentChannels.count { it == scored.video.channelId } >= 2
        val titleSimilar = closeTitle(finalPlaylist, scored.video.title, HomeScoring.TITLE_SIMILARITY_RELAXED)
        if (!channelSpam && !titleSimilar) finalPlaylist += scored.video
    }
    return finalPlaylist
}

/** A video's topic vector: its title and channel terms, counting the title twice. */
internal fun homeTopicVector(video: Video): Map<String, Double> {
    val weights = mutableMapOf<String, Double>()
    homeTokens(video.title).forEach { weights.merge(it, 1.0, Double::plus) }
    homeTokens(video.channelName).forEach { weights.merge(it, 0.5, Double::plus) }
    video.tags.forEach { tag -> homeTokens(tag).forEach { weights.merge(it, 0.5, Double::plus) } }
    return weights
}

/** The topic a candidate is counted under: its strongest term. */
internal fun primaryTopic(vector: Map<String, Double>): String? =
    vector.entries
        .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key

internal fun cosine(
    a: Map<String, Double>,
    b: Map<String, Double>,
): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    var dot = 0.0
    for ((key, value) in a) {
        dot += value * (b[key] ?: 0.0)
    }
    val normA = sqrt(a.values.sumOf { it * it })
    val normB = sqrt(b.values.sumOf { it * it })
    return if (normA == 0.0 || normB == 0.0) 0.0 else dot / (normA * normB)
}

/** The app's `calculateTitleSimilarity`: the share of tokens the two titles have in common. */
internal fun titleSimilarity(
    a: String,
    b: String,
): Double {
    val left = homeTokens(a).toSet()
    val right = homeTokens(b).toSet()
    if (left.isEmpty() || right.isEmpty()) return 0.0
    val shared = left.intersect(right).size
    return shared.toDouble() / (left union right).size
}

private fun closeTitle(
    placed: List<Video>,
    title: String,
    threshold: Double,
): Boolean = placed.takeLast(5).any { existing -> titleSimilarity(existing.title, title) > threshold }

private fun homeTokens(text: String): List<String> =
    text
        .lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length > 1 && it !in HOME_STOPWORDS }

private fun smoothstep(
    edge0: Double,
    edge1: Double,
    x: Double,
): Double {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return t * t * (3 - 2 * t)
}

private fun ageNumber(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 1

private fun isRecent(
    text: String,
    unit: String,
): Boolean = text.lowercase().contains(unit)

/** Words that carry no topic and would otherwise make every title look alike. */
private val HOME_STOPWORDS =
    setOf(
        "the",
        "and",
        "for",
        "with",
        "you",
        "your",
        "this",
        "that",
        "from",
        "are",
        "was",
        "have",
        "has",
        "not",
        "all",
        "new",
        "how",
        "why",
        "what",
        "when",
        "official",
        "video",
        "music",
        "full",
        "song",
        "live",
        "vs",
        "ft",
        "feat",
    )
