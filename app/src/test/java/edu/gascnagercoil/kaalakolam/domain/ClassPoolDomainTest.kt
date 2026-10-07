package edu.gascnagercoil.kaalakolam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassPoolDomainTest {
    @Test
    fun importMatchesSharedIdDuplicateAndCorruptRules() {
        val first = M2Domain.InterviewRecord("I", "iv001", 0, 1, "1234501234")
        val plan = M2Domain.PlanRecord("P", "pl001", 2, listOf(0, 1, 4))
        val firstCode = M2Domain.interviewCode(first)
        val planCode = M2Domain.planCode(plan)
        val corrupt = planCode.dropLast(1) + if (planCode.last() == '0') "1" else "0"
        val pool = ClassPoolState(imports = listOf(first))

        val update = ClassPoolDomain.importCodes(
            listOf(firstCode, planCode, corrupt).joinToString("\n"),
            pool,
        )

        assertEquals(3, update.summary.found)
        assertEquals(1, update.summary.added)
        assertEquals(1, update.summary.duplicates)
        assertEquals(1, update.summary.rejected)
        assertEquals(listOf(first), update.state.imports)
        assertEquals(listOf(plan), update.state.plans)
    }

    @Test
    fun allRecordsUsePrototypePrecedenceImportedThenMineThenSample() {
        val imported = M2Domain.InterviewRecord("I", "smp000", 4, 5, "5555555555")
        val mine = Interview(
            id = "smp000",
            nickname = "Mine",
            birthDecade = 1,
            place = 0,
            answers = mapOf(
                "heat" to InterviewAnswer(true, 2),
                "nights" to InterviewAnswer(true, 1),
                "monsoon" to InterviewAnswer(true, 0),
            ),
        )
        val state = AppState(
            interviews = listOf(mine),
            classPool = ClassPoolState(imports = listOf(imported), sample = true),
        )

        val records = ClassPoolDomain.allInterviewRecords(state)
        val sampleFirst = M2Domain.sampleRecords().first()

        assertEquals(28, records.size)
        assertEquals(sampleFirst, records.first { it.i == "smp000" })
    }

    @Test
    fun decadeAggregationMatchesGenericPrototypePort() {
        val records = M2Domain.sampleRecords()
        val expected = M2Domain.aggregate(
            records = records,
            questionIndex = 0,
            keys = (0..5).map { it to "d$it" },
            keyOf = { it.b },
        )
        assertEquals(
            expected,
            ClassPoolDomain.aggregate(records, 0, ClassGroupBy.DECADE),
        )
    }

    @Test
    fun strongestSharedMemoriesUsePrototypeScoreAndThreshold() {
        val strongest = ClassPoolDomain.strongestSharedMemories(M2Domain.sampleRecords())
        assertTrue(strongest.size <= 3)
        assertTrue(strongest.all { it.row.n >= 5 && it.score > 0.3 })
        assertEquals(strongest.sortedByDescending { it.score }, strongest)
    }

    @Test
    fun planSummaryCountsEachPlanOncePerOptionAndFlagsUncoveredPlans() {
        val plans = listOf(
            M2Domain.PlanRecord("P", "p1", 0, listOf(0, 1)),
            M2Domain.PlanRecord("P", "p2", 2, listOf(6)),
        )
        val summary = ClassPoolDomain.planSummary(plans)

        assertEquals(listOf(1, 1, 0, 0, 0, 0, 1), summary.optionCounts)
        assertEquals(1, summary.plansLeavingSomeoneUnprotected)
    }

    @Test
    fun repairRejectsInvalidRecordsAndDeduplicatesAcrossTypes() {
        val duplicateInterview = M2Domain.InterviewRecord("I", "same", 0, 1, "1234501234")
        val duplicatePlan = M2Domain.PlanRecord("P", "same", 0, listOf(0))
        val invalidInterview = M2Domain.InterviewRecord("I", "bad", 9, 1, "1234501234")
        val invalidPlan = M2Domain.PlanRecord("P", "badplan", 9, listOf(0))

        val repaired = ClassPoolDomain.repair(
            ClassPoolState(
                imports = listOf(duplicateInterview, invalidInterview),
                plans = listOf(duplicatePlan, invalidPlan),
            ),
        )

        assertEquals(listOf(duplicateInterview), repaired.imports)
        assertTrue(repaired.plans.isEmpty())
    }
}
