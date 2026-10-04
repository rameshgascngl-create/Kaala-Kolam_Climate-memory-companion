package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.ui.theme.KaalaKolamTheme
import org.junit.Rule
import org.junit.Test

class TamilNavigationSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allSixTamilTabsExposeTamilContentDescriptions() {
        composeRule.setContent {
            KaalaKolamTheme(ThemeMode.LIGHT) {
                KaalaKolamVisualFixture(
                    uiState = AppUiState(
                        appState = AppState(language = "ta", themeMode = ThemeMode.LIGHT),
                        gaps = null,
                    ),
                    visualState = "home",
                )
            }
        }
        listOf("முகப்பு", "கற்க", "மூத்தோர்", "வகுப்பு", "ஊர்சபை", "கணிப்பு").forEach { label ->
            composeRule.onNode(hasContentDescription(label), useUnmergedTree = true).assertExists()
        }
    }
}
