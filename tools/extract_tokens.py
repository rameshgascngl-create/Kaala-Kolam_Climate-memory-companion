#!/usr/bin/env python3
"""Extract visual and typography tokens from the audited HTML prototype.

The shipped HTML remains the visual source of truth. Material 3 supplies only
behaviour/accessibility primitives. Palette, spacing, radii and typography are
generated from the prototype CSS; no Material tonal or type substitutions are
permitted.
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
RULE_RE = re.compile(r"([^{}]+)\{([^{}]*)\}", re.S)
DECL_RE = re.compile(r"([\w-]+)\s*:\s*([^;]+)")


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


def cascade(css: str, selector: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for match in RULE_RE.finditer(css):
        selectors = [item.strip() for item in match.group(1).split(",")]
        if selector not in selectors:
            continue
        for decl in DECL_RE.finditer(match.group(2)):
            out[decl.group(1).strip().lower()] = decl.group(2).strip()
    return out


def numeric(value: str) -> float:
    match = re.match(r"-?\d*\.?\d+", value.strip())
    if not match:
        raise SystemExit(f"Expected numeric CSS value, got {value!r}")
    return float(match.group())


def to_sp(value: str, root_sp: float = 16.0) -> float:
    value = value.strip().lower()
    if value.endswith("rem"):
        return numeric(value) * root_sp
    if value.endswith("px"):
        return numeric(value)
    raise SystemExit(f"Unsupported CSS font unit: {value}")


def line_height_sp(value: str | None, font_sp: float, inherited: str = "1.55") -> float:
    raw = (value or inherited).strip().lower()
    if re.fullmatch(r"-?\d*\.?\d+", raw):
        return numeric(raw) * font_sp
    if raw.endswith(("rem", "px")):
        return to_sp(raw)
    raise SystemExit(f"Unsupported CSS line-height: {raw}")


def type_role(css: str, selector: str, inherited_line: str = "1.55") -> tuple[float, float, int]:
    decl = cascade(css, selector)
    if "font-size" not in decl:
        raise SystemExit(f"Missing font-size for {selector}")
    font = to_sp(decl["font-size"])
    line = line_height_sp(decl.get("line-height"), font, inherited_line)
    weight = int(numeric(decl.get("font-weight", "400")))
    return font, line, weight


def heading_role(css: str, selector: str) -> tuple[float, float, int]:
    shared = cascade(css, "h1")
    own = cascade(css, selector)
    font = to_sp(own["font-size"])
    line_raw = own.get("line-height", shared.get("line-height", "1.2"))
    weight_raw = own.get("font-weight", shared.get("font-weight", "600"))
    return font, line_height_sp(line_raw, font, "1.2"), int(numeric(weight_raw))


def clamp(css: str, selector: str) -> tuple[float, float, float]:
    value = cascade(css, selector).get("font-size", "")
    match = re.fullmatch(
        r"clamp\(\s*([^,]+),\s*([0-9.]+)vw\s*,\s*([^\)]+)\)",
        value,
    )
    if not match:
        raise SystemExit(f"Missing clamp font-size for {selector}: {value!r}")
    return to_sp(match.group(1)), float(match.group(2)), to_sp(match.group(3))


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


def f(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return text + "f"


def render(html: str) -> str:
    style_match = STYLE_RE.search(html)
    if not style_match:
        raise SystemExit("No <style> block found")
    css = style_match.group(1)
    dark = variables(css_block(css, r":root\s*\{(.*?)\}"))
    light_overrides = variables(css_block(css, r':root\[data-theme="light"\]\s*\{(.*?)\}'))
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
    spacing = css_values(css, r"padding(?:-[\w]+)?|margin(?:-[\w]+)?|gap|row-gap|column-gap")
    font_sizes = css_values(css, r"font-size")
    font_weights = sorted(
        {int(numeric(value)) for value in re.findall(r"font-weight\s*:\s*([^;}}]+)", css)}
    )
    line_heights = sorted(
        {value.strip() for value in re.findall(r"line-height\s*:\s*([^;}}]+)", css)}
    )

    body_decl = cascade(css, "body")
    body_font = to_sp(body_decl["font-size"])
    body_line = line_height_sp(body_decl["line-height"], body_font)
    body_weight = int(numeric(body_decl.get("font-weight", "400")))
    h1 = heading_role(css, "h1")
    h2 = heading_role(css, "h2")
    h3 = heading_role(css, "h3")
    brand = type_role(css, ".brand b")
    muted = type_role(css, ".muted")
    foot = type_role(css, ".foot")
    chip = type_role(css, ".chip")
    tabs = type_role(css, ".tabs button")
    qask = type_role(css, ".qask")
    hero_min, hero_vw, hero_max = clamp(css, ".hero h1")

    if dark["turmeric"].lower() != "#f0b429":
        raise SystemExit("Prototype turmeric must remain #F0B429")
    if dark["vermilion"].lower() != "#e2573a":
        raise SystemExit("Prototype vermilion must remain #E2573A")
    if dark["sea"].lower() != "#34b3ab":
        raise SystemExit("Prototype sea must remain #34B3AB")

    return f"""// GENERATED FROM Kaala_Kolam_Climate_Memory_Companion.html CSS.
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
) {{
    fun heroFontSp(viewportWidthDp: Int): Float =
        (viewportWidthDp * heroPreferredVw / 100f).coerceIn(heroMinSp, heroMaxSp)
}}

object PrototypeTokens {{
    const val DisplayFontStack = {kotlin_string(dark["font-display"])}
    const val BodyFontStack = {kotlin_string(dark["font-body"])}
    val CssColours = listOf({kotlin_strings(css_colours)})
    val CssRadii = listOf({kotlin_strings(radii)})
    val CssSpacing = listOf({kotlin_strings(spacing)})
    val CssFontSizes = listOf({kotlin_strings(font_sizes)})
    val CssFontWeights = listOf({", ".join(str(x) for x in font_weights)})
    val CssLineHeights = listOf({kotlin_strings(line_heights)})

    val BrandTurmeric = {colour(dark["turmeric"])}
    val BrandVermilion = {colour(dark["vermilion"])}
    val BrandSea = {colour(dark["sea"])}
    val ScaleColours = listOf({", ".join(colour(dark[key]) for key in ("s-2","s-1","s0","s1","s2"))})

    val Type = PrototypeTypographyTokens(
        bodyFontSp={f(body_font)}, bodyLineSp={f(body_line)},
        h1FontSp={f(h1[0])}, h1LineSp={f(h1[1])},
        h2FontSp={f(h2[0])}, h2LineSp={f(h2[1])},
        h3FontSp={f(h3[0])}, h3LineSp={f(h3[1])},
        brandFontSp={f(brand[0])}, brandLineSp={f(brand[1])},
        mutedFontSp={f(muted[0])}, mutedLineSp={f(muted[1])},
        footFontSp={f(foot[0])}, footLineSp={f(foot[1])},
        chipFontSp={f(chip[0])}, chipLineSp={f(chip[1])},
        tabFontSp={f(tabs[0])}, tabLineSp={f(tabs[1])},
        qaskFontSp={f(qask[0])}, qaskLineSp={f(qask[1])},
        heroMinSp={f(hero_min)}, heroPreferredVw={f(hero_vw)}, heroMaxSp={f(hero_max)},
    )

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

private fun prototypeTypography(language: String): Typography {{
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
}}

private val PrototypeShapes = Shapes(
    extraSmall=RoundedCornerShape(4.dp), small=RoundedCornerShape(10.dp),
    medium=RoundedCornerShape(12.dp), large=RoundedCornerShape(14.dp), extraLarge=RoundedCornerShape(14.dp),
)

@Composable
fun KaalaKolamTheme(
    themeMode: ThemeMode,
    language: String? = null,
    content: @Composable () -> Unit,
) {{
    val dark = when(themeMode) {{ ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }}
    val palette = if (dark) PrototypeTokens.Dark else PrototypeTokens.Light
    val effectiveLanguage = language ?: LocalConfiguration.current.locales[0].language
    CompositionLocalProvider(LocalPrototypePalette provides palette) {{
        MaterialTheme(
            colorScheme=scheme(palette,dark),
            typography=prototypeTypography(effectiveLanguage),
            shapes=PrototypeShapes,
            content=content,
        )
    }}
}}
"""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()
    html = args.html.read_text(encoding="utf-8")
    generated = render(html)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(generated, encoding="utf-8")
    print(f"TOKEN_EXTRACT_PASS {args.out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
