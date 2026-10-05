#!/usr/bin/env python3
"""Capture deterministic web-prototype visual references with Playwright.

Matrix:
  9 states x 2 viewports x 2 languages x 2 themes = 72 PNG files.

Each state uses a fresh browser context. Captures are accepted only when
window.scrollY == 0 and the state-specific top element is visibly below the
sticky header. The shipped HTML is never modified.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from playwright.sync_api import Page, sync_playwright
from reference_capture_guard import assert_scroll_zero, assert_visible_top
from reference_fonts import install_reference_fonts

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests" / "golden-web"
KEY = "kaala_kolam_v1"

VIEWPORTS = ((390, 844), (320, 568))
LANGUAGES = ("en", "ta")
THEMES = ("dark", "light")
STATES = (
    "home",
    "learn",
    "elders",
    "class",
    "council",
    "predict",
    "learn-topic",
    "learn-game",
    "learn-words",
)


def base_state(language: str, theme: str) -> dict:
    return {
        "lang": language,
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
        "sabha": {
            "scn": 0,
            "picks": [],
            "note": "",
            "role": None,
            "saved": False,
            "id": None,
        },
        "pool": {"imports": [], "plans": [], "sample": False, "groupBy": "all"},
    }


def state_for(name: str, language: str, theme: str) -> dict:
    state = base_state(language, theme)
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
    elif name == "learn-words":
        state.update(view="learn", lmode="words")
    elif name == "learn-game":
        state.update(view="learn", lmode="home")
    else:
        raise ValueError(name)
    return state


def load_state(page: Page, state: dict) -> None:
    page.goto(HTML.as_uri(), wait_until="load")
    page.evaluate(
        """([key, value]) => {
            localStorage.clear();
            localStorage.setItem(key, JSON.stringify(value));
        }""",
        [KEY, state],
    )
    page.reload(wait_until="load")
    install_reference_fonts(page, state["lang"])


def open_game(page: Page, language: str) -> None:
    label = "வானிலையா? காலநிலையா?" if language == "ta" else "Weather or climate?"
    page.get_by_role("button", name=label, exact=True).evaluate("(el) => el.click()")
    page.wait_for_function(
        """() => {
            const app = document.querySelector('#app');
            return app && /1\s*\/\s*10/.test(app.textContent || '');
        }"""
    )


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def capture(output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    manifest: list[dict] = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        try:
            for width, height in VIEWPORTS:
                for language in LANGUAGES:
                    for theme in THEMES:
                        for state_name in STATES:
                            context = browser.new_context(
                                viewport={"width": width, "height": height},
                                device_scale_factor=1,
                                color_scheme="dark",
                                reduced_motion="reduce",
                                bypass_csp=True,
                            )
                            page = context.new_page()
                            try:
                                state = state_for(state_name, language, theme)
                                load_state(page, state)
                                assert_scroll_zero(page, state_name)
                                if state_name == "learn-game":
                                    open_game(page, language)
                                    assert_scroll_zero(page, state_name)
                                assert_visible_top(page, state_name, language)
                                page.wait_for_timeout(50)

                                target = (
                                    output
                                    / f"{width}x{height}"
                                    / language
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
                                manifest.append(
                                    {
                                        "state": state_name,
                                        "viewport": f"{width}x{height}",
                                        "language": language,
                                        "theme": theme,
                                        "path": target.relative_to(ROOT).as_posix(),
                                        "sha256": sha256(target),
                                        "scrollY": 0,
                                        "topElementVisible": True,
                                    }
                                )
                            finally:
                                context.close()
        finally:
            browser.close()

    expected = len(STATES) * len(VIEWPORTS) * len(LANGUAGES) * len(THEMES)
    if len(manifest) != expected:
        raise SystemExit(f"Expected {expected} captures, got {len(manifest)}")
    (output / "manifest.json").write_text(
        json.dumps(
            {
                "source": HTML.name,
                "captureCount": len(manifest),
                "states": list(STATES),
                "viewports": [f"{w}x{h}" for w, h in VIEWPORTS],
                "languages": list(LANGUAGES),
                "themes": list(THEMES),
                "capturePolicy": "fresh-context-per-state; scrollY=0; visible-top-element",
                "captures": manifest,
            },
            indent=2,
            ensure_ascii=False,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"GOLDEN_WEB_PASS captures={len(manifest)} fresh_contexts={len(manifest)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=OUT)
    args = parser.parse_args()
    capture(args.output)


if __name__ == "__main__":
    main()
