package edu.gascnagercoil.kaalakolam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test
    fun repairCoercesInvalidLanguageAndSchema() {
        val repaired = AppState(
            schemaVersion = 99,
            language = "fr",
            learnedTopicIds = setOf("", "ok", "x".repeat(65)),
            memoryFlags = mapOf("" to true, "elder" to true),
        ).repair()

        assertEquals(AppState.CURRENT_SCHEMA, repaired.schemaVersion)
        assertEquals("en", repaired.language)
        assertEquals(setOf("ok"), repaired.learnedTopicIds)
        assertFalse("" in repaired.memoryFlags)
        assertTrue(repaired.memoryFlags["elder"] == true)
    }
}
