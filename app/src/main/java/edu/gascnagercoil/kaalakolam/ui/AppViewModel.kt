package edu.gascnagercoil.kaalakolam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.gascnagercoil.kaalakolam.content.ContentRepository
import edu.gascnagercoil.kaalakolam.content.GapManifest
import edu.gascnagercoil.kaalakolam.data.AppStateRepository
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.BackupCodec
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AppUiState(
    val appState: AppState = AppState(),
    val gaps: GapManifest? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val stateRepository = AppStateRepository(application)
    private val contentRepository = ContentRepository(application)
    private val gapManifest = MutableStateFlow<GapManifest?>(null)

    val uiState = combine(stateRepository.state, gapManifest) { state, gaps ->
        AppUiState(appState = state, gaps = gaps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            gapManifest.value = runCatching { contentRepository.loadGaps() }.getOrNull()
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { stateRepository.setLanguage(language) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { stateRepository.setThemeMode(themeMode) }
    }

    fun setCurrentTab(tab: String) {
        viewModelScope.launch { stateRepository.setCurrentTab(tab) }
    }

    fun setPredictionAnswer(id: String, answer: PredictionState) {
        viewModelScope.launch { stateRepository.setPredictionAnswer(id, answer) }
    }

    fun clearPredictionAnswers() {
        viewModelScope.launch { stateRepository.clearPredictionAnswers() }
    }

    fun setElderSession(session: ElderSession) {
        viewModelScope.launch { stateRepository.setElderSession(session) }
    }

    fun createElderInterview(
        nickname: String,
        birthDecade: Int,
        place: Int,
        defaultNickname: String,
    ) {
        val id = Integer.toUnsignedString(UUID.randomUUID().hashCode(), 36)
        val interview = Interview(
            id = id,
            nickname = nickname.trim().ifBlank { defaultNickname },
            birthDecade = birthDecade,
            place = place,
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { stateRepository.addElderInterview(interview) }
    }

    fun deleteElderInterview(id: String) {
        viewModelScope.launch { stateRepository.deleteElderInterview(id) }
    }

    fun setElderAnswer(interviewId: String, questionId: String, answer: InterviewAnswer) {
        viewModelScope.launch {
            stateRepository.setElderAnswer(interviewId, questionId, answer)
        }
    }

    fun advanceElderQuestion(
        interviewId: String,
        questionId: String,
        answer: InterviewAnswer?,
        session: ElderSession,
    ) {
        viewModelScope.launch {
            stateRepository.advanceElderQuestion(interviewId, questionId, answer, session)
        }
    }

    fun setElderReaction(interviewId: String, reaction: String) {
        viewModelScope.launch {
            stateRepository.setElderReaction(interviewId, reaction)
        }
    }

    fun backupCode(state: AppState): String = BackupCodec.encode(state)

    fun validateBackup(code: String): AppState? = BackupCodec.decode(code).getOrNull()

    fun restore(validated: AppState) {
        viewModelScope.launch { stateRepository.replace(validated) }
    }

    fun reset() {
        viewModelScope.launch { stateRepository.reset() }
    }
}
