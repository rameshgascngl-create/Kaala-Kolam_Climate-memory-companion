#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java"
THEME = JAVA / "edu/gascnagercoil/kaalakolam/ui/theme/Theme.kt"

def fail(message: str) -> None:
    raise SystemExit("TAMIL_TYPOGRAPHY_FAIL: " + message)

def call_block(lines: list[str], start: int) -> str:
    """Return only the balanced Kotlin call that starts on the Text( line."""
    block: list[str] = []
    depth = 0
    started = False
    for line in lines[start:]:
        block.append(line)
        structural = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
        if not started:
            marker = structural.find("Text(")
            if marker < 0:
                marker = structural.find("Text (")
            if marker >= 0:
                structural = structural[marker:]
                started = True
        if started:
            depth += structural.count("(") - structural.count(")")
            if depth <= 0:
                break
    return "\n".join(block)

sources = {path: path.read_text(encoding="utf-8") for path in sorted(JAVA.rglob("*.kt"))}
joined = "\n".join(sources.values())
if ".uppercase(" in joined:
    fail("visible-source policy forbids .uppercase()")
if "TextOverflow.Ellipsis" in joined:
    fail("titles, labels and buttons must not ellipsise")
for path, text in sources.items():
    for match in re.finditer(r"letterSpacing\s*=\s*([^,\n)]+)", text):
        value = match.group(1).strip()
        if value not in {"0.sp", "0.em", "TextUnit.Unspecified"}:
            fail(f"{path}: non-zero letter spacing {value}")

for path, text in sources.items():
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if re.search(r"\bText\s*\(", line):
            block = call_block(lines, index)
            if re.search(r"Modifier[\s\S]*?\.height\(", block):
                fail(f"{path}:{index+1}: fixed-height Text container")

theme = THEME.read_text(encoding="utf-8")
pairs = {
    "body": ("bodyFontSp", "bodyLineSp"),
    "muted": ("mutedFontSp", "mutedLineSp"),
    "foot": ("footFontSp", "footLineSp"),
}
for label, (font_key, line_key) in pairs.items():
    font_match = re.search(rf"\b{font_key}=([0-9.]+)f", theme)
    line_match = re.search(rf"\b{line_key}=([0-9.]+)f", theme)
    if not font_match or not line_match:
        fail(f"could not read generated {label} font/line-height tokens")
    font = float(font_match.group(1))
    line = float(line_match.group(1))
    if line + 1e-6 < font * 1.5:
        fail(f"{label}: line height {line} is less than 1.5 x {font}")

for token in ("h1FontSp", "h1LineSp", "h2FontSp", "h2LineSp", "h3FontSp", "h3LineSp",
              "heroMinSp", "heroPreferredVw", "heroMaxSp", "tabFontSp", "tabLineSp"):
    if not re.search(rf"\b{token}=[0-9.]+f", theme):
        fail(f"generated typography token missing: {token}")

if "NotoSerifTamilFamily" not in theme or "NotoSansTamilFamily" not in theme:
    fail("Tamil display/body pinned families missing")
if "NotoSerifFamily" not in theme or "NotoSansFamily" not in theme:
    fail("English display/body pinned families missing")

print("TAMIL_TYPOGRAPHY_PASS generated-css-tokens body_line_height>=1.5 serif-display sans-body no-uppercase no-ellipsis no-fixed-text-height")
