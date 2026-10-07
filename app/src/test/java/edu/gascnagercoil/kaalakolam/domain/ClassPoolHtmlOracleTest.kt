package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassPoolHtmlOracleTest {
    private val fixture: JsonObject by lazy {
        val stream = requireNotNull(
            javaClass.getResourceAsStream("/class/class-parity-fixtures.json"),
        )
        Json.parseToJsonElement(stream.bufferedReader().use { it.readText() }).jsonObject
    }

    @Test
    fun shareCodesRoundTripExactlyAcrossNativeAndHtmlContract() {
        fixture.getValue("codeCases").jsonArray.forEach { element ->
            val item = element.jsonObject
            val name = item.getValue("name").jsonPrimitive.content
            val kind = item.getValue("kind").jsonPrimitive.content
            val record = item.getValue("record").jsonObject
            val code = item.getValue("code").jsonPrimitive.content

            if (kind == "K") {
                val nativeRecord = interview(record)
                assertEquals(name + ": export", code, M2Domain.interviewCode(nativeRecord))
                assertEquals(
                    name + ": import",
                    M2Domain.ShareRecord.Interview(nativeRecord),
                    M2Domain.decodeShareCode(code),
                )
            } else {
                val nativeRecord = plan(record)
                assertEquals(name + ": export", code, M2Domain.planCode(nativeRecord))
                assertEquals(
                    name + ": import",
                    M2Domain.ShareRecord.Plan(nativeRecord),
                    M2Domain.decodeShareCode(code),
                )
            }
        }

        fixture.getValue("invalidCodes").jsonArray.forEach { element ->
            val item = element.jsonObject
            val name = item.getValue("name").jsonPrimitive.content
            val code = item.getValue("code").jsonPrimitive.content
            assertNull(name, M2Domain.decodeShareCode(code))
        }
    }

    @Test
    fun importCasesMatchHtmlOracleForEmptyTamilBoundaryDuplicateAndMalformedInputs() {
        fixture.getValue("importCases").jsonArray.forEach { element ->
            val item = element.jsonObject
            val name = item.getValue("name").jsonPrimitive.content
            val existing = item.getValue("existing").jsonObject
            val expected = item.getValue("expected").jsonObject
            val pool = ClassPoolState(
                imports = existing.getValue("imports").jsonArray.map { interview(it.jsonObject) },
                plans = existing.getValue("plans").jsonArray.map { plan(it.jsonObject) },
            )

            val update = ClassPoolDomain.importCodes(
                item.getValue("text").jsonPrimitive.content,
                pool,
            )

            assertEquals(name + ": found", expected.int("found"), update.summary.found)
            assertEquals(name + ": added", expected.int("added"), update.summary.added)
            assertEquals(name + ": duplicates", expected.int("duplicates"), update.summary.duplicates)
            assertEquals(name + ": rejected", expected.int("rejected"), update.summary.rejected)
            assertEquals(
                name + ": interview ids",
                expected.strings("importIds"),
                update.state.imports.map { it.i },
            )
            assertEquals(
                name + ": plan ids",
                expected.strings("planIds"),
                update.state.plans.map { it.i },
            )
        }
    }

    @Test
    fun aggregatesAndStrongestSharedMemoriesMatchHtmlOracle() {
        val records = fixture.getValue("aggregateRecords").jsonArray.map {
            interview(it.jsonObject)
        }
        val aggregates = fixture.getValue("aggregates").jsonObject

        assertRows(
            "allQ0",
            aggregates.getValue("allQ0").jsonArray,
            ClassPoolDomain.aggregate(records, 0, ClassGroupBy.ALL),
        )
        assertRows(
            "placeQ0",
            aggregates.getValue("placeQ0").jsonArray,
            ClassPoolDomain.aggregate(records, 0, ClassGroupBy.PLACE),
        )
        assertRows(
            "decadeQ0",
            aggregates.getValue("decadeQ0").jsonArray,
            ClassPoolDomain.aggregate(records, 0, ClassGroupBy.DECADE),
        )

        val expectedStrongest = fixture.getValue("strongest").jsonArray
        val actualStrongest = ClassPoolDomain.strongestSharedMemories(records)
        assertEquals("strongest size", expectedStrongest.size, actualStrongest.size)
        expectedStrongest.indices.forEach { index ->
            val expected = expectedStrongest[index].jsonObject
            val actual = actualStrongest[index]
            assertEquals(
                "strongest question " + index,
                expected.int("questionIndex"),
                actual.questionIndex,
            )
            assertEquals("strongest n " + index, expected.int("n"), actual.row.n)
            assertNullableDouble("strongest mean " + index, expected["mean"], actual.row.mean)
            assertNullableDouble(
                "strongest agreement " + index,
                expected["agreement"],
                actual.row.agreement,
            )
            assertEquals(
                "strongest score " + index,
                expected.getValue("score").jsonPrimitive.double,
                actual.score,
                1e-12,
            )
        }
    }

    @Test
    fun planSummaryMatchesHtmlOracle() {
        val plans = fixture.getValue("planRecords").jsonArray.map { plan(it.jsonObject) }
        val expected = fixture.getValue("planSummary").jsonObject
        val actual = ClassPoolDomain.planSummary(plans)

        assertEquals(expected.ints("optionCounts"), actual.optionCounts)
        assertEquals(
            expected.int("plansLeavingSomeoneUnprotected"),
            actual.plansLeavingSomeoneUnprotected,
        )
    }

    private fun assertRows(
        name: String,
        expected: JsonArray,
        actual: List<M2Domain.AggregateRow>,
    ) {
        assertEquals(name + ": row count", expected.size, actual.size)
        expected.indices.forEach { index ->
            val row = expected[index].jsonObject
            val native = actual[index]
            assertEquals(name + ": label " + index, row.getValue("label").jsonPrimitive.content, native.label)
            assertEquals(name + ": counts " + index, row.ints("counts"), native.counts)
            assertEquals(name + ": n " + index, row.int("n"), native.n)
            assertNullableDouble(name + ": mean " + index, row["mean"], native.mean)
            assertNullableDouble(name + ": agreement " + index, row["agreement"], native.agreement)
        }
    }

    private fun assertNullableDouble(name: String, expected: JsonElement?, actual: Double?) {
        if (expected == null || expected == JsonNull) {
            assertNull(name, actual)
        } else {
            assertTrue(name + ": actual should not be null", actual != null)
            assertEquals(name, expected.jsonPrimitive.double, requireNotNull(actual), 1e-12)
        }
    }

    private fun interview(obj: JsonObject): M2Domain.InterviewRecord =
        M2Domain.InterviewRecord(
            t = obj.getValue("t").jsonPrimitive.content,
            i = obj.getValue("i").jsonPrimitive.content,
            p = obj.int("p"),
            b = obj.int("b"),
            a = obj.getValue("a").jsonPrimitive.content,
        )

    private fun plan(obj: JsonObject): M2Domain.PlanRecord =
        M2Domain.PlanRecord(
            t = obj.getValue("t").jsonPrimitive.content,
            i = obj.getValue("i").jsonPrimitive.content,
            s = obj.int("s"),
            o = obj.ints("o"),
        )

    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int

    private fun JsonObject.ints(key: String): List<Int> =
        getValue(key).jsonArray.map { it.jsonPrimitive.int }

    private fun JsonObject.strings(key: String): List<String> =
        getValue(key).jsonArray.map { it.jsonPrimitive.content }
}
