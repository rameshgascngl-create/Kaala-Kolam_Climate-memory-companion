package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Exact SVG path data from the HTML prototype, converted to Compose ImageVectors.
 * The vectors use the prototype's 24 x 24 viewport and rounded 1.7/1.8 px strokes.
 */
object PrototypeIcons {
    private val paths = mapOf(
        "home" to "M3 11 12 4l9 7M5 10v9h14v-9",
        "learn" to "M4 5a2 2 0 012-2h13v16H6a2 2 0 00-2 2zM4 19V5M9 7h6",
        "elder" to "M4 5h16v11H9l-5 4z",
        "pool" to "M8 11a3 3 0 100-6 3 3 0 000 6zM16 11a3 3 0 100-6 3 3 0 000 6zM2 20c0-3 3-5 6-5s6 2 6 5M14 15c3 0 8 1 8 5",
        "council" to "M12 4v16M5 20h14M5 8h14M5 8l-3 7h6zM19 8l-3 7h6z",
        "predict" to "M12 3a9 9 0 100 18 9 9 0 000-18zM12 8a4 4 0 100 8 4 4 0 000-8z",
        "sun" to "M12 8a4 4 0 100 8 4 4 0 000-8zM12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9 7 7M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1",
        "water" to "M12 3C9 8 6 11 6 14a6 6 0 0012 0c0-3-3-6-6-11z",
        "cloud" to "M7 18a4 4 0 010-8 5 5 0 019.5-1A4.5 4.5 0 0117 18z",
        "wind" to "M3 8h11a3 3 0 10-3-3M3 12h16a3 3 0 11-3 3M3 16h8",
        "humid" to "M12 3C9 8 6 11 6 14a6 6 0 0012 0c0-3-3-6-6-11zM9 14a3 3 0 003 3",
        "bolt" to "M13 2 5 14h6l-1 8 8-12h-6z",
        "breeze" to "M2 17c2-2 4-2 6 0s4 2 6 0 4-2 6 0M3 9h8M9 5h6",
        "monsoon" to "M7 15a4 4 0 010-7 5 5 0 019.5-1A4 4 0 0117 15zM8 20l1-2M12 20l1-2M16 20l1-2",
        "cyclone" to "M12 12c0-2 2-3 3-2s1 4-2 5-6-1-6-5 4-7 8-6 7 5 6 9",
        "enso" to "M2 9c2-2 4-2 6 0s4 2 6 0 4-2 6 0M2 15c2-2 4-2 6 0s4 2 6 0 4-2 6 0M18 3l3 3-3 3",
        "heat" to "M10 4a2 2 0 014 0v9a4 4 0 11-4 0zM18 5h3M18 9h3M18 13h3",
        "flood" to "M3 14c2-2 4-2 6 0s4 2 6 0 4-2 6 0M3 19c2-2 4-2 6 0s4 2 6 0 4-2 6 0M7 11V6l5-3 5 3v5",
        "drought" to "M3 20h18M7 20l2-5-3-3M12 20l-1-6 3-4-2-4M17 20l-2-5 3-3",
        "tsunami" to "M2 18c4 0 5-10 11-10 4 0 4 5 1 5-2 0-2-3 0-3M2 21h20",
        "house" to "M3 11 12 4l9 7v9H3zM9 20v-6h6v6",
        "globe" to "M12 3a9 9 0 100 18 9 9 0 000-18zM3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18",
        "sea" to "M3 14c2-2 4-2 6 0s4 2 6 0 4-2 6 0M12 3v9M9 9l3 3 3-3M3 19h18",
        "coral" to "M12 21v-8M12 13l-4-4V5M12 13l4-4V4M12 17l-3-2M8 21h8",
        "ice" to "M12 2v20M3.3 7l17.4 10M3.3 17 20.7 7M9 4l3 2 3-2M9 20l3-2 3 2",
        "ozone" to "M12 3a9 9 0 100 18 9 9 0 000-18zM9 12h.01M12 12h.01M15 12h.01",
        "city" to "M4 21V8h6v13M10 21V3h6v18M16 21v-9h4v9M3 21h18",
        "leaf" to "M5 19C5 9 11 5 20 4c0 9-4 15-13 15zM5 19c3-5 6-8 11-11",
        "cal" to "M4 5h16v14H4zM4 10h16M9 10v9M15 10v9",
        "heart" to "M12 21s-7-4.4-7-10a4 4 0 017-2.6A4 4 0 0119 11c0 5.6-7 10-7 10z",
        "book" to "M4 5a2 2 0 012-2h13v16H6a2 2 0 00-2 2zM4 19V5M9 7h6",
        "spk" to "M4 9v6h4l5 4V5L8 9zM16 8a5 5 0 010 8M18.5 5.5a9 9 0 010 13",
        "play" to "M8 5v14l11-7z",
        "pause" to "M8 5v14M16 5v14",
        "prev" to "M6 5v14M19 5 9 12l10 7z",
        "next" to "M18 5v14M5 5l10 7-10 7z",
        "replay" to "M4 12a8 8 0 108-8M4 4v5h5",
    )

    private val cache = mutableMapOf<String, ImageVector>()

    fun get(name: String): ImageVector = cache.getOrPut(name) {
        val data = requireNotNull(paths[name]) { "Unknown prototype icon: $name" }
        ImageVector.Builder(
            name = "Prototype-$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = if (name in setOf("home", "learn", "elder", "pool", "council", "predict")) 1.8f else 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
    }
}
