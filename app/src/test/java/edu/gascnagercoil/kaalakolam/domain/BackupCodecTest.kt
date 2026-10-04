package edu.gascnagercoil.kaalakolam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun roundTripPreservesRepairedState() {
        val original = AppState(
            language = "ta",
            themeMode = ThemeMode.DARK,
            learnedTopicIds = setOf("monsoon", "cyclone"),
            memoryFlags = mapOf("elder" to true, "council" to false),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(original)).getOrThrow()

        assertEquals(original.repair(), decoded)
    }

    @Test
    fun badChecksumIsRejected() {
        val valid = BackupCodec.encode(AppState())
        val corrupted = valid.dropLast(1) + if (valid.last() == '0') "1" else "0"

        assertTrue(BackupCodec.decode(corrupted).isFailure)
    }

    @Test
    fun wrongPrefixIsRejected() {
        val valid = BackupCodec.encode(AppState())
        assertTrue(BackupCodec.decode("XX1" + valid.drop(3)).isFailure)
    }

    @Test
    fun fnvKnownVectorMatches() {
        assertEquals(0x4f9f2cab, BackupCodec.fnv1a32("hello".encodeToByteArray()).toUInt().toLong())
    }
}
