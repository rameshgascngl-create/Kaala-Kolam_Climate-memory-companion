// GENERATED FROM Kaala_Kolam_Climate_Memory_Companion.html CSS.
// Source generator: tools/extract_tokens.py. Do not introduce Material tonal substitutes here.
package edu.gascnagercoil.kaalakolam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gascnagercoil.kaalakolam.domain.ThemeMode

data class PrototypePalette(
    val ground: Color, val ground2: Color, val ground3: Color, val line: Color,
    val flour: Color, val flour2: Color, val faint: Color, val turmeric: Color,
    val vermilion: Color, val sea: Color, val ink: Color,
)

object PrototypeTokens {
    const val DisplayFontStack = "\"Palatino Linotype\",\"Book Antiqua\",Palatino,Georgia,\"Noto Serif Tamil\",\"Latha\",serif"
    const val BodyFontStack = "system-ui,-apple-system,\"Segoe UI\",Roboto,\"Noto Sans Tamil\",\"Nirmala UI\",\"Latha\",Helvetica,Arial,sans-serif"
    val CssColours = listOf("#10263a", "#16334b", "#1d4160", "#2f5876", "#f4efe6", "#cfc9bd", "#8fa6b6", "#f0b429", "#e2573a", "#34b3ab", "#0b1a27", "#1f6f8b", "#6bb3c4", "#d8d2c4", "#eba46d", "#c8452c", "#eef3f2", "#ffffff", "#dfe9e8", "#b7ccd0", "#12303a", "#38535c", "#587783", "#9a6a00", "#b8391f", "#0f7f78", "#111", "#17120a", "rgba(240,180,41,.18)", "#000", "rgba(8,20,32,.45)", "#fff")
    val CssRadii = listOf("3px", "4px", "6px", "8px", "10px", "12px", "50%", "99px")
    val CssSpacing = listOf(".4rem", ".5rem", ".8rem", "2px", "4px", "6px", "8px", "10px", "12px", "14px", "16px", "18px", "24px", "28px", "76px")
    val CssFontSizes = listOf(".66rem", ".72rem", ".74rem", ".76rem", ".78rem", ".8rem", ".82rem", ".84rem", ".85rem", ".9rem", ".92rem", "1rem", "1.08rem", "1.1rem", "1.15rem", "1.18rem", "1.2rem", "1.3rem", "1.35rem", "1.4rem", "1.9rem", "2.2rem", "3rem", "6.5vw", "12px")

    val BrandTurmeric = Color(0xFFF0B429)
    val BrandVermilion = Color(0xFFE2573A)
    val BrandSea = Color(0xFF34B3AB)
    val ScaleColours = listOf(Color(0xFF1F6F8B), Color(0xFF6BB3C4), Color(0xFFD8D2C4), Color(0xFFEBA46D), Color(0xFFC8452C))

    val Dark = PrototypePalette(
        ground = Color(0xFF10263A), ground2 = Color(0xFF16334B), ground3 = Color(0xFF1D4160), line = Color(0xFF2F5876), flour = Color(0xFFF4EFE6), flour2 = Color(0xFFCFC9BD), faint = Color(0xFF8FA6B6), turmeric = Color(0xFFF0B429), vermilion = Color(0xFFE2573A), sea = Color(0xFF34B3AB), ink = Color(0xFF0B1A27),
    )
    val Light = PrototypePalette(
        ground = Color(0xFFEEF3F2), ground2 = Color(0xFFFFFFFF), ground3 = Color(0xFFDFE9E8), line = Color(0xFFB7CCD0), flour = Color(0xFF12303A), flour2 = Color(0xFF38535C), faint = Color(0xFF587783), turmeric = Color(0xFF9A6A00), vermilion = Color(0xFFB8391F), sea = Color(0xFF0F7F78), ink = Color(0xFFFFFFFF),
    )
}

private val LocalPrototypePalette = staticCompositionLocalOf { PrototypeTokens.Dark }
object PrototypeTheme {
    val palette: PrototypePalette
        @Composable @ReadOnlyComposable get() = LocalPrototypePalette.current
}

private fun scheme(p: PrototypePalette, dark: Boolean) =
    if (dark) darkColorScheme(
        primary=p.turmeric, onPrimary=Color(0xFF17120A), primaryContainer=p.ground3, onPrimaryContainer=p.flour,
        secondary=p.sea, onSecondary=p.ink, secondaryContainer=p.ground3, onSecondaryContainer=p.flour,
        tertiary=p.vermilion, onTertiary=p.ink, tertiaryContainer=p.ground3, onTertiaryContainer=p.flour,
        background=p.ground, onBackground=p.flour, surface=p.ground2, onSurface=p.flour,
        surfaceVariant=p.ground3, onSurfaceVariant=p.flour2, outline=p.line,
        error=p.vermilion, onError=p.ink, scrim=Color(0xFF000000),
    ) else lightColorScheme(
        primary=p.turmeric, onPrimary=Color.White, primaryContainer=p.ground3, onPrimaryContainer=p.flour,
        secondary=p.sea, onSecondary=Color.White, secondaryContainer=p.ground3, onSecondaryContainer=p.flour,
        tertiary=p.vermilion, onTertiary=Color.White, tertiaryContainer=p.ground3, onTertiaryContainer=p.flour,
        background=p.ground, onBackground=p.flour, surface=p.ground2, onSurface=p.flour,
        surfaceVariant=p.ground3, onSurfaceVariant=p.flour2, outline=p.line,
        error=p.vermilion, onError=Color.White, scrim=Color(0xFF000000),
    )

private val PrototypeTypography = Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=30.4.sp,lineHeight=36.48.sp),
    headlineMedium=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=21.6.sp,lineHeight=25.92.sp),
    headlineSmall=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=17.28.sp,lineHeight=20.74.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=18.4.sp,lineHeight=22.08.sp),
    titleMedium=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=17.28.sp,lineHeight=20.74.sp),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=24.8.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.4.sp,lineHeight=22.32.sp),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.8.sp,lineHeight=19.84.sp),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.48.sp,lineHeight=16.sp),
    labelSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=14.4.sp),
)
private val PrototypeShapes = Shapes(
    extraSmall=RoundedCornerShape(4.dp), small=RoundedCornerShape(10.dp),
    medium=RoundedCornerShape(12.dp), large=RoundedCornerShape(14.dp), extraLarge=RoundedCornerShape(14.dp),
)

@Composable
fun KaalaKolamTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when(themeMode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val palette = if (dark) PrototypeTokens.Dark else PrototypeTokens.Light
    CompositionLocalProvider(LocalPrototypePalette provides palette) {
        MaterialTheme(colorScheme=scheme(palette,dark), typography=PrototypeTypography, shapes=PrototypeShapes, content=content)
    }
}
