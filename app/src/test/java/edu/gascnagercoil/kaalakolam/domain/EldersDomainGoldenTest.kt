package edu.gascnagercoil.kaalakolam.domain

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EldersDomainGoldenTest {
    private val json = Json { ignoreUnknownKeys = false }

    private fun goldenRoot() = json.parseToJsonElement(goldenFile().readText()).jsonObject

    private fun goldenFile(): File {
        val candidates = listOf(
            File("../tests/m2/web-golden.json"),
            File("tests/m2/web-golden.json"),
        )
        return requireNotNull(candidates.firstOrNull(File::isFile)) {
            "tests/m2/web-golden.json not found; user.dir=" + System.getProperty("user.dir")
        }
    }

    private fun goldenInterview(): Pair<M2Domain.InterviewRecord, String> {
        val node = goldenRoot().getValue("shareCodes").jsonObject
            .getValue("interview").jsonObject
        val record = json.decodeFromJsonElement(
            M2Domain.InterviewRecord.serializer(),
            node.getValue("record"),
        )
        return record to node.getValue("code").jsonPrimitive.content
    }

    @Test
    fun ivRecordAndKk1ExportMatchWebGoldenExactly() {
        val (record, code) = goldenInterview()
        val answers = EldersDomain.questionIds.zip(record.a.toList()).associate { (id, encoded) ->
            id to if (encoded == '0') {
                InterviewAnswer(answered = true, rating = null)
            } else {
                InterviewAnswer(
                    answered = true,
                    rating = encoded.digitToInt() - 3,
                    confidence = 2,
                    story = "private story",
                )
            }
        }
        val interview = Interview(
            id = record.i,
            nickname = "Paati",
            birthDecade = record.b,
            place = record.p,
            answers = answers,
            reaction = "private reaction",
            createdAt = 1L,
        )

        assertEquals(record, EldersDomain.ivRecord(interview))
        assertEquals(code, EldersDomain.exportShareCode(interview))
        assertEquals(record, EldersDomain.importShareCode(code))
    }

    @Test
    fun shareCodeNeverContainsNicknameStoriesReactionNotesOrReflection() {
        val answersA = mapOf(
            "heat" to InterviewAnswer(true, 2, 3, "story A"),
            "nights" to InterviewAnswer(true, null, null, ""),
        )
        val answersB = mapOf(
            "heat" to InterviewAnswer(true, 2, 1, "story B that must stay private"),
            "nights" to InterviewAnswer(true, null, null, ""),
        )
        val a = Interview("ivprivacy", "Nickname A", 1, 0, answersA, "reaction A", 1L)
        val b = Interview("ivprivacy", "Nickname B", 1, 0, answersB, "reaction B", 2L)
        val privateState = AppState(
            interviews = listOf(a),
            notes = "private notes",
            reflection = "private reflection",
        )

        assertEquals(EldersDomain.exportShareCode(a), EldersDomain.exportShareCode(b))
        assertTrue(privateState.notes.isNotBlank())
        assertTrue(privateState.reflection.isNotBlank())
        assertEquals("I", EldersDomain.ivRecord(a).t)
    }

    @Test
    fun validationPreservesFiftyInterviewsAndRejectsInvalidRecords() {
        val valid = (0 until 50).map { index ->
            Interview(
                id = "iv" + index.toString().padStart(4, '0'),
                nickname = "N".repeat(EldersDomain.MAX_NICKNAME_CHARS),
                birthDecade = index % 6,
                place = index % 5,
                createdAt = index.toLong(),
            )
        }
        val invalid = listOf(
            Interview(
                id = "too-long-interview-id",
                nickname = "bad",
                birthDecade = 9,
                place = 9,
            ),
            Interview(
                id = "ivover",
                nickname = "N".repeat(EldersDomain.MAX_NICKNAME_CHARS + 1),
                birthDecade = 1,
                place = 1,
            ),
        )
        val repaired = EldersDomain.repairInterviews(valid + invalid)

        assertEquals(50, repaired.size)
        assertTrue(repaired.all { it.nickname.length == EldersDomain.MAX_NICKNAME_CHARS })
        assertEquals(49L, repaired.last().createdAt)
    }

    @Test
    fun corruptedKk1IsRejectedWithoutThrowing() {
        val (_, code) = goldenInterview()
        val corrupted = code.dropLast(1) + if (code.last() == '0') "1" else "0"
        assertNull(EldersDomain.importShareCode(corrupted))
    }
}
