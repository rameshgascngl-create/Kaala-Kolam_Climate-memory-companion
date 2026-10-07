package edu.gascnagercoil.kaalakolam.domain

import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
enum class ClassGroupBy {
    ALL,
    PLACE,
    DECADE,
}

@Serializable
data class ClassPoolState(
    val imports: List<M2Domain.InterviewRecord> = emptyList(),
    val plans: List<M2Domain.PlanRecord> = emptyList(),
    val sample: Boolean = false,
    val groupBy: ClassGroupBy = ClassGroupBy.ALL,
)

data class ClassImportSummary(
    val added: Int = 0,
    val duplicates: Int = 0,
    val rejected: Int = 0,
    val found: Int = 0,
)

data class ClassImportUpdate(
    val state: ClassPoolState,
    val summary: ClassImportSummary,
)

data class SharedMemory(
    val questionIndex: Int,
    val row: M2Domain.AggregateRow,
    val score: Double,
)

data class ClassPlanSummary(
    val optionCounts: List<Int>,
    val plansLeavingSomeoneUnprotected: Int,
)

object ClassPoolDomain {
    val planOptionIds: List<String> = listOf(
        "warn",
        "shelter",
        "retrofit",
        "drains",
        "mangrove",
        "income",
        "clean",
    )

    fun repair(pool: ClassPoolState): ClassPoolState {
        val ids = mutableSetOf<String>()
        val interviews = pool.imports.filter { record ->
            validInterview(record) && ids.add(record.i)
        }
        val plans = pool.plans.filter { record ->
            validPlan(record) && ids.add(record.i)
        }
        return pool.copy(imports = interviews, plans = plans)
    }

    fun importCodes(text: String, pool: ClassPoolState): ClassImportUpdate {
        val repaired = repair(pool)
        val existingIds = buildSet {
            repaired.imports.forEach { add(it.i) }
            repaired.plans.forEach { add(it.i) }
        }
        val imported = M2Domain.importCodes(text, existingIds)
        val interviews = repaired.imports.toMutableList()
        val plans = repaired.plans.toMutableList()
        imported.records.forEach { record ->
            when (record) {
                is M2Domain.ShareRecord.Interview -> interviews += record.value
                is M2Domain.ShareRecord.Plan -> plans += record.value
            }
        }
        return ClassImportUpdate(
            state = repair(repaired.copy(imports = interviews, plans = plans)),
            summary = ClassImportSummary(
                added = imported.added,
                duplicates = imported.duplicates,
                rejected = imported.rejected,
                found = imported.found,
            ),
        )
    }

    fun allInterviewRecords(state: AppState): List<M2Domain.InterviewRecord> {
        val pool = repair(state.classPool)
        val byId = linkedMapOf<String, M2Domain.InterviewRecord>()
        pool.imports.forEach { byId[it.i] = it }
        state.interviews
            .filter { EldersDomain.answeredCount(it) >= 3 }
            .forEach { interview ->
                runCatching { EldersDomain.ivRecord(interview) }
                    .getOrNull()
                    ?.let { byId[it.i] = it }
            }
        if (pool.sample) {
            M2Domain.sampleRecords().forEach { byId[it.i] = it }
        }
        return byId.values.toList()
    }

    fun allPlanRecords(state: AppState): List<M2Domain.PlanRecord> =
        repair(state.classPool).plans

    fun aggregate(
        records: List<M2Domain.InterviewRecord>,
        questionIndex: Int,
        groupBy: ClassGroupBy,
    ): List<M2Domain.AggregateRow> = when (groupBy) {
        ClassGroupBy.ALL -> M2Domain.aggregateAll(records, questionIndex)
        ClassGroupBy.PLACE -> M2Domain.aggregateByPlace(records, questionIndex)
        ClassGroupBy.DECADE -> M2Domain.aggregate(
            records = records,
            questionIndex = questionIndex,
            keys = (0..5).map { it to "d$it" },
            keyOf = { it.b },
        )
    }

    fun strongestSharedMemories(
        records: List<M2Domain.InterviewRecord>,
    ): List<SharedMemory> =
        (0 until M2Domain.QUESTION_COUNT)
            .map { questionIndex ->
                val row = M2Domain.aggregateAll(records, questionIndex).single()
                val score = if (row.n >= 5 && row.agreement != null && row.mean != null) {
                    abs(row.mean) * row.agreement
                } else {
                    0.0
                }
                SharedMemory(questionIndex, row, score)
            }
            .sortedByDescending { it.score }
            .subList(0, minOf(3, M2Domain.QUESTION_COUNT))
            .filter { it.score > 0.3 }

    fun planSummary(plans: List<M2Domain.PlanRecord>): ClassPlanSummary {
        val counts = planOptionIds.indices.map { optionIndex ->
            plans.count { optionIndex in it.o }
        }
        val left = plans.count { plan ->
            val picks = plan.o.mapNotNull(planOptionIds::getOrNull)
            M2Domain.councilUncovered(picks).isNotEmpty()
        }
        return ClassPlanSummary(
            optionCounts = counts,
            plansLeavingSomeoneUnprotected = left,
        )
    }

    private fun validInterview(record: M2Domain.InterviewRecord): Boolean =
        runCatching { M2Domain.interviewCode(record) }.isSuccess

    private fun validPlan(record: M2Domain.PlanRecord): Boolean =
        runCatching { M2Domain.planCode(record) }.isSuccess
}
