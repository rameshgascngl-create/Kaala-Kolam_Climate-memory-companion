package edu.gascnagercoil.kaalakolam.data

import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ElderMode
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import edu.gascnagercoil.kaalakolam.domain.repair
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateSerializerTamilTest {
    @Test
    fun datastoreSerializerRoundTripPreservesTamilUserText() = runBlocking {
        val state = AppState(
            language = "ta",
            interviews = listOf(
                Interview(
                    id = "ivta01",
                    nickname = "பாட்டி",
                    birthDecade = 1,
                    place = 0,
                    answers = mapOf(
                        "heat" to InterviewAnswer(
                            answered = true,
                            rating = 2,
                            confidence = 3,
                            story = "கோடை வெப்பம் மிகவும் அதிகரித்தது.",
                        ),
                    ),
                    reaction = "கருவிப் பதிவை பார்த்ததும் ஆச்சரியப்பட்டார்.",
                    createdAt = 1_760_000_000_000L,
                ),
            ),
            elderSession = ElderSession(ElderMode.ASK, "ivta01", 3),
            elderAlias = "பாட்டி",
            notes = "வடகிழக்குப் பருவமழை நினைவுகள்",
            reflection = "மூத்தோர் அனுபவமும் அளவீடும் சேர்ந்து பார்க்கப்பட வேண்டும்.",
        )
        val bytes = ByteArrayOutputStream().also { AppStateSerializer.writeTo(state, it) }.toByteArray()
        val restored = AppStateSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(state.repair(), restored)
        assertEquals(state.notes.encodeToByteArray().toList(), restored.notes.encodeToByteArray().toList())
    }

    @Test
    fun corruptPersistedStateFallsBackSafely() = runBlocking {
        val restored = AppStateSerializer.readFrom(
            ByteArrayInputStream("{not-valid-json".encodeToByteArray()),
        )
        assertEquals(AppState(), restored)
    }
}
