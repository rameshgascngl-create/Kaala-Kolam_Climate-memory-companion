#!/usr/bin/env python3
"""Deterministic guards for Playwright reference captures.

Every state must start from a fresh browser context at scrollY == 0. The first
state-specific visible control or heading must sit below the sticky header and
inside the viewport before a screenshot is accepted.
"""
from __future__ import annotations

from playwright.sync_api import Page

TOP_TEXT = {
    "home": {
        "en": "A climate app that starts with people, not theory",
        "ta": "கோட்பாட்டில் அல்ல, மக்களிடமிருந்து தொடங்கும் காலநிலைப் பயன்பாடு",
    },
    "learn": {"en": "Learn", "ta": "கற்றுக்கொள்ளுங்கள்"},
    "elders": {"en": "Elder interviews", "ta": "மூத்தோர் நேர்காணல்"},
    "class": {"en": "Class pool", "ta": "வகுப்புத் தொகுப்பு"},
    "council": {"en": "The Kadalur council", "ta": "கடலூர் ஊர்சபை"},
    "predict": {"en": "Predict and calibrate", "ta": "கணித்து அளவிடு"},
    "learn-topic": {"en": "← All topics", "ta": "← அனைத்துத் தலைப்புகள்"},
    "learn-deep": {"en": "← All topics", "ta": "← அனைத்துத் தலைப்புகள்"},
    "learn-game": {"en": "← Learn", "ta": "← கற்க"},
    "learn-words": {"en": "← All topics", "ta": "← அனைத்துத் தலைப்புகள்"},
}


def assert_scroll_zero(page: Page, state_name: str) -> None:
    scroll_y = page.evaluate("window.scrollY")
    if scroll_y != 0:
        raise AssertionError(
            f"REFERENCE_SCROLL_FAIL state={state_name} scrollY={scroll_y}"
        )


def assert_visible_top(page: Page, state_name: str, language: str) -> None:
    expected = TOP_TEXT[state_name][language]
    locator = page.get_by_text(expected, exact=True).first
    box = locator.bounding_box()
    if box is None:
        raise AssertionError(
            f"REFERENCE_TOP_FAIL state={state_name} lang={language} missing={expected!r}"
        )
    metrics = page.evaluate(
        """() => {
            const header = document.querySelector('header.top');
            return {
              headerBottom: header ? header.getBoundingClientRect().bottom : 0,
              viewportHeight: window.innerHeight,
              scrollY: window.scrollY,
            };
        }"""
    )
    if metrics["scrollY"] != 0:
        raise AssertionError(
            f"REFERENCE_TOP_FAIL state={state_name} lang={language} "
            f"scrollY={metrics['scrollY']}"
        )
    top = box["y"]
    bottom = box["y"] + box["height"]
    if top < metrics["headerBottom"] - 0.5:
        raise AssertionError(
            f"REFERENCE_TOP_FAIL state={state_name} lang={language} "
            f"top={top:.2f} headerBottom={metrics['headerBottom']:.2f}"
        )
    if top >= metrics["viewportHeight"] or bottom <= 0:
        raise AssertionError(
            f"REFERENCE_TOP_FAIL state={state_name} lang={language} "
            f"box=({top:.2f},{bottom:.2f}) viewport={metrics['viewportHeight']}"
        )
