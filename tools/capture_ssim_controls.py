#!/usr/bin/env python3
"""Create a rendered SSIM negative control with exactly one extra wrapped line."""
from __future__ import annotations
import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from reference_capture_guard import assert_scroll_zero, assert_visible_top
from reference_fonts import install_reference_fonts

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests/ssim-controls"
KEY = "kaala_kolam_v1"


def state() -> dict:
    return {
        "lang":"en","theme":"dark","view":"home","level":"all","zoom":0,"narr":False,
        "learned":{},"lmode":"home","topic":None,"interviews":[],"mode":"list",
        "activeId":None,"qi":0,"seen":{},"pred":{},"flags":{},
        "sabha":{"scn":0,"picks":[],"note":"","role":None,"saved":False,"id":None},
        "pool":{"imports":[],"plans":[],"sample":False,"groupBy":"all"},
    }


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        context = browser.new_context(
            viewport={"width":390,"height":844},
            device_scale_factor=1,
            reduced_motion="reduce",
            bypass_csp=True,
        )
        page = context.new_page()
        try:
            page.goto(HTML.as_uri(), wait_until="load")
            page.evaluate(
                "([key,value]) => { localStorage.clear(); localStorage.setItem(key, JSON.stringify(value)); }",
                [KEY, state()],
            )
            page.reload(wait_until="load")
            install_reference_fonts(page, "en")
            assert_scroll_zero(page, "home")
            assert_visible_top(page, "home", "en")
            result = page.evaluate(
                """() => {
                    const p = document.querySelector('#app p');
                    if (!p) throw new Error('No home paragraph found');
                    const lineCount = (el) => {
                      const range = document.createRange();
                      range.selectNodeContents(el);
                      const ys = [...range.getClientRects()]
                        .filter(r => r.width > 0 && r.height > 0)
                        .map(r => Math.round(r.top * 2) / 2);
                      return [...new Set(ys)].length;
                    };
                    const baseLines = lineCount(p);
                    const originalWidth = p.getBoundingClientRect().width;
                    let selected = null;
                    for (let width = Math.floor(originalWidth) - 1;
                         width >= Math.floor(originalWidth * 0.55); width -= 1) {
                      p.style.width = width + 'px';
                      p.style.maxWidth = width + 'px';
                      const lines = lineCount(p);
                      if (lines === baseLines + 1) {
                        selected = {baseLines, newLines: lines, originalWidth, width};
                        break;
                      }
                    }
                    if (!selected) {
                      throw new Error('Could not force exactly one additional wrapped line');
                    }
                    return selected;
                }"""
            )
            assert_scroll_zero(page, "home")
            target = OUT / "home-extra-wrap.png"
            page.screenshot(path=str(target), full_page=False, animations="disabled", caret="hide", scale="css")
            (OUT / "home-extra-wrap.json").write_text(
                json.dumps(result, indent=2)+"\n", encoding="utf-8"
            )
            print(
                "SSIM_EXTRA_WRAP_CAPTURE "
                f"base_lines={result['baseLines']} new_lines={result['newLines']} "
                f"width={result['originalWidth']:.2f}->{result['width']}"
            )
        finally:
            context.close()
            browser.close()


if __name__ == "__main__":
    main()
