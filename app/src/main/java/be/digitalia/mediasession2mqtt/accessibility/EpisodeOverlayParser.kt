package be.digitalia.mediasession2mqtt.accessibility

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Last overlay episode, bound to the session title it was read for.
 * MainWorker drops it when the title no longer matches.
 */
object EpisodeOverlayState {
    val episode = MutableStateFlow("")

    @Volatile
    var sessionTitle: String = ""

    @Volatile
    var boundTitle: String = ""

    /**
     * Remember an episode for the title that was on screen.
     *
     * @param title Session title the overlay was checked against.
     * @param value `S#E#`, or empty to clear.
     */
    fun publish(title: String, value: String) {
        boundTitle = title
        episode.value = value
    }
}

/**
 * One `S#E#` from an allowlisted paused player. Anything else is empty.
 * A show page lists many episode numbers and has no timecode, so it does not match.
 */
object EpisodeOverlayParser {
    private val allowlist = setOf(
        "com.disney.disneyplus",
        "com.wbd.stream"
    )
    private val episodePattern = Regex("""S(\d+)\s*[: ]\s*E(\d+)""", RegexOption.IGNORE_CASE)
    private val seasonEpisodePattern = Regex("""Season\s+(\d+)\s+Episode\s+(\d+)""", RegexOption.IGNORE_CASE)
    private val timecodePattern = Regex("""\d{1,2}:\d{2}""")

    /**
     * @param packageName Active app.
     * @param windowText Accessibility window text.
     * @param sessionTitle Media session title. Must appear on screen.
     * @return `S#E#` or empty.
     */
    fun parse(packageName: String, windowText: String, sessionTitle: String): String {
        if (packageName !in allowlist) {
            return ""
        }
        if (sessionTitle.isBlank() || !windowText.contains(sessionTitle)) {
            return ""
        }
        if (!timecodePattern.containsMatchIn(windowText)) {
            return ""
        }
        val matches = buildList {
            addAll(episodePattern.findAll(windowText).map { "S${it.groupValues[1]}E${it.groupValues[2]}" })
            addAll(seasonEpisodePattern.findAll(windowText).map { "S${it.groupValues[1]}E${it.groupValues[2]}" })
        }.distinct()
        if (matches.size != 1) {
            return ""
        }
        return matches[0]
    }
}
