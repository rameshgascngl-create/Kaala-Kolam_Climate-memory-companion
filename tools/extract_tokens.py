#!/usr/bin/env python3
"""Extract visual tokens from the audited HTML prototype and generate Theme.kt.

Material 3 is used only for behaviour/accessibility. Visible palette values are
taken directly from the prototype CSS; no tonal palette is generated.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
DEFAULT_OUT = ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/theme/Theme.kt"

STYLE_RE = re.compile(r"<style>(.*?)</style>", re.I | re.S)
VAR_RE = re.compile(r"--([\w-]+)\s*:\s*([^;]+);")
COLOUR_RE = re.compile(r"#[0-9a-fA-F]{3,8}\b|rgba?\([^)]*\)")
UNIT_RE = re.compile(r"(?:^|[\s,(])(-?\d*\.?\d+(?:px|rem|em|%|vw|vh))")


def css_block(css: str, pattern: str) -> str:
    match = re.search(pattern, css, re.S)
    if not match:
        raise SystemExit(f"Missing CSS block: {pattern}")
    return match.group(1)


def variables(block: str) -> dict[str, str]:
    return {m.group(1): m.group(2).strip() for m in VAR_RE.finditer(block)}


def css_values(css: str, property_pattern: str) -> list[str]:
    found: list[str] = []
    for match in re.finditer(rf"(?:{property_pattern})\s*:\s*([^;}}]+)", css, re.I):
        for unit in UNIT_RE.finditer(match.group(1)):
            value = unit.group(1)
            if value not in found:
                found.append(value)
    return sorted(
        found,
        key=lambda value: (float(re.match(r"-?\d*\.?\d+", value).group()), value),
    )


def colour(hex_value: str) -> str:
    value = hex_value.lstrip("#")
    if len(value) == 3:
        value = "".join(ch * 2 for ch in value)
    if len(value) != 6:
        raise ValueError(hex_value)
    return f"Color(0xFF{value.upper()})"


def kotlin_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def kotlin_strings(values: list[str]) -> str:
    return ", ".join(kotlin_string(v) for v in values)


def render(html: str) -> str:
    style_match = STYLE_RE.search(html)
    if not style_match:
        raise SystemExit("No <style> block found")
    css = style_match.group(1)
    dark = variables(css_block(css, r":root\s*\{(.*?)\}"))
    light_overrides = variables(
        css_block(css, r':root\[data-theme="light"\]\s*\{(.*?)\}')
    )
    light = dict(dark)
    light.update(light_overrides)

    required = {
        "ground", "ground2", "ground3", "line", "flour", "flour2", "faint",
        "turmeric", "vermilion", "sea", "ink", "s-2", "s-1", "s0", "s1", "s2",
        "font-display", "font-body", "r",
    }
    missing = sorted(required - dark.keys())
    if missing:
        raise SystemExit(f"Missing CSS variables: {missing}")

    css_colours: list[str] = []
    for match in COLOUR_RE.finditer(css):
        value = match.group(0)
        if value not in css_colours:
            css_colours.append(value)

    radii = css_values(css, r"border-radius")
    spacing = css_values(
        css,
        r"padding(?:-[\w]+)?|margin(?:-[\w]+)?|gap|row-gap|column-gap",
    )
    font_sizes = css_values(css, r"font-size")

    if dark["turmeric"].lower() != "#f0b429":
        raise SystemExit("Prototype turmeric must remain #F0B429")
    if dark["vermilion"].lower() != "#e2573a":
        raise SystemExit("Prototype vermilion must remain #E2573A")
    if dark["sea"].lower() != "#34b3ab":
        raise SystemExit("Prototype sea must remain #34B3AB")

    return f"""// GENERATED FROM Kaala_Kolam_Climate_Memory_Companion.html CSS.
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
import androidx.compose.runtime.remember
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

object PrototypeTokens {{
    const val DisplayFontStack = {kotlin_string(dark["font-display"])}
    const val BodyFontStack = {kotlin_string(dark["font-body"])}
    val CssColours = listOf({kotlin_strings(css_colours)})
    val CssRadii = listOf({kotlin_strings(radii)})
    val CssSpacing = listOf({kotlin_strings(spacing)})
    val CssFontSizes = listOf({kotlin_strings(font_sizes)})

    val BrandTurmeric = {colour(dark["turmeric"])}
    val BrandVermilion = {colour(dark["vermilion"])}
    val BrandSea = {colour(dark["sea"])}
    val ScaleColours = listOf({", ".join(colour(dark[key]) for key in ("s-2","s-1","s0","s1","s2"))})

    val Dark = PrototypePalette(
        {", ".join(f"{key} = {colour(dark[key])}" for key in ("ground","ground2","ground3","line","flour","flour2","faint","turmeric","vermilion","sea","ink"))},
    )
    val Light = PrototypePalette(
        {", ".join(f"{key} = {colour(light[key])}" for key in ("ground","ground2","ground3","line","flour","flour2","faint","turmeric","vermilion","sea","ink"))},
    )
}}

private val LocalPrototypePalette = staticCompositionLocalOf {{ PrototypeTokens.Dark }}
object PrototypeTheme {{
    val palette: PrototypePalette
        @Composable @ReadOnlyComposable get() = LocalPrototypePalette.current
}}

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

private val BundledTamilFont = FontFamily(Font(R.font.noto_sans_tamil))

@Composable
private fun prototypeTypography(): Typography {{
    val isTamil = LocalConfiguration.current.locales[0].language == "ta"
    val bodyFamily = remember(isTamil) {{
        if (isTamil) BundledTamilFont else FontFamily.SansSerif
    }}
    val displayFamily = if (isTamil) BundledTamilFont else FontFamily.Serif
    val localeList = if (isTamil) LocaleList("ta") else LocaleList("en")
    return Typography(
        headlineLarge=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=30.4.sp,lineHeight=36.48.sp),
        headlineMedium=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=21.6.sp,lineHeight=25.92.sp),
        headlineSmall=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=17.28.sp,lineHeight=20.74.sp),
        titleLarge=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=18.4.sp,lineHeight=22.08.sp),
        titleMedium=TextStyle(fontFamily=displayFamily,localeList=localeList,fontWeight=FontWeight.SemiBold,fontSize=17.28.sp,lineHeight=20.74.sp),
        bodyLarge=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontSize=16.sp,lineHeight=24.8.sp),
        bodyMedium=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontSize=14.4.sp,lineHeight=22.32.sp),
        bodySmall=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontSize=12.8.sp,lineHeight=19.84.sp),
        labelMedium=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontSize=12.48.sp,lineHeight=16.sp),
        labelSmall=TextStyle(fontFamily=bodyFamily,localeList=localeList,fontSize=12.sp,lineHeight=14.4.sp),
    )
}}
private val PrototypeShapes = Shapes(
    extraSmall=RoundedCornerShape(4.dp), small=RoundedCornerShape(10.dp),
    medium=RoundedCornerShape(12.dp), large=RoundedCornerShape(14.dp), extraLarge=RoundedCornerShape(14.dp),
)

@Composable
fun KaalaKolamTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {{
    val dark = when(themeMode) {{ ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }}
    val palette = if (dark) PrototypeTokens.Dark else PrototypeTokens.Light
    CompositionLocalProvider(LocalPrototypePalette provides palette) {{
        MaterialTheme(colorScheme=scheme(palette,dark), typography=prototypeTypography(), shapes=PrototypeShapes, content=content)
    }}
}}
"""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    generated = render(args.html.read_text(encoding="utf-8"))
    if args.check:
        current = args.output.read_text(encoding="utf-8")
        if current != generated:
            raise SystemExit("Theme.kt is stale; run tools/extract_tokens.py")
        print("TOKEN_THEME_CHECK_PASS")
        return

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(generated, encoding="utf-8")
    print("TOKEN_THEME_GENERATED")
    print(f"source={args.html}")
    print(f"output={args.output}")


if __name__ == "__main__":
    main()
