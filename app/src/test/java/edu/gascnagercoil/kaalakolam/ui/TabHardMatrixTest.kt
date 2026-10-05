package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as AndroidDensity
import com.android.resources.NightMode
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Captures the accessibility-first persistent-tab contract independently of
 * the clipped 320 px Tamil web reference.
 *
 * The HARD gate consumes these metrics and requires:
 * - every visible tab label to have zero width/height overflow;
 * - every semantic touch target to be at least 48 x 48 dp;
 * - every visual tab box to remain inside the viewport;
 * at both 320/390 dp and 100%/200% font scale.
 */
@RunWith(Parameterized::class)
class TabHardMatrixTest(
    private val width: Int,
    private val height: Int,
    private val language: String,
    private val fontScale: Float,
) {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenHeight = height,
            screenWidth = width,
            xdpi = 160,
            ydpi = 160,
            density = AndroidDensity(160),
            locale = language,
            nightMode = NightMode.NIGHT,
            softButtons = false,
        ),
        showSystemUi = false,
    )

    @Serializable
    private data class BoxMetric(
        val tag: String,
        val leftDp: Float,
        val topDp: Float,
        val widthDp: Float,
        val heightDp: Float,
        val interactive: Boolean,
    )

    @Serializable
    private data class TextMetric(
        val tag: String,
        val kind: String,
        val text: String,
        val lineCount: Int,
        val didOverflowWidth: Boolean,
        val didOverflowHeight: Boolean,
    )

    @Serializable
    private data class Output(
        val viewport: String,
        val widthDp: Int,
        val language: String,
        val fontScalePercent: Int,
        val boxes: List<BoxMetric>,
        val texts: List<TextMetric>,
    )

    private class Recorder : FidelityRecorder {
        val boxes = linkedMapOf<String, BoxMetric>()
        val texts = linkedMapOf<String, TextMetric>()

        override fun recordBox(
            tag: String,
            leftDp: Float,
            topDp: Float,
            widthDp: Float,
            heightDp: Float,
            interactive: Boolean,
        ) {
            boxes[tag] = BoxMetric(tag, leftDp, topDp, widthDp, heightDp, interactive)
        }

        override fun recordText(
            tag: String,
            kind: String,
            text: String,
            lineCount: Int,
            didOverflowWidth: Boolean,
            didOverflowHeight: Boolean,
        ) {
            texts[tag] = TextMetric(
                tag = tag,
                kind = kind,
                text = text,
                lineCount = lineCount,
                didOverflowWidth = didOverflowWidth,
                didOverflowHeight = didOverflowHeight,
            )
        }
    }

    @Test
    fun captureTabHardMetrics() {
        val recorder = Recorder()
        val mode = ThemeMode.DARK
        paparazzi.snapshot(name = "tab-hard-$language-$width-${(fontScale * 100).toInt()}") {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                KaalaKolamTheme(mode, language = language) {
                    KaalaKolamVisualFixture(
                        uiState = AppUiState(
                            appState = AppState(language = language, themeMode = mode),
                            gaps = null,
                        ),
                        visualState = "home",
                        fidelityRecorder = recorder,
                    )
                }
            }
        }

        val scalePercent = (fontScale * 100).toInt()
        val output = Output(
            viewport = "$width" + "x" + "$height",
            widthDp = width,
            language = language,
            fontScalePercent = scalePercent,
            boxes = recorder.boxes.values.sortedBy { it.tag },
            texts = recorder.texts.values.sortedBy { it.tag },
        )
        val file = File(
            "build/fidelity/tab-hard/" + width + "x" + height + "/" + language + "/" + scalePercent + ".json",
        )
        file.parentFile.mkdirs()
        file.writeText(Json { prettyPrint = true }.encodeToString(output) + "\n")
        println(
            "TAB_HARD_METRICS width=$width language=$language fontScale=$scalePercent " +
                "boxes=${output.boxes.size} texts=${output.texts.size}",
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}-{2}-{3}")
        fun parameters(): List<Array<Any>> = buildList {
            listOf(390 to 844, 320 to 568).forEach { (width, height) ->
                listOf("en", "ta").forEach { language ->
                    listOf(1f, 2f).forEach { fontScale ->
                        add(arrayOf(width, height, language, fontScale))
                    }
                }
            }
        }
    }
}
