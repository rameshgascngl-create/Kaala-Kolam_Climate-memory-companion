#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[1]
FONT_DIR = ROOT / "app/src/main/res/font"
TEXT_FILES = list((ROOT / "app/src/main/assets/content").glob("*.json")) + [
    ROOT / "app/src/main/res/values/strings.xml",
    ROOT / "app/src/main/res/values-ta/strings.xml",
]
KOTLIN = list((ROOT / "app/src/main/java").rglob("*.kt"))
TAMIL_RANGE = range(0x0B80, 0x0C00)
VISIBLE_LITERAL = re.compile(r'"((?:\\.|[^"\\])*)"')

def cmap(path: Path) -> set[int]:
    font = TTFont(path)
    return set().union(*(table.cmap.keys() for table in font["cmap"].tables))

def visible_codepoints() -> set[int]:
    chars: set[str] = set()
    for path in TEXT_FILES:
        chars.update(path.read_text(encoding="utf-8"))
    for path in KOTLIN:
        text = path.read_text(encoding="utf-8")
        for match in VISIBLE_LITERAL.finditer(text):
            chars.update(match.group(1))
    chars.update("க்ஷ ஶ்ரீ ஸ்ரீ கொ கௌ நந்தை பூக்கள் குழந்தைகள் CO₂ °C ± → ← – — … × ≥ ≤")
    return {ord(ch) for ch in chars if not ch.isspace()}

all_used = visible_codepoints()
tamil_used = {cp for cp in all_used if cp in TAMIL_RANGE}
if not tamil_used:
    raise SystemExit("FONT_COVERAGE_FAIL: no Tamil code points found")

expected = []
for family in ("noto_serif", "noto_serif_tamil", "noto_sans", "noto_sans_tamil"):
    for weight in (400, 500, 600, 700):
        path = FONT_DIR / f"{family}_{weight}_subset.ttf"
        if not path.exists():
            raise SystemExit(f"FONT_COVERAGE_FAIL missing={path}")
        expected.append(path)

for path in [p for p in expected if "_tamil_" in p.name]:
    missing = sorted(tamil_used - cmap(path))
    if missing:
        raise SystemExit(
            f"FONT_COVERAGE_FAIL {path.name} Tamil missing="
            + ",".join(f"U+{cp:04X}" for cp in missing)
        )

display_shared = {ord(ch) for ch in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 Aa+-/().,:;!?%–—…"}
body_shared = display_shared | {ord(ch) for ch in "°←→₂±×≥≤"}
for path in expected:
    required = body_shared if "noto_sans" in path.name else display_shared
    missing = sorted(required - cmap(path))
    if missing:
        raise SystemExit(
            f"FONT_COVERAGE_FAIL {path.name} role missing="
            + ",".join(f"U+{cp:04X}" for cp in missing)
        )

total = sum(path.stat().st_size for path in expected)
print(f"PINNED_FONT_COVERAGE_PASS files={len(expected)} tamil_codepoints={len(tamil_used)} bytes={total}")
