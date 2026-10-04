#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[1]
BUNDLED = ROOT / "app/src/main/res/font/noto_sans_tamil.ttf"
TEXT_FILES = list((ROOT / "app/src/main/assets/content").glob("*.json")) + [
    ROOT / "app/src/main/res/values/strings.xml",
    ROOT / "app/src/main/res/values-ta/strings.xml",
]
TAMIL_RANGE = range(0x0B80, 0x0C00)

def codepoints(path: Path) -> set[int]:
    text = path.read_text(encoding="utf-8")
    return {ord(ch) for ch in text if ord(ch) in TAMIL_RANGE}

used = set().union(*(codepoints(path) for path in TEXT_FILES))
if not used:
    raise SystemExit("FONT_COVERAGE_FAIL: no Tamil code points found")

font = TTFont(BUNDLED)
bundled_cmap = set().union(*(table.cmap.keys() for table in font["cmap"].tables))
missing = sorted(used - bundled_cmap)
if missing:
    raise SystemExit("FONT_COVERAGE_FAIL bundled missing=" + ",".join(f"U+{cp:04X}" for cp in missing))

system_candidates = [
    Path("/system/fonts/NotoSansTamil-Regular.ttf"),
    Path("/usr/share/fonts/truetype/noto/NotoSansTamil-Regular.ttf"),
    Path("/usr/share/fonts/opentype/noto/NotoSansTamil-Regular.ttf"),
]
system = next((p for p in system_candidates if p.exists()), None)
if system is None:
    print("SYSTEM_FONT_BLOCKED no known Tamil system-font path in CI")
else:
    sys_font = TTFont(system)
    sys_cmap = set().union(*(table.cmap.keys() for table in sys_font["cmap"].tables))
    sys_missing = sorted(used - sys_cmap)
    if sys_missing:
        print("SYSTEM_FONT_INCOMPLETE path=" + str(system) + " missing=" + ",".join(f"U+{cp:04X}" for cp in sys_missing))
    else:
        print("SYSTEM_FONT_PASS path=" + str(system) + f" codepoints={len(used)}")

print(f"BUNDLED_FONT_PASS path={BUNDLED} codepoints={len(used)} bytes={BUNDLED.stat().st_size}")
