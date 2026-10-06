package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ElderMode {
    LIST,
    SETUP,
    ASK,
    RESULT,
}

@Serializable
data class InterviewAnswer(
    val answered: Boolean = false,
    val rating: Int? = null,
    val confidence: Int? = null,
    val story: String = "",
)

@Serializable
data class Interview(
    val id: String,
    val nickname: String,
    val birthDecade: Int,
    val place: Int,
    val answers: Map<String, InterviewAnswer> = emptyMap(),
    val reaction: String = "",
    val createdAt: Long = 0L,
)

@Serializable
data class ElderSession(
    val mode: ElderMode = ElderMode.LIST,
    val activeId: String? = null,
    val questionIndex: Int = 0,
)

object EldersDomain {
    const val MAX_INTERVIEWS = 50
    const val MAX_NICKNAME_CHARS = 30
    const val MAX_FREE_TEXT_CHARS = 500

    val questionIds: List<String> = listOf(
        "heat",
        "nights",
        "monsoon",
        "downpour",
        "water",
        "coast",
        "yield",
        "birds",
        "cold",
        "storms",
    )

    fun answeredCount(interview: Interview): Int =
        questionIds.count { interview.answers[it]?.answered == true }

    fun repairInterviews(interviews: List<Interview>): List<Interview> {
        val byId = linkedMapOf<String, Interview>()
        interviews.forEach { candidate ->
            val repaired = repairInterview(candidate) ?: return@forEach
            if (repaired.id !in byId && byId.size < MAX_INTERVIEWS) {
                byId[repaired.id] = repaired
            }
        }
        return byId.values.toList()
    }

    fun repairInterview(interview: Interview): Interview? {
        val id = interview.id.trim()
        val nickname = interview.nickname.trim().ifBlank { "Elder" }
        if (id.isEmpty() || id.length > 12) return null
        if (nickname.length > MAX_NICKNAME_CHARS) return null
        if (interview.reaction.length > MAX_FREE_TEXT_CHARS) return null
        if (interview.place !in 0..4 || interview.birthDecade !in 0..5) return null

        val repairedAnswers = linkedMapOf<String, InterviewAnswer>()
        questionIds.forEach { questionId ->
            val answer = interview.answers[questionId] ?: return@forEach
            if (!answer.answered) return@forEach
            if (answer.rating != null && answer.rating !in -2..2) return@forEach
            if (answer.story.length > MAX_FREE_TEXT_CHARS) return null
            val confidence = if (answer.rating != null && answer.confidence in 1..3) {
                answer.confidence
            } else {
                null
            }
            repairedAnswers[questionId] = answer.copy(
                answered = true,
                confidence = confidence,
                story = if (answer.rating == null) "" else answer.story,
            )
        }

        return interview.copy(
            id = id,
            nickname = nickname,
            answers = repairedAnswers,
            reaction = interview.reaction,
            createdAt = interview.createdAt.coerceAtLeast(0L),
        )
    }

    fun repairSession(session: ElderSession, interviews: List<Interview>): ElderSession {
        val ids = interviews.asSequence().map { it.id }.toSet()
        val activeId = session.activeId?.takeIf { it in ids }
        val mode = if (activeId == null && session.mode in setOf(ElderMode.ASK, ElderMode.RESULT)) {
            ElderMode.LIST
        } else {
            session.mode
        }
        return session.copy(
            mode = mode,
            activeId = activeId,
            questionIndex = session.questionIndex.coerceIn(0, M2Domain.QUESTION_COUNT - 1),
        )
    }

    fun ivRecord(interview: Interview): M2Domain.InterviewRecord {
        val repaired = requireNotNull(repairInterview(interview)) { "Interview is invalid." }
        val ratings = questionIds.map { questionId ->
            val answer = repaired.answers[questionId]
            if (answer?.answered == true) answer.rating else null
        }
        return M2Domain.interviewRecord(
            id = repaired.id,
            place = repaired.place,
            birthDecade = repaired.birthDecade,
            answers = ratings,
        )
    }

    fun exportShareCode(interview: Interview): String =
        M2Domain.interviewCode(ivRecord(interview))

    fun importShareCode(code: String): M2Domain.InterviewRecord? =
        when (val decoded = M2Domain.decodeShareCode(code)) {
            is M2Domain.ShareRecord.Interview -> decoded.value
            else -> null
        }
}
