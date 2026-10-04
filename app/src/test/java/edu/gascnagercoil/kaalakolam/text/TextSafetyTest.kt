package edu.gascnagercoil.kaalakolam.text

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSafetyTest {
    private val ta = Locale("ta", "IN")

    @Test
    fun prefixUsesBreakIteratorCharacterBoundary() {
        val value = "க்ஷ ஶ்ரீ ஸ்ரீ கொ கௌ நந்தை பூக்கள் குழந்தைகள்"
        val iterator = BreakIterator.getCharacterInstance(ta)
        iterator.setText(value)
        iterator.first()
        val expectedBoundary = iterator.next()
        assertEquals(value.subSequence(0, expectedBoundary).toString(), TextSafety.graphemePrefix(value, 1, ta))
    }

    @Test
    fun searchNormalisesNfc() {
        val decomposed = Normalizer.normalize("கொ", Normalizer.Form.NFD)
        assertTrue(TextSafety.normalisedContains("இன்று கொ மேகம்", decomposed))
    }

    @Test
    fun tamilSortUsesTamilCollatorAndPreservesValues() {
        val source = listOf("ழகு", "அகம்", "கடல்")
        val sorted = TextSafety.sortTamil(source)
        assertEquals(source.toSet(), sorted.toSet())
        assertEquals(source.size, sorted.size)
    }
}
