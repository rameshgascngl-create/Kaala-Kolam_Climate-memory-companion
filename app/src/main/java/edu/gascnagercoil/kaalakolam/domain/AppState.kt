package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class AppState(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val language: String = "en",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val learnedTopicIds: Set<String> = emptySet(),
    val memoryFlags: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}

fun AppState.repair(): AppState {
    val repairedLanguage = if (language == "ta") "ta" else "en"
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

    return copy(
        schemaVersion = AppState.CURRENT_SCHEMA,
        language = repairedLanguage,
        learnedTopicIds = repairedTopics,
        memoryFlags = repairedFlags,
    )
}
