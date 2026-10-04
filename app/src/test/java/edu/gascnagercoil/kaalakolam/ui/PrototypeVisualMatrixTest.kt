package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.NightMode
import edu.gascnagercoil.kaalakolam.content.GapEntry
import edu.gascnagercoil.kaalakolam.content.GapManifest
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Cross-renderer visual contract capture.
 *
 * These JVM snapshots deliberately cover the same 72 states as tests/golden-web.
 * tools/compare_visuals.py, rather than Paparazzi's own native golden store,
 * compares these PNGs with the HTML references and writes the measured
 * PASS/FAIL matrix.
 */
@RunWith(Parameterized::class)
class PrototypeVisualMatrixTest(
    private val state: String,
    private val width: Int,
    private val height: Int,
    private val language: String,
    private val theme: String,
) {
    private val route: String = when (state) {
        "home" -> "home"
        "learn", "learn-topic", "learn-game", "learn-words" -> "learn"
        "elders" -> "elders"
        "class" -> "class"
        "council" -> "council"
        "predict" -> "predict"
        else -> error("Unknown state: $state")
    }

    private val output = File(
        "build/visual-native/$width" + "x$height/$language/$theme/$state.png",
    )

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenHeight = height,
            screenWidth = width,
            xdpi = 160,
            ydpi = 160,
            density = Density(160),
            locale = language,
            nightMode = if (theme == "dark") NightMode.NIGHT else NightMode.NOTNIGHT,
            softButtons = false,
        ),
        showSystemUi = false,
        onNewFrame = { image ->
            output.parentFile.mkdirs()
            ImageIO.write(image, "png", output)
        },
    )

    @Test
    fun capturePrototypeState() {
        val mode = if (theme == "dark") ThemeMode.DARK else ThemeMode.LIGHT
        paparazzi.snapshot(name = "$state-$width" + "x$height-$language-$theme") {
            KaalaKolamTheme(mode) {
                KaalaKolamApp(
                    uiState = AppUiState(
                        appState = AppState(language = language, themeMode = mode),
                        gaps = testGapManifest(),
                    ),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onLanguageChange = {},
                    onThemeChange = {},
                    onBackup = { "" },
                    onValidateBackup = { null },
                    onRestore = {},
                    onReset = {},
                    initialRoute = route,
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}x{2}-{3}-{4}")
        fun parameters(): List<Array<Any>> {
            val states = listOf(
                "home",
                "learn",
                "elders",
                "class",
                "council",
                "predict",
                "learn-topic",
                "learn-game",
                "learn-words",
            )
            val sizes = listOf(390 to 844, 320 to 568)
            val languages = listOf("en", "ta")
            val themes = listOf("dark", "light")
            return buildList {
                sizes.forEach { (width, height) ->
                    languages.forEach { language ->
                        themes.forEach { theme ->
                            states.forEach { state ->
                                add(arrayOf(state, width, height, language, theme))
                            }
                        }
                    }
                }
            }
        }

        private fun testGapManifest(): GapManifest {
            val entries = listOf(
                GapEntry(
                    kind = "deepDive",
                    logicalPath = "topics.sun.deepDive",
                    asset = "content/book_content.json",
                    pointer = "/topics/sun/deepDive",
                    en = "Deep-dive content remains under M2 visual-fidelity implementation.",
                ),
                GapEntry(
                    kind = "eldersCrossCheck",
                    logicalPath = "elders.sun",
                    asset = "content/book_content.json",
                    pointer = "/elders/sun",
                    en = "Cross-check content remains under M2 visual-fidelity implementation.",
                ),
                GapEntry(
                    kind = "councilDescription",
                    logicalPath = "council.flood",
                    asset = "content/book_content.json",
                    pointer = "/council/flood",
                    en = "Council content remains under M2 visual-fidelity implementation.",
                ),
                GapEntry(
                    kind = "predictExplanation",
                    logicalPath = "predict.rain",
                    asset = "content/book_content.json",
                    pointer = "/predict/rain",
                    en = "Prediction content remains under M2 visual-fidelity implementation.",
                ),
            )
            return GapManifest(
                schemaVersion = 1,
                status = "visual-test-fixture",
                uiLabel = "English only",
                count = entries.size,
                countsByKind = entries.groupingBy { it.kind }.eachCount(),
                gaps = entries,
            )
        }
    }
}
