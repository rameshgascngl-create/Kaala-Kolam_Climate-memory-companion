package edu.gascnagercoil.kaalakolam.domain

import java.io.File
import java.util.Base64
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EldersSliceCTest {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private fun fixture(name: String): AppState {
        val candidates = listOf(
            File("../tests/elders/$name.json"),
            File("tests/elders/$name.json"),
        )
        val file = requireNotNull(candidates.firstOrNull(File::isFile)) {
            "Elders fixture not found: $name; user.dir=" + System.getProperty("user.dir")
        }
        return json.decodeFromString<AppState>(file.readText(Charsets.UTF_8)).repair()
    }

    @Test
    fun resumeFixturesPreserveQuestionsOneFiveAndTen() {
        val q1 = fixture("answer-q1")
        val q5 = fixture("answer-q5")
        val q10 = fixture("answer-q10")

        assertEquals(0, q1.elderSession.questionIndex)
        assertEquals(1, EldersDomain.answeredCount(q1.interviews.single()))

        assertEquals(4, q5.elderSession.questionIndex)
        assertEquals(5, EldersDomain.answeredCount(q5.interviews.single()))

        assertEquals(9, q10.elderSession.questionIndex)
        assertEquals(10, EldersDomain.answeredCount(q10.interviews.single()))
    }

    @Test
    fun allCannotSayAndZeroAnswerFixturesRemainDistinct() {
        val cannotSay = fixture("all-cannot-say").interviews.single()
        val zero = fixture("zero-answers").interviews.single()

        assertEquals(10, EldersDomain.answeredCount(cannotSay))
        assertTrue(cannotSay.answers.values.all { it.answered && it.rating == null })
        assertEquals(0, EldersDomain.answeredCount(zero))
        assertTrue(zero.answers.isEmpty())
        assertEquals(9, fixture("zero-answers").elderSession.questionIndex)
    }

    @Test
    fun overlongBackupStoryIsTrimmedOnBreakIteratorBoundary() {
        val locale = Locale("ta", "IN")
        val cluster = "கி"
        val overlong = cluster.repeat(EldersDomain.MAX_STORY_GRAPHEMES + 1)
        assertEquals(
            EldersDomain.MAX_STORY_GRAPHEMES + 1,
            EldersDomain.graphemeCount(overlong, locale),
        )

        val rawState = AppState(
            interviews = listOf(
                Interview(
                    id = "ivbackup",
                    nickname = "பாட்டி",
                    birthDecade = 1,
                    place = 0,
                    answers = mapOf(
                        "heat" to InterviewAnswer(
                            answered = true,
                            rating = 1,
                            confidence = 2,
                            story = overlong,
                        ),
                    ),
                ),
            ),
        )
        val payload = json.encodeToString(rawState).encodeToByteArray()
        val body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
        val checksum = BackupCodec.fnv1a32(payload).toUInt().toString(16).padStart(8, '0')
        val decoded = BackupCodec.decode("KB1.$body.$checksum").getOrThrow()
        val story = decoded.interviews.single().answers.getValue("heat").story

        assertEquals(EldersDomain.MAX_STORY_GRAPHEMES, EldersDomain.graphemeCount(story, locale))
        assertEquals(cluster.repeat(EldersDomain.MAX_STORY_GRAPHEMES), story)
    }

    @Test
    fun cannotSayDoesNotInventConfidence() {
        val answer = fixture("all-cannot-say").interviews.single().answers.getValue("heat")
        assertNull(answer.confidence)
    }
}
