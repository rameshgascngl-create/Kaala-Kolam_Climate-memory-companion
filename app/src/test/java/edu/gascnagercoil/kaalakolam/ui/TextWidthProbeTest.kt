package edu.gascnagercoil.kaalakolam.ui

import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import edu.gascnagercoil.kaalakolam.R
import java.io.File
import java.util.Locale
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class TextWidthProbeTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 390,
            screenHeight = 844,
            xdpi = 160,
            ydpi = 160,
            density = Density(160),
            locale = "en",
            softButtons = false,
        ),
        showSystemUi = false,
    )

    private data class Sample(
        val id: String,
        val lang: String,
        val text: String,
        val sizePx: Float,
        val fontRes: Int,
        val resolvedFamily: String,
        val resourceName: String,
    )

    @Test
    fun measurePinnedFontsWithAndroidPaint() {
        val samples = listOf(
            Sample(
                id = "en-display",
                lang = "en",
                text = "Weather or climate?",
                sizePx = 30.4f,
                fontRes = R.font.noto_serif_600_subset,
                resolvedFamily = "Noto Serif",
                resourceName = "noto_serif_600_subset",
            ),
            Sample(
                id = "en-body",
                lang = "en",
                text = "It rained heavily in my town last night.",
                sizePx = 16f,
                fontRes = R.font.noto_sans_400_subset,
                resolvedFamily = "Noto Sans",
                resourceName = "noto_sans_400_subset",
            ),
            Sample(
                id = "ta-display",
                lang = "ta",
                text = "வானிலையா? காலநிலையா?",
                sizePx = 30.4f,
                fontRes = R.font.noto_serif_tamil_600_subset,
                resolvedFamily = "Noto Serif Tamil",
                resourceName = "noto_serif_tamil_600_subset",
            ),
            Sample(
                id = "ta-body",
                lang = "ta",
                text = "நேற்றிரவு என் ஊரில் கனமழை பெய்தது.",
                sizePx = 16f,
                fontRes = R.font.noto_sans_tamil_400_subset,
                resolvedFamily = "Noto Sans Tamil",
                resourceName = "noto_sans_tamil_400_subset",
            ),
        )

        val measured = samples.map { sample ->
            val typeface = ResourcesCompat.getFont(paparazzi.context, sample.fontRes)
            assertNotNull("Missing pinned font ${sample.resourceName}", typeface)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                this.typeface = requireNotNull(typeface)
                textSize = sample.sizePx
                letterSpacing = 0f
                textLocale = Locale.forLanguageTag(sample.lang)
                isLinearText = true
                hinting = Paint.HINTING_OFF
            }
            val width = paint.measureText(sample.text)
            println(
                "ANDROID_TEXT_WIDTH sample=${sample.id} width=$width " +
                    "family=${sample.resolvedFamily} resource=${sample.resourceName}",
            )
            sample to width
        }

        val output = File("build/fidelity/android-text-widths.json")
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                append("{\n  \"samples\": [\n")
                measured.forEachIndexed { index, (sample, width) ->
                    append(
                        """    {"id":"${sample.id}","widthPx":${"%.6f".format(Locale.US, width)},"resolvedFamily":"${sample.resolvedFamily}","resourceName":"${sample.resourceName}"}"""
                    )
                    if (index != measured.lastIndex) append(",")
                    append("\n")
                }
                append("  ]\n}\n")
            },
        )
        println("ANDROID_TEXT_WIDTH_PASS samples=${measured.size} output=${output.path}")
    }
}
