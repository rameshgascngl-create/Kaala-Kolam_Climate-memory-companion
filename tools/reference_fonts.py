#!/usr/bin/env python3
"""Harness-only pinned fonts for deterministic HTML reference captures.

The shipped HTML is deliberately unchanged. This module injects subsetted OFL
fonts into Playwright pages after load so Chromium and Compose use the same
font binaries and weights.
"""
from __future__ import annotations

import base64
from functools import lru_cache
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FONT_DIR = ROOT / "app/src/main/res/font"
WEIGHTS = (400, 500, 600, 700)


def _face(family: str, file_stem: str, weight: int) -> str:
    path = FONT_DIR / f"{file_stem}_{weight}_subset.ttf"
    if not path.exists():
        raise SystemExit(f"REFERENCE_FONT_MISSING {path}")
    payload = base64.b64encode(path.read_bytes()).decode("ascii")
    return (
        f"@font-face{{font-family:'{family}';"
        f"src:url(data:font/ttf;base64,{payload}) format('truetype');"
        f"font-style:normal;font-weight:{weight};font-display:block;}}"
    )


@lru_cache(maxsize=2)
def css_for(language: str) -> str:
    if language == "ta":
        display_stem = "noto_serif_tamil"
        body_stem = "noto_sans_tamil"
    else:
        display_stem = "noto_serif"
        body_stem = "noto_sans"
    parts = []
    for weight in WEIGHTS:
        parts.append(_face("KKDisplay", display_stem, weight))
        parts.append(_face("KKBody", body_stem, weight))
    parts.append(":root{--font-display:'KKDisplay';--font-body:'KKBody';}")
    return "\n".join(parts)


def install_reference_fonts(page, language: str) -> None:
    page.add_style_tag(content=css_for(language))
    page.evaluate("document.fonts ? document.fonts.ready : Promise.resolve()")
