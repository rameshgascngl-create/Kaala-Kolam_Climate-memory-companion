// GENERATED FROM Kaala_Kolam_Climate_Memory_Companion.html CSS.
// Source generator: tools/extract_tokens.py. Do not introduce Material tonal/type substitutes here.
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gascnagercoil.kaalakolam.R
import edu.gascnagercoil.kaalakolam.domain.ThemeMode

data class PrototypePalette(
    val ground: Color, val ground2: Color, val ground3: Color, val line: Color,
    val flour: Color, val flour2: Color, val faint: Color, val turmeric: Color,
    val vermilion: Color, val sea: Color, val ink: Color,
)

data class PrototypeTypographyTokens(
    val bodyFontSp: Float,
    val bodyLineSp: Float,
    val h1FontSp: Float,
    val h1LineSp: Float,
    val h2FontSp: Float,
    val h2LineSp: Float,
    val h3FontSp: Float,
    val h3LineSp: Float,
    val brandFontSp: Float,
    val brandLineSp: Float,
    val mutedFontSp: Float,
    val mutedLineSp: Float,
    val footFontSp: Float,
    val footLineSp: Float,
    val chipFontSp: Float,
    val chipLineSp: Float,
    val tabFontSp: Float,
    val tabLineSp: Float,
    val qaskFontSp: Float,
    val qaskLineSp: Float,
    val heroMinSp: Float,
    val heroPreferredVw: Float,
    val heroMaxSp: Float,
) {
    fun heroFontSp(viewportWidthDp: Int): Float =
        (viewportWidthDp * heroPreferredVw / 100f).coerceIn(heroMinSp, heroMaxSp)
}

object PrototypeTokens {
    const val DisplayFontStack = "\"Palatino Linotype\",\"Book Antiqua\",Palatino,Georgia,\"Noto Serif Tamil\",\"Latha\",serif"
    const val BodyFontStack = "system-ui,-apple-system,\"Segoe UI\",Roboto,\"Noto Sans Tamil\",\"Nirmala UI\",\"Latha\",Helvetica,Arial,sans-serif"
    val CssColours = listOf("#10263a", "#16334b", "#1d4160", "#2f5876", "#f4efe6", "#cfc9bd", "#8fa6b6", "#f0b429", "#e2573a", "#34b3ab", "#0b1a27", "#1f6f8b", "#6bb3c4", "#d8d2c4", "#eba46d", "#c8452c", "#eef3f2", "#ffffff", "#dfe9e8", "#b7ccd0", "#12303a", "#38535c", "#587783", "#9a6a00", "#b8391f", "#0f7f78", "#111", "#17120a", "rgba(240,180,41,.18)", "#000", "rgba(8,20,32,.45)", "#fff")
    val CssRadii = listOf("3px", "4px", "6px", "8px", "10px", "12px", "50%", "99px")
    val CssSpacing = listOf(".4rem", ".5rem", ".8rem", "2px", "4px", "6px", "8px", "10px", "12px", "14px", "16px", "18px", "24px", "28px", "76px")
    val CssFontSizes = listOf(".66rem", ".72rem", ".74rem", ".76rem", ".78rem", ".8rem", ".82rem", ".84rem", ".85rem", ".9rem", ".92rem", "1rem", "1.08rem", "1.1rem", "1.15rem", "1.18rem", "1.2rem", "1.3rem", "1.35rem", "1.4rem", "1.9rem", "2.2rem", "3rem", "6.5vw", "12px")
    val CssFontWeights = listOf(500, 600, 700)
    val CssLineHeights = listOf("1", "1.1", "1.2", "1.25", "1.35", "1.4", "1.45", "1.55", "1.65")

    val BrandTurmeric = Color(0xFFF0B429)
    val BrandVermilion = Color(0xFFE2573A)
    val BrandSea = Color(0xFF34B3AB)
    val ScaleColours = listOf(Color(0xFF1F6F8B), Color(0xFF6BB3C4), Color(0xFFD8D2C4), Color(0xFFEBA46D), Color(0xFFC8452C))

    val Type = PrototypeTypographyTokens(
        bodyFontSp=16f, bodyLineSp=24.8f,
        h1FontSp=30.4f, h1LineSp=36.48f,
        h2FontSp=21.6f, h2LineSp=25.92f,
        h3FontSp=17.28f, h3LineSp=20.736f,
        brandFontSp=18.4f, brandLineSp=28.52f,
        mutedFontSp=14.4f, mutedLineSp=22.32f,
        footFontSp=12.8f, footLineSp=19.84f,
        chipFontSp=12.48f, chipLineSp=19.344f,
        tabFontSp=12f, tabLineSp=18.6f,
        qaskFontSp=20.8f, qaskLineSp=28.08f,
        heroMinSp=30.4f, heroPreferredVw=6.5f, heroMaxSp=48f,
    )

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

private val NotoSerifFamily = FontFamily(
    Font(R.font.noto_serif_400_subset, FontWeight.Normal),
    Font(R.font.noto_serif_500_subset, FontWeight.Medium),
    Font(R.font.noto_serif_600_subset, FontWeight.SemiBold),
    Font(R.font.noto_serif_700_subset, FontWeight.Bold),
)
private val NotoSerifTamilFamily = FontFamily(
    Font(R.font.noto_serif_tamil_400_subset, FontWeight.Normal),
    Font(R.font.noto_serif_tamil_500_subset, FontWeight.Medium),
    Font(R.font.noto_serif_tamil_600_subset, FontWeight.SemiBold),
    Font(R.font.noto_serif_tamil_700_subset, FontWeight.Bold),
)
private val NotoSansFamily = FontFamily(
    Font(R.font.noto_sans_400_subset, FontWeight.Normal),
    Font(R.font.noto_sans_500_subset, FontWeight.Medium),
    Font(R.font.noto_sans_600_subset, FontWeight.SemiBold),
    Font(R.font.noto_sans_700_subset, FontWeight.Bold),
)
private val NotoSansTamilFamily = FontFamily(
    Font(R.font.noto_sans_tamil_400_subset, FontWeight.Normal),
    Font(R.font.noto_sans_tamil_500_subset, FontWeight.Medium),
    Font(R.font.noto_sans_tamil_600_subset, FontWeight.SemiBold),
    Font(R.font.noto_sans_tamil_700_subset, FontWeight.Bold),
)

private fun prototypeTypography(language: String): Typography {
    val isTamil = language == "ta"
    val bodyFamily = if (isTamil) NotoSansTamilFamily else NotoSansFamily
    val displayFamily = if (isTamil) NotoSerifTamilFamily else NotoSerifFamily
    val localeList = if (isTamil) LocaleList("ta") else LocaleList("en")
    val t = PrototypeTokens.Type
    return Typography(
        headlineLarge=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=t.h1FontSp.sp,lineHeight=t.h1LineSp.sp),
        headlineMedium=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=t.h2FontSp.sp,lineHeight=t.h2LineSp.sp),
        headlineSmall=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=t.h3FontSp.sp,lineHeight=t.h3LineSp.sp),
        titleLarge=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.brandFontSp.sp,lineHeight=t.brandLineSp.sp),
        titleMedium=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=t.h3FontSp.sp,lineHeight=t.h3LineSp.sp),
        bodyLarge=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.bodyFontSp.sp,lineHeight=t.bodyLineSp.sp),
        bodyMedium=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.mutedFontSp.sp,lineHeight=t.mutedLineSp.sp),
        bodySmall=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.footFontSp.sp,lineHeight=t.footLineSp.sp),
        labelMedium=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.chipFontSp.sp,lineHeight=t.chipLineSp.sp),
        labelSmall=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontWeight=FontWeight.Normal,fontSize=t.tabFontSp.sp,lineHeight=t.tabLineSp.sp),
    )
}

private val PrototypeShapes = Shapes(
    extraSmall=RoundedCornerShape(4.dp), small=RoundedCornerShape(10.dp),
    medium=RoundedCornerShape(12.dp), large=RoundedCornerShape(14.dp), extraLarge=RoundedCornerShape(14.dp),
)

@Composable
fun KaalaKolamTheme(
    themeMode: ThemeMode,
    language: String? = null,
    content: @Composable () -> Unit,
) {
    val dark = when(themeMode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val palette = if (dark) PrototypeTokens.Dark else PrototypeTokens.Light
    val effectiveLanguage = language ?: LocalConfiguration.current.locales[0].language
    val typography = prototypeTypography(effectiveLanguage)
    CompositionLocalProvider(LocalPrototypePalette provides palette) {
        MaterialTheme(
            colorScheme=scheme(palette,dark),
            typography=typography,
            shapes=PrototypeShapes,
        ) {
            ProvideTextStyle(value = typography.bodyLarge, content = content)
        }
    }
}
