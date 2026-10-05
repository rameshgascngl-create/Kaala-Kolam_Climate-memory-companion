package edu.gascnagercoil.kaalakolam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictWorkflowTest {
    @Test
    fun webGoldenPredictionOrderAndInitialFormattingArePreserved() {
        assertEquals(
            listOf("ocean", "co2", "warm", "nogh", "vapour", "sea", "india", "nat"),
            M2Domain.predictionSpecs.map { it.id },
        )
        assertEquals(
            listOf("50%", "350 ppm", "0.5 °C", "0 °C", "3%", "5 cm", "0.3 °C", "0.5 °C"),
            M2Domain.predictionSpecs.map { M2Domain.formatPredictionValue(it, it.initial) },
        )
    }

    @Test
    fun nextQuestionFollowsPrototypeOrder() {
        assertEquals("ocean", M2Domain.nextPredictionSpec(emptySet())?.id)
        assertEquals("co2", M2Domain.nextPredictionSpec(setOf("ocean"))?.id)
        assertEquals(
            "nat",
            M2Domain.nextPredictionSpec(
                setOf("ocean", "co2", "warm", "nogh", "vapour", "sea", "india"),
            )?.id,
        )
        assertNull(M2Domain.nextPredictionSpec(M2Domain.predictionSpecs.map { it.id }.toSet()))
    }

    @Test
    fun guessesSnapToTheWebSliderStepAndHitRange() {
        val warm = requireNotNull(M2Domain.predictionSpec("warm"))
        assertEquals(1.1, M2Domain.snapPredictionGuess(warm, 1.06), 0.0)
        assertEquals("1.1 °C", M2Domain.formatPredictionValue(warm, 1.06))
        assertTrue(M2Domain.predictionHit(warm, 1.1))
        assertFalse(M2Domain.predictionHit(warm, 0.9))
    }

    @Test
    fun appStateRepairNormalisesStoredPredictionAnswers() {
        val repaired = AppState(
            predictionAnswers = mapOf(
                "warm" to PredictionState(1.06, 3, false),
                "unknown" to PredictionState(999.0, 2, true),
                "co2" to PredictionState(Double.NaN, 2, true),
            ),
        ).repair()

        assertEquals(setOf("warm"), repaired.predictionAnswers.keys)
        assertEquals(1.1, repaired.predictionAnswers.getValue("warm").guess, 0.0)
        assertTrue(repaired.predictionAnswers.getValue("warm").hit)
    }
}
