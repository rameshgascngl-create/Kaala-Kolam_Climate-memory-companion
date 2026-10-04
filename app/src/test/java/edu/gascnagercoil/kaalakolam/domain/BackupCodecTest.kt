package edu.gascnagercoil.kaalakolam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BackupCodecTest {
    @Test
    fun roundTripPreservesRepairedState() {
        val original = AppState(
            language = "ta",
            themeMode = ThemeMode.DARK,
            learnedTopicIds = setOf("monsoon", "cyclone"),
            memoryFlags = mapOf("elder" to true, "council" to false),
            elderAlias = "பாட்டி — கொளச்சல்",
            notes = "மழை மாறிவிட்டது; குளம் வற்றுகிறது.",
            reflection = "நினைவு + அளவீடு இரண்டும் தேவை.",
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(original)).getOrThrow()

        assertEquals(original.repair(), decoded)
    }

    @Test
    fun tamilFreeTextSurvivesUtf8Base64UrlRoundTrip() {
        val original = AppState(
            language = "ta",
            elderAlias = "அம்மம்மா",
            notes = "க்ஷ ஶ்ரீ ஸ்ரீ கொ கௌ நந்தை பூக்கள் குழந்தைகள்",
            reflection = "காலநிலை நினைவுகளைச் சான்றுடன் ஒப்பிட வேண்டும்.",
        )
        val code = BackupCodec.encode(original)
        val decoded = BackupCodec.decode(code).getOrThrow()
        assertEquals(original.repair(), decoded)
        assertTrue(code.startsWith("KB1."))
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
    fun fiveHundredDeterministicCorruptionsAreRejectedWithoutThrowing() {
        val valid = BackupCodec.encode(
            AppState(
                language = "ta",
                learnedTopicIds = setOf("monsoon", "cyclone"),
                memoryFlags = mapOf("elder" to true),
            ),
        )
        val random = Random(20261004)

        repeat(500) {
            val chars = valid.toCharArray()
            val index = random.nextInt(chars.size)
            chars[index] = if (chars[index] == 'A') 'B' else 'A'
            val result = BackupCodec.decode(chars.concatToString())
            assertTrue("corruption $it unexpectedly validated", result.isFailure)
        }
    }

    @Test
    fun oversizedInputIsRejected() {
        assertTrue(BackupCodec.decode("K".repeat(65_537)).isFailure)
    }

    @Test
    fun fnvKnownVectorMatches() {
        assertEquals(0x4f9f2cabL, BackupCodec.fnv1a32("hello".encodeToByteArray()).toUInt().toLong())
    }
}
