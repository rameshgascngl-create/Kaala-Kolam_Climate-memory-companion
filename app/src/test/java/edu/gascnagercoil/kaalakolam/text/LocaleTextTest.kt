package edu.gascnagercoil.kaalakolam.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleTextTest {
    @Test
    fun englishAcronymsInsideTamilReceiveEnglishLocaleSpans() {
        val value = LocaleText.annotateTamil("IMD மற்றும் IPCC AR6 மதிப்பீடு")
        assertEquals("IMD மற்றும் IPCC AR6 மதிப்பீடு", value.text)
        val spans = value.spanStyles
        assertEquals(3, spans.size)
        assertTrue(spans.all { it.item.localeList != null })
    }
}
