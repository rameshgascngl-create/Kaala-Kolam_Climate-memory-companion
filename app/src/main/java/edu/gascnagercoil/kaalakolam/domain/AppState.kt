package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class PredictionState(
    val guess: Double,
    val confidence: Int,
    val hit: Boolean,
)

@Serializable
data class AppState(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val language: String = "en",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currentTab: String = "home",
    val learnedTopicIds: Set<String> = emptySet(),
    val memoryFlags: Map<String, Boolean> = emptyMap(),
    val predictionAnswers: Map<String, PredictionState> = emptyMap(),
    val interviews: List<Interview> = emptyList(),
    val elderSession: ElderSession = ElderSession(),
    val elderAlias: String = "",
    val notes: String = "",
    val reflection: String = "",
) {
    companion object {
        const val CURRENT_SCHEMA = 2
    }
}

fun AppState.repair(): AppState {
    val repairedLanguage = if (language == "ta") "ta" else "en"
    val repairedCurrentTab = currentTab.takeIf {
        it in setOf("home", "learn", "elders", "class", "council", "predict")
    } ?: "home"
    val repairedTopics = learnedTopicIds
        .asSequence()
        .filter { it.isNotBlank() && it.length <= 64 }
        .distinct()
        .take(256)
        .toSet()
    val repairedFlags = memoryFlags
        .asSequence()
        .filter { (key, _) -> key.isNotBlank() && key.length <= 64 }
        .take(256)
        .associate { it.toPair() }
    val repairedPredictions = M2Domain.predictionSpecs.mapNotNull { spec ->
        predictionAnswers[spec.id]?.let { answer ->
            if (!answer.guess.isFinite() || answer.confidence !in 1..3) {
                null
            } else {
                val guess = M2Domain.snapPredictionGuess(spec, answer.guess)
                spec.id to PredictionState(
                    guess = guess,
                    confidence = answer.confidence,
                    hit = M2Domain.predictionHit(spec, guess),
                )
            }
        }
    }.toMap()
    val repairedInterviews = EldersDomain.repairInterviews(interviews)
    val repairedSession = EldersDomain.repairSession(elderSession, repairedInterviews)

    return copy(
        schemaVersion = AppState.CURRENT_SCHEMA,
        language = repairedLanguage,
        currentTab = repairedCurrentTab,
        learnedTopicIds = repairedTopics,
        memoryFlags = repairedFlags,
        predictionAnswers = repairedPredictions,
        interviews = repairedInterviews,
        elderSession = repairedSession,
    )
}
