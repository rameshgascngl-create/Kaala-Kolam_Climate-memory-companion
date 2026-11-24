package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test

class CitizenOfflineScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun todayShowsNoDataAndManualSelectionInEnglish() {
        composeRule.setContent {
            var district by remember { mutableStateOf("") }
            KaalaKolamTheme(ThemeMode.LIGHT) {
                CitizenTodayScreen("en", districtId = district, onDistrictChange = { district = it }, onSafety = {})
            }
        }
        composeRule.onNodeWithTag("citizen.weather.unavailable").assertExists()
        composeRule.onNodeWithText("Last official update: none").assertExists()
        composeRule.onNodeWithTag("citizen.page").performScrollToNode(hasTestTag("citizen.district.KANNIYAKUMARI"))
        composeRule.onNodeWithTag("citizen.district.KANNIYAKUMARI").performClick()
        composeRule.onNodeWithTag("citizen.page").performScrollToNode(hasTestTag("citizen.district.selected"))
        composeRule.onNodeWithTag("citizen.district.selected").assertTextEquals("Kanniyakumari")
        composeRule.onNodeWithTag("citizen.page").performScrollToNode(hasTestTag("citizen.district.clear"))
        composeRule.onNodeWithTag("citizen.district.clear").performClick()
        composeRule.onNodeWithTag("citizen.page").performScrollToNode(hasTestTag("citizen.district.selected"))
        composeRule.onNodeWithTag("citizen.district.selected").assertTextEquals("No district selected")
        composeRule.onNodeWithTag("citizen.page").performScrollToIndex(0)
        composeRule.onNodeWithTag("citizen.weather.unavailable").assertExists()
        composeRule.onNodeWithText("Live information unavailable").assertExists()
    }

    @Test fun safetyIsUnknownInTamil() {
        composeRule.setContent {
            KaalaKolamTheme(ThemeMode.LIGHT, language = "ta") { CitizenSafetyScreen("ta", onToday = {}) }
        }
        composeRule.onNodeWithTag("citizen.warning.unknown").assertExists()
        composeRule.onNodeWithText("தற்போதைய எச்சரிக்கை நிலை தெரியவில்லை").assertExists()
    }
}
