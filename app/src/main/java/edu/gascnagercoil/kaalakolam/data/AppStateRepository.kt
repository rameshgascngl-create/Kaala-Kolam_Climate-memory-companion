package edu.gascnagercoil.kaalakolam.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.ElderMode
import edu.gascnagercoil.kaalakolam.domain.EldersDomain
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.domain.repair
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val stateJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

internal object AppStateSerializer : Serializer<AppState> {
    override val defaultValue: AppState = AppState()

    override suspend fun readFrom(input: InputStream): AppState =
        runCatching {
            stateJson.decodeFromString<AppState>(input.readBytes().decodeToString()).repair()
        }.getOrElse { defaultValue }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(stateJson.encodeToString(t.repair()).encodeToByteArray())
    }
}

private val Context.appStateDataStore: DataStore<AppState> by dataStore(
    fileName = "kaala_kolam_state.json",
    serializer = AppStateSerializer,
)

class AppStateRepository(context: Context) {
    private val store = context.applicationContext.appStateDataStore

    val state: Flow<AppState> = store.data
        .catch { emit(AppState()) }
        .map { it.repair() }

    suspend fun setLanguage(language: String) {
        store.updateData { current -> current.copy(language = language).repair() }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        store.updateData { current -> current.copy(themeMode = themeMode).repair() }
    }

    suspend fun setPredictionAnswer(id: String, answer: PredictionState) {
        store.updateData { current ->
            current.copy(
                predictionAnswers = current.predictionAnswers + (id to answer),
            ).repair()
        }
    }

    suspend fun clearPredictionAnswers() {
        store.updateData { current -> current.copy(predictionAnswers = emptyMap()).repair() }
    }

    suspend fun setElderState(interviews: List<Interview>, session: ElderSession) {
        store.updateData { current ->
            current.copy(
                interviews = interviews,
                elderSession = session,
            ).repair()
        }
    }

    suspend fun setElderSession(session: ElderSession) {
        store.updateData { current ->
            current.copy(elderSession = session).repair()
        }
    }

    suspend fun addElderInterview(interview: Interview) {
        store.updateData { current ->
            if (
                current.interviews.size >= EldersDomain.MAX_INTERVIEWS ||
                current.interviews.any { it.id == interview.id }
            ) {
                current
            } else {
                current.copy(
                    interviews = current.interviews + interview,
                    elderSession = ElderSession(
                        mode = ElderMode.ASK,
                        activeId = interview.id,
                        questionIndex = 0,
                    ),
                ).repair()
            }
        }
    }

    suspend fun setElderAnswer(
        interviewId: String,
        questionId: String,
        answer: InterviewAnswer,
    ) {
        store.updateData { current ->
            current.copy(
                interviews = current.interviews.map { interview ->
                    if (interview.id == interviewId && questionId in EldersDomain.questionIds) {
                        interview.copy(answers = interview.answers + (questionId to answer))
                    } else {
                        interview
                    }
                },
            ).repair()
        }
    }

    suspend fun deleteElderInterview(id: String) {
        store.updateData { current ->
            current.copy(
                interviews = current.interviews.filterNot { it.id == id },
            ).repair()
        }
    }

    suspend fun replace(validated: AppState) {
        store.updateData { validated.repair() }
    }

    suspend fun reset() {
        store.updateData { AppState() }
    }
}
