package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as AndroidDensity
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class TamilTypographyStressTest {
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
    fun tamilCombiningGlyphStressDoesNotClipAtTwoHundredPercent() {
        var layout: TextLayoutResult? = null
        val stress = "க்ஷ ஶ்ரீ ஸ்ரீ கொ கௌ நந்தை பூக்கள் குழந்தைகள்"
        paparazzi.snapshot(name = "tamil-glyph-stress-200") {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                KaalaKolamTheme(ThemeMode.LIGHT) {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = stress,
                            style = MaterialTheme.typography.bodyLarge,
                            onTextLayout = { layout = it },
                        )
                    }
                }
            }
        }
        assertNotNull(layout)
        assertFalse("Tamil stress string overflowed width", requireNotNull(layout).didOverflowWidth)
        assertFalse("Tamil stress string overflowed height", requireNotNull(layout).didOverflowHeight)
    }
}
