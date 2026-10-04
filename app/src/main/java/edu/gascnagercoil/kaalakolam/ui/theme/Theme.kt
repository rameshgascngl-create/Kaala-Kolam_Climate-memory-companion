package edu.gascnagercoil.kaalakolam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import edu.gascnagercoil.kaalakolam.domain.ThemeMode

private val LightColours = lightColorScheme(
    primary = Color(0xFF735400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08B),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF006B66),
    onSecondary = Color.White,
    tertiary = Color(0xFF8B3B25),
    onTertiary = Color.White,
    background = Color(0xFFF7FAF9),
    onBackground = Color(0xFF10263A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10263A),
)

private val DarkColours = darkColorScheme(
    primary = Color(0xFFFFD166),
    onPrimary = Color(0xFF2A1D00),
    primaryContainer = Color(0xFF584000),
    onPrimaryContainer = Color(0xFFFFE7A8),
    secondary = Color(0xFF5FD5CD),
    onSecondary = Color(0xFF003733),
    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF542011),
    background = Color(0xFF10263A),
    onBackground = Color(0xFFF4EFE6),
    surface = Color(0xFF16334B),
    onSurface = Color(0xFFF4EFE6),
)

@Composable
fun KaalaKolamTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColours else LightColours,
        content = content,
    )
}
