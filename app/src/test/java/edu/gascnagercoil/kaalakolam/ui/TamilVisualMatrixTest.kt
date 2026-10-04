package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density as ComposeDensity
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.android.resources.Density
import com.android.resources.NightMode
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TamilVisualMatrixTest(
    private val width: Int,
    private val height: Int,
    private val fontScalePercent: Int,
    private val theme: String,
) {
    private val outputRoot = File("build/visual-tamil/${width}x$height/$fontScalePercent/$theme")

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenHeight = height,
            screenWidth = width,
            xdpi = 160,
            ydpi = 160,
            density = Density(160),
            locale = "ta",
            nightMode = if (theme == "dark") NightMode.NIGHT else NightMode.NOTNIGHT,
            softButtons = false,
        ),
        showSystemUi = false,
        snapshotHandler = FileSnapshotHandler(outputRoot),
    )

    @Test
    fun captureTamilLocalisationMatrix() {
        val mode = if (theme == "dark") ThemeMode.DARK else ThemeMode.LIGHT
        val appState = AppState(language = "ta", themeMode = mode)
        states.forEach { state ->
            paparazzi.snapshot(name = state) {
                CompositionLocalProvider(
                    LocalDensity provides ComposeDensity(1f, fontScalePercent / 100f),
                ) {
                    KaalaKolamTheme(mode) {
                        KaalaKolamVisualFixture(
                            uiState = AppUiState(appState = appState, gaps = null),
                            visualState = state,
                        )
                    }
                }
            }
        }
    }

    private class FileSnapshotHandler(private val outputRoot: File) : SnapshotHandler {
        override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler {
            val output = File(outputRoot, requireNotNull(snapshot.name) + ".png")
            return object : SnapshotHandler.FrameHandler {
                override fun handle(image: BufferedImage) {
                    output.parentFile?.mkdirs()
                    ImageIO.write(image, "png", output)
                }
                override fun close() = Unit
            }
        }
        override fun close() = Unit
    }

    companion object {
        private val states = listOf("home","learn","elders","class","council","predict","learn-topic","learn-deep","learn-game","learn-words")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}-{2}pct-{3}")
        fun parameters(): List<Array<Any>> = buildList {
            listOf(390 to 844, 320 to 568).forEach { (width,height) ->
                listOf(100,200).forEach { scale ->
                    listOf("dark","light").forEach { theme ->
                        add(arrayOf(width,height,scale,theme))
                    }
                }
            }
        }
    }
}
