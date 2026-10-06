package edu.gascnagercoil.kaalakolam.domain

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EldersKk1CompatibilityTest {
    private val json = Json { ignoreUnknownKeys = false }
    private fun fixtureFile(): File = listOf(
        File("../tests/elders/kk1-native-fixtures.json"),
        File("tests/elders/kk1-native-fixtures.json"),
    ).first(File::isFile)

    @Test
    fun nativeExportAndImportMatchHtmlOracleFixturesAndExcludePrivateText() {
        val cases = json.parseToJsonElement(fixtureFile().readText()).jsonObject
            .getValue("cases").jsonArray
        cases.forEach { node ->
            val item = node.jsonObject
            val record = json.decodeFromJsonElement(
                M2Domain.InterviewRecord.serializer(),
                item.getValue("record"),
            )
            val code = item.getValue("nativeCode").jsonPrimitive.content
            val answers = EldersDomain.questionIds.zip(record.a.toList()).associate { (id, encoded) ->
                id to InterviewAnswer(
                    answered = true,
                    rating = if (encoded == '0') null else encoded.digitToInt() - 3,
                    confidence = if (encoded == '0') null else 2,
                    story = "private story தமிழ்",
                )
            }
            val interview = Interview(
                id = record.i,
                nickname = item.getValue("nickname").jsonPrimitive.content,
                birthDecade = record.b,
                place = record.p,
                answers = answers,
                reaction = item.getValue("reaction").jsonPrimitive.content,
                createdAt = 1L,
            )
            assertEquals(record, EldersDomain.ivRecord(interview))
            assertEquals(code, EldersDomain.exportShareCode(interview))
            assertEquals(record, EldersDomain.importShareCode(code))
            assertFalse(code.contains(interview.nickname))
            assertFalse(code.contains(interview.reaction))
            assertFalse(code.contains("private story"))
            assertFalse(code.contains("தமிழ்"))
        }
    }

    @Test
    fun corruptInputIsRejected() {
        val code = json.parseToJsonElement(fixtureFile().readText()).jsonObject
            .getValue("cases").jsonArray.first().jsonObject
            .getValue("nativeCode").jsonPrimitive.content
        val corrupted = code.dropLast(1) + if (code.last() == '0') "1" else "0"
        assertNull(EldersDomain.importShareCode(corrupted))
    }
}
