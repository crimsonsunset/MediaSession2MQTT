package be.digitalia.mediasession2mqtt.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Read one `S#E#` off an allowlisted paused player and hand it to [EpisodeOverlayState].
 * Account commands stay the write path. This only fills a miss.
 */
class EpisodeOverlayService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val packageName = event?.packageName?.toString().orEmpty()
        val title = EpisodeOverlayState.sessionTitle
        if (title.isBlank()) {
            return
        }
        val text = collectText(root)
        val episode = EpisodeOverlayParser.parse(packageName, text, title)
        if (episode.isNotEmpty()) {
            EpisodeOverlayState.publish(title, episode)
        }
    }

    override fun onInterrupt() = Unit

    /**
     * Flatten the accessibility tree to lines.
     *
     * @param node Window root.
     * @return Joined text.
     */
    private fun collectText(node: AccessibilityNodeInfo): String {
        val lines = ArrayList<String>()
        walk(node, lines)
        return lines.joinToString("\n")
    }

    /**
     * @param node Current node.
     * @param lines Accumulator.
     */
    private fun walk(node: AccessibilityNodeInfo, lines: ArrayList<String>) {
        val text = node.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            lines.add(text)
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            walk(child, lines)
        }
    }
}
