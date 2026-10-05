package edu.gascnagercoil.kaalakolam.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
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

@RunWith(Parameterized::class)
class FidelityMetricMatrixTest(
    private val width: Int,
    private val height: Int,
    private val language: String,
    private val state: String,
) {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenHeight = height,
            screenWidth = width,
            xdpi = 160,
            ydpi = 160,
            density = Density(160),
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
        val state: String,
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
                tag, kind, text, lineCount, didOverflowWidth, didOverflowHeight,
            )
        }
    }

    @Test
    fun captureNativeFidelityMetrics() {
        val recorder = Recorder()
        val mode = ThemeMode.DARK
        paparazzi.snapshot(name = "fidelity-$state") {
            KaalaKolamTheme(mode, language = language) {
                KaalaKolamVisualFixture(
                    uiState = AppUiState(
                        appState = AppState(language = language, themeMode = mode),
                        gaps = null,
                    ),
                    visualState = state,
                    fidelityRecorder = recorder,
                )
            }
        }

        val output = Output(
            viewport = "${width}x$height",
            widthDp = width,
            language = language,
            state = state,
            boxes = recorder.boxes.values.sortedBy { it.tag },
            texts = recorder.texts.values.sortedBy { it.tag },
        )
        val file = File("build/fidelity/native-layout/${width}x$height/$language/$state.json")
        file.parentFile.mkdirs()
        file.writeText(Json { prettyPrint = true }.encodeToString(output) + "\n")
        println(
            "NATIVE_FIDELITY_METRICS state=$state lang=$language width=$width " +
                "boxes=${output.boxes.size} texts=${output.texts.size}",
        )
    }

    companion object {
        private val states = listOf("home", "learn", "elders", "class", "council", "predict", "learn-topic", "learn-game", "learn-words")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}-{2}-{3}")
        fun parameters(): List<Array<Any>> = buildList {
            listOf(390 to 844, 320 to 568).forEach { (width, height) ->
                listOf("en", "ta").forEach { language ->
                    states.forEach { state ->
                        add(arrayOf(width, height, language, state))
                    }
                }
            }
        }
    }
}
