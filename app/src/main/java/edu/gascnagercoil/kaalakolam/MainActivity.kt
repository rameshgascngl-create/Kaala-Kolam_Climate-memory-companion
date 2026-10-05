package edu.gascnagercoil.kaalakolam

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import edu.gascnagercoil.kaalakolam.ui.AppViewModel
import edu.gascnagercoil.kaalakolam.ui.KaalaKolamApp
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AppViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

            LaunchedEffect(uiState.appState.language) {
                val target = uiState.appState.language
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != target) {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(target),
                    )
                }
            }

            KaalaKolamTheme(uiState.appState.themeMode, language = uiState.appState.language) {
                KaalaKolamApp(
                    uiState = uiState,
                    widthSizeClass = widthSizeClass,
                    onLanguageChange = viewModel::setLanguage,
                    onThemeChange = viewModel::setThemeMode,
                    onBackup = viewModel::backupCode,
                    onValidateBackup = viewModel::validateBackup,
                    onRestore = viewModel::restore,
                    onReset = viewModel::reset,
                )
            }
        }
    }
}
