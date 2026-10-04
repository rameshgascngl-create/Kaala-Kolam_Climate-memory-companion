package edu.gascnagercoil.kaalakolam.data

import edu.gascnagercoil.kaalakolam.domain.AppState
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
            elderAlias = "பாட்டி",
            notes = "வடகிழக்குப் பருவமழை நினைவுகள்",
            reflection = "மூத்தோர் அனுபவமும் அளவீடும் சேர்ந்து பார்க்கப்பட வேண்டும்.",
        )
        val bytes = ByteArrayOutputStream().also { AppStateSerializer.writeTo(state, it) }.toByteArray()
        val restored = AppStateSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(state.repair(), restored)
        assertEquals(state.notes.encodeToByteArray().toList(), restored.notes.encodeToByteArray().toList())
    }
}
