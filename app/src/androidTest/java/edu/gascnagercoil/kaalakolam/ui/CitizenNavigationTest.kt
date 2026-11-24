package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import edu.gascnagercoil.kaalakolam.domain.AppState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import org.junit.Rule
import org.junit.Test

class CitizenNavigationTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun allFiveTamilSectionsSelectActualRoutes() {
        composeRule.setContent {
            var route by remember { mutableStateOf(TODAY_ROUTE) }
            KaalaKolamTheme(ThemeMode.LIGHT, language = "ta") {
                CitizenNavigationBar("ta", route) { route = it }
            }
        }
        CitizenSection.entries.forEach { section ->
            composeRule.onNodeWithTag("citizen.tab." + section.name).assertExists().performClick()
            composeRule.onNodeWithTag("citizen.tab." + section.name).assertIsSelected()
        }
    }
    @Test fun realNavigationHostOpensCitizenAndLegacyModules() {
        composeRule.setContent {
            var state by remember { mutableStateOf(AppState()) }
            KaalaKolamTheme(ThemeMode.LIGHT, language = "en") {
                KaalaKolamApp(
                    uiState = AppUiState(appState = state),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onLanguageChange = { state = state.copy(language = it) },
                    onThemeChange = { state = state.copy(themeMode = it) },
                    onCurrentTabChange = { state = state.copy(currentTab = it) },
                    onPilotDistrictChange = { state = state.copy(pilotDistrictId = it) },
                    onBackup = { "" },
                    onValidateBackup = { null },
                    onRestore = { state = it },
                    onReset = { state = AppState() },
                )
            }
        }
        composeRule.onNodeWithTag("citizen.home.list").performScrollToIndex(3)
        composeRule.onNodeWithTag("touch.citizen.home.today").performClick()
        composeRule.onNodeWithTag("citizen.weather.unavailable").assertExists()
        composeRule.onNodeWithTag("citizen.tab.SAFETY").performClick()
        composeRule.onNodeWithTag("citizen.warning.unknown").assertExists()
        composeRule.onNodeWithTag("citizen.tab.CLIMATE").performClick()
        composeRule.onNodeWithTag("citizen.climate.unavailable").assertExists()
        composeRule.onNodeWithTag("citizen.tab.LEARN").performClick()
        composeRule.onNodeWithTag("citizen.module.learn").assertExists()
        composeRule.onNodeWithTag("citizen.tab.COMMUNITY").performClick()
        composeRule.onNodeWithTag("citizen.module.elders").performClick()
        composeRule.onNodeWithTag("touch.tab.elders").assertExists()
        composeRule.onNodeWithText("Elder interviews").assertExists()
    }

    @Test fun tamilNavigationAtDoubleFontScaleRetainsFiveTargets() {
        composeRule.setContent {
            val density = LocalDensity.current
            KaalaKolamTheme(ThemeMode.LIGHT, language = "ta") {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    CitizenNavigationBar("ta", TODAY_ROUTE) {}
                }
            }
        }
        CitizenSection.entries.forEach { section ->
            composeRule.onNodeWithTag("citizen.tab." + section.name).assertExists()
        }
    }

    @Test fun climateShowsUnavailableRatherThanInventedTrend() {
        composeRule.setContent {
            KaalaKolamTheme(ThemeMode.LIGHT) { CitizenClimateScreen("en", "KANNIYAKUMARI") }
        }
        composeRule.onNodeWithTag("citizen.climate.unavailable").assertExists()
    }
}
