package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    @Test fun climateShowsUnavailableRatherThanInventedTrend() {
        composeRule.setContent {
            KaalaKolamTheme(ThemeMode.LIGHT) { CitizenClimateScreen("en", "KANNIYAKUMARI") }
        }
        composeRule.onNodeWithTag("citizen.climate.unavailable").assertExists()
    }
}
