#!/usr/bin/env python3
"""Capture deterministic Chromium text widths with the pinned harness fonts."""
from __future__ import annotations

import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from reference_fonts import install_reference_fonts

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests/reference-metrics/text-widths.json"

SAMPLES = [
    {"id":"en-display","lang":"en","role":"display","weight":600,"size":30.4,
     "text":"Weather or climate?"},
    {"id":"en-body","lang":"en","role":"body","weight":400,"size":16.0,
     "text":"It rained heavily in my town last night."},
    {"id":"ta-display","lang":"ta","role":"display","weight":600,"size":30.4,
     "text":"வானிலையா? காலநிலையா?"},
    {"id":"ta-body","lang":"ta","role":"body","weight":400,"size":16.0,
     "text":"நேற்றிரவு என் ஊரில் கனமழை பெய்தது."},
]


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    results = []
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        try:
            for sample in SAMPLES:
                context = browser.new_context(viewport={"width": 800, "height": 300}, bypass_csp=True)
                page = context.new_page()
                try:
                    page.goto(HTML.as_uri(), wait_until="load")
                    install_reference_fonts(page, sample["lang"])
                    family = "KKDisplay" if sample["role"] == "display" else "KKBody"
                    page.evaluate(
                        """sample => {
                            document.body.innerHTML = '';
                            const span = document.createElement('span');
                            span.id = 'probe';
                            span.textContent = sample.text;
                            Object.assign(span.style, {
                              position: 'absolute',
                              left: '0px',
                              top: '0px',
                              whiteSpace: 'pre',
                              fontFamily: sample.family,
                              fontWeight: String(sample.weight),
                              fontSize: sample.size + 'px',
                              lineHeight: 'normal',
                              letterSpacing: '0px',
                              fontKerning: 'normal'
                            });
                            document.body.appendChild(span);
                        }""",
                        {**sample, "family": family},
                    )
                    page.evaluate("document.fonts ? document.fonts.ready : Promise.resolve()")
                    width = page.locator("#probe").evaluate("el => el.getBoundingClientRect().width")
                    canvas_width = page.evaluate(
                        """sample => {
                            const canvas = document.createElement('canvas');
                            const ctx = canvas.getContext('2d');
                            ctx.font = sample.weight + ' ' + sample.size + 'px ' + sample.family;
                            return ctx.measureText(sample.text).width;
                        }""",
                        {**sample, "family": family},
                    )
                    computed = page.locator("#probe").evaluate(
                        "el => getComputedStyle(el).fontFamily"
                    )
                    loaded = page.evaluate(
                        """sample => document.fonts.check(
                            sample.weight + ' ' + sample.size + 'px "' + sample.family + '"',
                            sample.text
                        )""",
                        {**sample, "family": family},
                    )
                    cdp = context.new_cdp_session(page)
                    cdp.send("DOM.enable")
                    cdp.send("CSS.enable")
                    root = cdp.send("DOM.getDocument")["root"]["nodeId"]
                    node_id = cdp.send(
                        "DOM.querySelector", {"nodeId": root, "selector": "#probe"}
                    )["nodeId"]
                    platform_fonts = cdp.send(
                        "CSS.getPlatformFontsForNode", {"nodeId": node_id}
                    )["fonts"]
                    platform = platform_fonts[0] if platform_fonts else {}
                    record = {
                        **sample,
                        "cssFamily": family,
                        "computedFamily": computed,
                        "fontLoaded": bool(loaded),
                        "widthPx": float(width),
                        "canvasWidthPx": float(canvas_width),
                        "platformFamily": platform.get("familyName"),
                        "postScriptName": platform.get("postScriptName"),
                        "isCustomFont": platform.get("isCustomFont"),
                        "glyphCount": platform.get("glyphCount"),
                    }
                    results.append(record)
                    print(
                        "CHROMIUM_TEXT_WIDTH "
                        f"sample={sample['id']} dom_width={width:.4f} canvas_width={canvas_width:.4f} "
                        f"css={family} resolved={record['platformFamily']} "
                        f"postscript={record['postScriptName']} custom={record['isCustomFont']}"
                    )
                    if not loaded or not record["isCustomFont"]:
                        raise SystemExit(f"CHROMIUM_FONT_RESOLUTION_FAIL {sample['id']}")
                finally:
                    context.close()
        finally:
            browser.close()
    OUT.write_text(json.dumps({"samples": results}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(f"CHROMIUM_TEXT_WIDTH_PASS samples={len(results)}")


if __name__ == "__main__":
    main()
