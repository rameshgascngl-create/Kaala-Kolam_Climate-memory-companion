package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as AndroidDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationLabelLayoutTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 320,
            screenHeight = 568,
            xdpi = 160,
            ydpi = 160,
            density = AndroidDensity(160),
            locale = "ta",
            softButtons = false,
        ),
        showSystemUi = false,
    )

    @Test
    fun tamilLabelsAreExactAndFitAtTwoHundredPercentFontScale() {
        val labels = listOf("முகப்பு", "கற்க", "மூத்தோர்", "வகுப்பு", "ஊர்சபை", "கணிப்பு")
        val layouts = linkedMapOf<String, TextLayoutResult>()
        paparazzi.snapshot(name = "tamil-navigation-320-200") {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Row(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(6.dp),
                ) {
                    labels.forEach { label ->
                        Box(modifier = Modifier.weight(1f)) {
                            PrototypeTabLabel(
                                text = label,
                                color = Color.Black,
                                onTextLayout = { layouts[label] = it },
                            )
                        }
                    }
                }
            }
        }
        assertEquals(labels.toSet(), layouts.keys)
        labels.forEach { label ->
            val result = requireNotNull(layouts[label])
            assertEquals(label, result.layoutInput.text.text)
            assertFalse(label + " overflowed width", result.didOverflowWidth)
            assertTrue(label + " used " + result.lineCount + " lines", result.lineCount <= 2)
        }
    }
}
