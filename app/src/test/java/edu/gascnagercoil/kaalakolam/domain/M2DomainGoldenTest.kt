package edu.gascnagercoil.kaalakolam.domain

import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M2DomainGoldenTest {
    @Serializable
    private data class ShareInterview(
        val record: M2Domain.InterviewRecord,
        val code: String,
    )

    @Serializable
    private data class SharePlan(
        val record: M2Domain.PlanRecord,
        val code: String,
    )

    @Serializable
    private data class ShareCodes(
        val interview: ShareInterview,
        val plan: SharePlan,
    )

    @Serializable
    private data class SampleSummary(
        val count: Int,
        val canonicalSha256: String,
        val first: List<M2Domain.InterviewRecord>,
        val last: List<M2Domain.InterviewRecord>,
    )

    @Serializable
    private data class AggregateRow(
        val label: String,
        val c: List<Int>,
        val n: Int,
        val mean: Double?,
        val agree: Double?,
    )

    @Serializable
    private data class AggregateFixture(
        val name: String,
        val questionIndex: Int,
        val groupBy: String,
        val rows: List<AggregateRow>,
    )

    @Serializable
    private data class CouncilFixture(
        val name: String,
        val picks: List<String>,
        val scenario: Int,
        val spent: Int,
        val hazard: Double,
        val risks: Map<String, Double>,
        val mean: Double,
        val uncovered: List<String>,
    )

    @Serializable
    private data class PredictionFixture(
        val id: String,
        val min: Double,
        val max: Double,
        val step: Double,
        val initial: Double,
        val low: Double,
        val high: Double,
        val unit: String,
    )

    @Serializable
    private data class Golden(
        val schemaVersion: Int,
        val sourceHtml: String,
        val sourceHtmlSha256: String,
        val shareCodes: ShareCodes,
        val sampleRecords: SampleSummary,
        val aggregates: List<AggregateFixture>,
        val council: List<CouncilFixture>,
        val predictionSpecs: List<PredictionFixture>,
    )

    private val json = Json { ignoreUnknownKeys = false }

    private fun golden(): Golden {
        val candidates = listOf(
            File("../tests/m2/web-golden.json"),
            File("tests/m2/web-golden.json"),
        )
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("tests/m2/web-golden.json not found; user.dir=" + System.getProperty("user.dir"), file)
        return json.decodeFromString(requireNotNull(file).readText())
    }

    @Test
    fun webShareCodesMatchExactly() {
        val golden = golden()

        assertEquals(
            golden.shareCodes.interview.code,
            M2Domain.interviewCode(golden.shareCodes.interview.record),
        )
        assertEquals(
            golden.shareCodes.plan.code,
            M2Domain.planCode(golden.shareCodes.plan.record),
        )
        assertEquals(
            M2Domain.ShareRecord.Interview(golden.shareCodes.interview.record),
            M2Domain.decodeShareCode(golden.shareCodes.interview.code),
        )
        assertEquals(
            M2Domain.ShareRecord.Plan(golden.shareCodes.plan.record),
            M2Domain.decodeShareCode(golden.shareCodes.plan.code),
        )

        val corrupted = golden.shareCodes.interview.code.dropLast(1) +
            if (golden.shareCodes.interview.code.last() == '0') "1" else "0"
        val imported = M2Domain.importCodes(
            listOf(
                golden.shareCodes.interview.code,
                golden.shareCodes.interview.code,
                golden.shareCodes.plan.code,
                corrupted,
            ).joinToString("\n"),
        )
        assertEquals(4, imported.found)
        assertEquals(2, imported.added)
        assertEquals(1, imported.duplicates)
        assertEquals(1, imported.rejected)
    }

    @Test
    fun deterministicSampleRecordsMatchWebGolden() {
        val golden = golden().sampleRecords
        val actual = M2Domain.sampleRecords()

        assertEquals(golden.count, actual.size)
        assertEquals(golden.first, actual.take(golden.first.size))
        assertEquals(golden.last, actual.takeLast(golden.last.size))

        val canonical = json.encodeToString(actual)
        assertEquals(golden.canonicalSha256, sha256(canonical))
    }

    @Test
    fun aggregateMathMatchesWebGolden() {
        val golden = golden()
        val records = M2Domain.sampleRecords()

        golden.aggregates.forEach { fixture ->
            val actual = when (fixture.groupBy) {
                "all" -> M2Domain.aggregateAll(records, fixture.questionIndex)
                "place" -> M2Domain.aggregateByPlace(records, fixture.questionIndex)
                else -> error("Unsupported golden group: " + fixture.groupBy)
            }
            assertEquals(fixture.rows.size, actual.size)
            fixture.rows.zip(actual).forEach { (expected, got) ->
                assertEquals(expected.label, got.label)
                assertEquals(expected.c, got.counts)
                assertEquals(expected.n, got.n)
                assertNullableDouble(expected.mean, got.mean)
                assertNullableDouble(expected.agree, got.agreement)
            }
        }
    }

    @Test
    fun councilMathMatchesWebGolden() {
        golden().council.forEach { fixture ->
            assertEquals(fixture.spent, M2Domain.councilSpent(fixture.picks))
            assertEquals(fixture.uncovered, M2Domain.councilUncovered(fixture.picks))
            val actual = M2Domain.councilRisk(fixture.picks, fixture.scenario)
            assertEquals(fixture.hazard, actual.hazard, 1e-12)
            assertEquals(fixture.risks.keys, actual.risks.keys)
            fixture.risks.forEach { (group, expected) ->
                assertEquals(expected, actual.risks.getValue(group), 1e-12)
            }
            assertEquals(fixture.mean, actual.mean, 1e-12)
        }
    }

    @Test
    fun predictionNumericSpecsMatchWebGolden() {
        val expected = golden().predictionSpecs
        val actual = M2Domain.predictionSpecs

        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (web, native) ->
            assertEquals(web.id, native.id)
            assertEquals(web.min, native.min, 0.0)
            assertEquals(web.max, native.max, 0.0)
            assertEquals(web.step, native.step, 0.0)
            assertEquals(web.initial, native.initial, 0.0)
            assertEquals(web.low, native.low, 0.0)
            assertEquals(web.high, native.high, 0.0)
            assertEquals(web.unit, native.unit)
            assertTrue(M2Domain.predictionHit(native, native.low))
            assertTrue(M2Domain.predictionHit(native, native.high))
        }
    }

    @Test
    fun predictionCalibrationUsesPrototypeThresholds() {
        val overconfident = listOf(
            M2Domain.PredictionAnswer("a", 0.0, 3, true),
            M2Domain.PredictionAnswer("b", 0.0, 3, false),
            M2Domain.PredictionAnswer("c", 0.0, 2, true),
        )
        assertEquals(
            M2Domain.CertainCalibration.OVERCONFIDENT,
            M2Domain.certainCalibration(overconfident),
        )

        val perfect = listOf(
            M2Domain.PredictionAnswer("a", 0.0, 3, true),
            M2Domain.PredictionAnswer("b", 0.0, 3, true),
        )
        assertEquals(
            M2Domain.CertainCalibration.PERFECT_SO_FAR,
            M2Domain.certainCalibration(perfect),
        )

        assertEquals(
            M2Domain.CertainCalibration.KEEP_GOING,
            M2Domain.certainCalibration(perfect.take(1)),
        )
    }

    private fun assertNullableDouble(expected: Double?, actual: Double?) {
        if (expected == null) {
            assertEquals(null, actual)
        } else {
            assertNotNull(actual)
            assertEquals(expected, requireNotNull(actual), 1e-12)
        }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
