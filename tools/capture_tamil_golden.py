#!/usr/bin/env python3
"""Capture Tamil HTML references for localisation stress testing.

Matrix: 10 states x 2 viewports x 2 font scales x 2 themes = 80 PNG files.
Every state uses a fresh browser context. Captures are accepted only when
window.scrollY == 0 and the state-specific top element is visibly below the
sticky header. The 200% scale remains an explicit accessibility stress case.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from playwright.sync_api import Page, sync_playwright
from reference_capture_guard import assert_scroll_zero, assert_visible_top
from reference_fonts import install_reference_fonts

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests/golden-tamil"
KEY = "kaala_kolam_v1"
VIEWPORTS = ((390, 844), (320, 568))
FONT_SCALES = (100, 200)
THEMES = ("dark", "light")
STATES = (
    "home",
    "learn",
    "elders",
    "class",
    "council",
    "predict",
    "learn-topic",
    "learn-deep",
    "learn-game",
    "learn-words",
)


def base_state(theme: str) -> dict:
    return {
        "lang": "ta",
        "theme": theme,
        "view": "home",
        "level": "all",
        "zoom": 0,
        "narr": False,
        "learned": {},
        "lmode": "home",
        "topic": None,
        "interviews": [],
        "mode": "list",
        "activeId": None,
        "qi": 0,
        "seen": {},
        "pred": {},
        "flags": {},
        "sabha": {"scn": 0, "picks": [], "note": "", "role": None, "saved": False, "id": None},
        "pool": {"imports": [], "plans": [], "sample": False, "groupBy": "all"},
    }


def state_for(name: str, theme: str) -> dict:
    state = base_state(theme)
    if name == "home":
        state["view"] = "home"
    elif name == "learn":
        state.update(view="learn", lmode="home")
    elif name == "elders":
        state.update(view="elder", mode="list")
    elif name == "class":
        state.update(view="pool")
    elif name == "council":
        state.update(view="council")
    elif name == "predict":
        state.update(view="predict")
    elif name == "learn-topic":
        state.update(view="learn", lmode="topic", topic="sun", level="all")
    elif name == "learn-deep":
        state.update(view="learn", lmode="topic", topic="sun", level="deep")
    elif name == "learn-words":
        state.update(view="learn", lmode="words")
    elif name == "learn-game":
        state.update(view="learn", lmode="home")
    else:
        raise ValueError(name)
    return state


def load(page: Page, state: dict, font_scale: int) -> None:
    page.goto(HTML.as_uri(), wait_until="load")
    page.evaluate(
        "([key,value]) => { localStorage.clear(); localStorage.setItem(key, JSON.stringify(value)); }",
        [KEY, state],
    )
    page.reload(wait_until="load")
    page.evaluate("scale => { document.documentElement.style.fontSize = scale + '%'; }", font_scale)
    install_reference_fonts(page, "ta")


def open_game(page: Page) -> None:
    page.get_by_role("button", name="வானிலையா? காலநிலையா?", exact=True).evaluate("(el) => el.click()")
    page.wait_for_function(
        "() => /1\s*\/\s*10/.test(document.querySelector('#app')?.textContent || '')"
    )


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    captures = []
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        try:
            for width, height in VIEWPORTS:
                for scale in FONT_SCALES:
                    for theme in THEMES:
                        for state_name in STATES:
                            context = browser.new_context(
                                viewport={"width": width, "height": height},
                                device_scale_factor=1,
                                reduced_motion="reduce",
                                bypass_csp=True,
                            )
                            page = context.new_page()
                            try:
                                load(page, state_for(state_name, theme), scale)
                                assert_scroll_zero(page, state_name)
                                if state_name == "learn-game":
                                    open_game(page)
                                    assert_scroll_zero(page, state_name)
                                assert_visible_top(page, state_name, "ta")
                                page.wait_for_timeout(50)
                                target = (
                                    OUT
                                    / f"{width}x{height}"
                                    / str(scale)
                                    / theme
                                    / f"{state_name}.png"
                                )
                                target.parent.mkdir(parents=True, exist_ok=True)
                                page.screenshot(
                                    path=str(target),
                                    full_page=False,
                                    animations="disabled",
                                    caret="hide",
                                    scale="css",
                                )
                                captures.append(
                                    {
                                        "state": state_name,
                                        "viewport": f"{width}x{height}",
                                        "fontScale": scale,
                                        "language": "ta",
                                        "theme": theme,
                                        "path": target.relative_to(ROOT).as_posix(),
                                        "sha256": sha(target),
                                        "scrollY": 0,
                                        "topElementVisible": True,
                                    }
                                )
                            finally:
                                context.close()
        finally:
            browser.close()
    if len(captures) != 80:
        raise SystemExit(f"Expected 80 captures, got {len(captures)}")
    (OUT / "manifest.json").write_text(
        json.dumps(
            {
                "captureCount": 80,
                "states": list(STATES),
                "viewports": [f"{w}x{h}" for w, h in VIEWPORTS],
                "fontScales": list(FONT_SCALES),
                "language": "ta",
                "themes": list(THEMES),
                "capturePolicy": "fresh-context-per-state; scrollY=0; visible-top-element",
                "captures": captures,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print("GOLDEN_TAMIL_PASS captures=80 fresh_contexts=80")


if __name__ == "__main__":
    main()
