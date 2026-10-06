package edu.gascnagercoil.kaalakolam.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import edu.gascnagercoil.kaalakolam.domain.EldersDomain
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TamilImeValueTest {
    @Test
    fun composingRegionSurvivesOrdinaryStateUpdate() {
        val value = TextFieldValue(
            text = "தமிழ்",
            selection = TextRange(5),
            composition = TextRange(0, 5),
        )
        val result = TextSafety.limitTextFieldValue(value, 100, Locale("ta", "IN"))
        assertEquals(value.text, result.text)
        assertEquals(value.selection, result.selection)
        assertEquals(value.composition, result.composition)
    }

    @Test
    fun elderStoryLimitDoesNotResetTamilComposingRegion() {
        val value = TextFieldValue(
            text = "தமிழ் நினைவு",
            selection = TextRange(11),
            composition = TextRange(6, 11),
        )
        val result = TextSafety.limitTextFieldValue(
            value,
            EldersDomain.MAX_STORY_GRAPHEMES,
            Locale("ta", "IN"),
        )
        assertEquals(value, result)
        assertEquals(value.composition, result.composition)
    }
}
