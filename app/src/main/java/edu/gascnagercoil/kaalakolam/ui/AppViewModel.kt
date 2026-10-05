package edu.gascnagercoil.kaalakolam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.gascnagercoil.kaalakolam.content.ContentRepository
import edu.gascnagercoil.kaalakolam.content.GapManifest
import edu.gascnagercoil.kaalakolam.data.AppStateRepository
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.BackupCodec
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun setPredictionAnswer(id: String, answer: PredictionState) {
        viewModelScope.launch { stateRepository.setPredictionAnswer(id, answer) }
    }

    fun clearPredictionAnswers() {
        viewModelScope.launch { stateRepository.clearPredictionAnswers() }
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
