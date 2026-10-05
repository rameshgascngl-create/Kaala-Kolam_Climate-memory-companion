#!/usr/bin/env python3
"""Vendor deterministic, subsetted Noto fonts for native and reference parity.

Sources are pinned to exact Google Fonts Git blobs and licensed under OFL-1.1.
The shipped HTML is never modified. The generated Android TTFs are static
instances at the CSS weights used by the prototype: 400, 500, 600 and 700.
"""
from __future__ import annotations

import hashlib
import re
import urllib.request
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/font"
LICENSES = ROOT / "docs/licenses"
CACHE = ROOT / "build/font-source"

FAMILIES = {
    "noto_serif": {
        "url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoserif/NotoSerif%5Bwdth%2Cwght%5D.ttf",
        "git_blob": "7664d59c95d6c589f7b15017a218f7a42b93f86d",
        "license_url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoserif/OFL.txt",
        "license_out": "OFL-Noto-Latin.txt",
    },
    "noto_serif_tamil": {
        "url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoseriftamil/NotoSerifTamil%5Bwdth%2Cwght%5D.ttf",
        "git_blob": "13d6ef318e91492cb688ff12357321645e19139d",
        "license_url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoseriftamil/OFL.txt",
        "license_out": "OFL-Noto-Tamil.txt",
    },
    "noto_sans": {
        "url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notosans/NotoSans%5Bwdth%2Cwght%5D.ttf",
        "git_blob": "75575046c015ff623a848096a15779867ba71453",
        "license_url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notosans/OFL.txt",
        "license_out": "OFL-Noto-Latin.txt",
    },
    "noto_sans_tamil": {
        "url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanstamil/NotoSansTamil%5Bwdth%2Cwght%5D.ttf",
        "git_blob": "cb08d499b2d05ef6fd8e470c36c2be47ff368f0d",
        "license_url": "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanstamil/OFL.txt",
        "license_out": "OFL-Noto-Tamil.txt",
    },
}
WEIGHTS = (400, 500, 600, 700)
TEXT_SUFFIXES = {".html", ".kt", ".xml", ".json", ".md", ".py", ".kts", ".yml", ".yaml"}


def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def fetch(url: str, target: Path, expected_blob: str | None = None) -> bytes:
    target.parent.mkdir(parents=True, exist_ok=True)
    data = urllib.request.urlopen(url, timeout=60).read()
    if expected_blob is not None:
        actual = git_blob_sha(data)
        if actual != expected_blob:
            raise SystemExit(f"FONT_SOURCE_MISMATCH {target.name}: {actual} != {expected_blob}")
    target.write_bytes(data)
    return data


def corpus_codepoints() -> set[int]:
    chars: set[str] = set()
    roots = [
        ROOT / "Kaala_Kolam_Climate_Memory_Companion.html",
        ROOT / "app/src/main",
        ROOT / "app/src/test",
        ROOT / "tools",
        ROOT / "docs",
    ]
    for root in roots:
        paths = [root] if root.is_file() else root.rglob("*")
        for path in paths:
            if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
                continue
            try:
                chars.update(path.read_text(encoding="utf-8"))
            except UnicodeDecodeError:
                continue
    # Explicit hard-gate stress characters and common scientific notation.
    chars.update("க்ஷ ஶ்ரீ ஸ்ரீ கொ கௌ நந்தை பூக்கள் குழந்தைகள் CO₂ °C ± → ← – — … × ≥ ≤")
    return {ord(ch) for ch in chars if ch not in {"\r"}}


def make_static_subset(source: Path, target: Path, weight: int, unicodes: set[int]) -> None:
    font = TTFont(source, recalcTimestamp=False)
    static_font = instantiateVariableFont(
        font,
        {"wght": float(weight), "wdth": 100.0},
        inplace=False,
    )
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.name_legacy = True
    options.name_languages = ["*"]
    options.notdef_glyph = True
    options.notdef_outline = True
    options.recalc_timestamp = False
    options.recommended_glyphs = True
    subsetter = subset.Subsetter(options=options)
    subsetter.populate(unicodes=unicodes)
    subsetter.subset(static_font)
    target.parent.mkdir(parents=True, exist_ok=True)
    static_font.save(target, reorderTables=True)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    LICENSES.mkdir(parents=True, exist_ok=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    unicodes = corpus_codepoints()
    source_paths: dict[str, Path] = {}

    for family, spec in FAMILIES.items():
        source = CACHE / f"{family}.ttf"
        fetch(spec["url"], source, spec["git_blob"])
        source_paths[family] = source
        license_path = LICENSES / spec["license_out"]
        if not license_path.exists():
            fetch(spec["license_url"], license_path)

    total = 0
    for family, source in source_paths.items():
        for weight in WEIGHTS:
            target = OUT / f"{family}_{weight}_subset.ttf"
            make_static_subset(source, target, weight, unicodes)
            size = target.stat().st_size
            total += size
            print(f"FONT_SUBSET {target.relative_to(ROOT)} weight={weight} bytes={size}")

    print(f"FONT_VENDOR_PASS files={len(FAMILIES) * len(WEIGHTS)} bytes={total} codepoints={len(unicodes)}")


if __name__ == "__main__":
    main()
