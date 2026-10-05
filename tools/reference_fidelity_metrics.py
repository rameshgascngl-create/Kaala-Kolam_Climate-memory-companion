#!/usr/bin/env python3
"""Capture reference geometry/line-count metrics with harness-only tags.

The shipped HTML is not changed. data-fidelity-tag attributes are injected only
inside the Playwright page at measurement time.
"""
from __future__ import annotations

import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from reference_capture_guard import assert_scroll_zero, assert_visible_top
from reference_fonts import install_reference_fonts

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests/reference-metrics/fidelity-layout.json"
KEY = "kaala_kolam_v1"
WIDTHS = (320, 390)
HEIGHT_FOR_WIDTH = {320: 568, 390: 844}
LANGS = ("en", "ta")
STATES = ("home", "learn", "elders", "class", "council", "predict")
TAB_ROUTES = ("home", "learn", "elders", "class", "council", "predict")

PRIMARY = {
    "home": {"en": "Start an interview", "ta": "நேர்காணலைத் தொடங்கு"},
    "learn": {"en": "Tamil–English word list", "ta": "தமிழ்–ஆங்கிலச் சொற்பட்டி"},
    "elders": {"en": "Start an interview", "ta": "நேர்காணலைத் தொடங்கு"},
    "class": {"en": "Add", "ta": "சேர்"},
    "council": {"en": "Deal me a role", "ta": "ஒரு பாத்திரம் வழங்கு"},
    "predict": {"en": "Lock in and reveal", "ta": "பூட்டி விடையைப் பார்"},
}


def base_state(language: str) -> dict:
    return {
        "lang": language, "theme": "dark", "view": "home", "level": "all",
        "zoom": 0, "narr": False, "learned": {}, "lmode": "home",
        "topic": None, "interviews": [], "mode": "list", "activeId": None,
        "qi": 0, "seen": {}, "pred": {}, "flags": {},
        "sabha": {"scn": 0, "picks": [], "note": "", "role": None, "saved": False, "id": None},
        "pool": {"imports": [], "plans": [], "sample": False, "groupBy": "all"},
    }


def state_for(name: str, language: str) -> dict:
    state = base_state(language)
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
    else:
        raise ValueError(name)
    return state


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    rows = []
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        try:
            for width in WIDTHS:
                height = HEIGHT_FOR_WIDTH[width]
                for lang in LANGS:
                    for state_name in STATES:
                        context = browser.new_context(
                            viewport={"width": width, "height": height},
                            device_scale_factor=1,
                            reduced_motion="reduce",
                            bypass_csp=True,
                        )
                        page = context.new_page()
                        try:
                            page.goto(HTML.as_uri(), wait_until="load")
                            state = state_for(state_name, lang)
                            page.evaluate(
                                "([k,v]) => { localStorage.clear(); localStorage.setItem(k, JSON.stringify(v)); }",
                                [KEY, state],
                            )
                            page.reload(wait_until="load")
                            install_reference_fonts(page, lang)
                            assert_scroll_zero(page, state_name)
                            assert_visible_top(page, state_name, lang)

                            tagged = page.evaluate(
                                """args => {
                                    const tags = [];
                                    const put = (el, tag, kind, interactive=false) => {
                                      if (!el) return;
                                      el.setAttribute('data-fidelity-tag', tag);
                                      tags.push({tag, kind, interactive});
                                    };
                                    put(document.querySelector('header.top'), 'chrome.topbar', 'box');
                                    put(document.querySelector('header .brand'), 'chrome.brand', 'box', true);
                                    put(document.querySelector('header .brand b'), 'chrome.brand-label', 'label');
                                    [...document.querySelectorAll('header .tool')].forEach((el, i) => {
                                      put(el, ['tool.text-size','tool.language','tool.theme'][i], 'button', true);
                                    });
                                    put(document.querySelector('nav.tabs'), 'chrome.tabbar', 'box');
                                    [...document.querySelectorAll('nav.tabs button')].forEach((el, i) => {
                                      put(el, 'tab.' + args.tabRoutes[i], 'box', true);
                                      el.setAttribute('data-fidelity-label-tag', 'tab-label.' + args.tabRoutes[i]);
                                    });
                                    put(document.querySelector('main h1'), 'screen.heading', 'heading');
                                    put(document.querySelector('main p'), 'screen.body.primary', 'body');
                                    put(document.querySelector('main .card'), 'screen.first-card', 'box');
                                    const primary = [...document.querySelectorAll('main button')]
                                      .find(el => (el.textContent || '').trim() === args.primary);
                                    put(primary, 'screen.primary-button', 'button', true);
                                    return tags;
                                }""",
                                {"tabRoutes": list(TAB_ROUTES), "primary": PRIMARY[state_name][lang]},
                            )

                            metrics = page.evaluate(
                                """() => {
                                    const textLines = el => {
                                      const ys = [];
                                      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
                                      while (walker.nextNode()) {
                                        const node = walker.currentNode;
                                        if (!(node.nodeValue || '').trim()) continue;
                                        const range = document.createRange();
                                        range.selectNodeContents(node);
                                        for (const rect of range.getClientRects()) {
                                          if (rect.width > 0 && rect.height > 0) {
                                            ys.push(Math.round(rect.top * 2) / 2);
                                          }
                                        }
                                      }
                                      return [...new Set(ys)].length;
                                    };
                                    const out = [];
                                    for (const el of document.querySelectorAll('[data-fidelity-tag]')) {
                                      const r = el.getBoundingClientRect();
                                      const tag = el.getAttribute('data-fidelity-tag');
                                      const kind = (() => {
                                        if (tag === 'screen.heading') return 'heading';
                                        if (tag === 'screen.body.primary') return 'body';
                                        if (tag === 'chrome.brand-label') return 'label';
                                        if (tag.startsWith('tool.') || tag === 'screen.primary-button') return 'button';
                                        return 'box';
                                      })();
                                      const cs = getComputedStyle(el);
                                      const clips = value => ['hidden','clip','auto','scroll'].includes(value);
                                      out.push({
                                        tag, kind,
                                        text: (el.textContent || '').trim(),
                                        left: r.left, top: r.top, width: r.width, height: r.height,
                                        lineCount: kind === 'box' ? null : textLines(el),
                                        overflowX: clips(cs.overflowX) && el.scrollWidth > el.clientWidth + 1,
                                        overflowY: clips(cs.overflowY) && el.scrollHeight > el.clientHeight + 1,
                                      });
                                    }
                                    for (const el of document.querySelectorAll('[data-fidelity-label-tag]')) {
                                      const r = el.getBoundingClientRect();
                                      out.push({
                                        tag: el.getAttribute('data-fidelity-label-tag'),
                                        kind: 'label',
                                        text: (el.textContent || '').trim(),
                                        left: r.left, top: r.top, width: r.width, height: r.height,
                                        lineCount: textLines(el),
                                        overflowX: false,
                                        overflowY: false,
                                      });
                                    }
                                    return out;
                                }"""
                            )
                            rows.append({
                                "viewport": f"{width}x{height}",
                                "widthDp": width,
                                "language": lang,
                                "state": state_name,
                                "metrics": metrics,
                            })
                            print(
                                f"REFERENCE_FIDELITY_METRICS state={state_name} "
                                f"lang={lang} width={width} tags={len(metrics)}"
                            )
                        finally:
                            context.close()
        finally:
            browser.close()
    OUT.write_text(
        json.dumps(
            {
                "schemaVersion": 1,
                "source": HTML.name,
                "capturePolicy": "runtime-only data-fidelity-tag; shipped HTML unchanged",
                "rows": rows,
            },
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    print(f"REFERENCE_FIDELITY_METRICS_PASS rows={len(rows)}")


if __name__ == "__main__":
    main()
