package edu.gascnagercoil.kaalakolam.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPronunciationTest {
    @Test
    fun allTamilSpeechTokensAreMappedBeforeNarration() {
        val tokens = listOf(
            "IPCC", "AR6", "SR1.5", "IMD", "MoES", "NOAA", "WMO", "UNEP",
            "ENSO", "ITCZ", "SPI", "SPEI", "ONI", "CO₂", "ppm", "°C", "km", "µm", "W/m²",
        )
        val input = tokens.joinToString(" ")
        val spoken = SpeechPronunciation.forTamil(input)
        tokens.forEach { token ->
            assertFalse("unmapped speech token " + token, spoken.contains(token))
        }
        assertFalse("Latin letters remain in Tamil spoken string: " + spoken, Regex("[A-Za-z]").containsMatchIn(spoken))
        assertTrue(Regex("[\u0B80-\u0BFF]").containsMatchIn(spoken))
    }
}
