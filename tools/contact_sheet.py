#!/usr/bin/env python3
"""Build the 390x844 EN/TA prototype-vs-native contact sheet.

The sheet is evidence only. It never changes a parity baseline.
"""
from __future__ import annotations

import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / "tests/golden-web"
NATIVE = ROOT / "app/build/visual-native"
FIDELITY = ROOT / "app/build/fidelity"
OUT = FIDELITY / "contact-sheet-390x844.png"
MANIFEST = FIDELITY / "contact-sheet-390x844.json"
STATES = ("home","learn","elders","class","council","predict")
LANGS = ("en","ta")


def load_report(name: str) -> dict:
    path = FIDELITY / name
    if not path.exists():
        return {"passed": None, "failures": []}
    return json.loads(path.read_text(encoding="utf-8"))


def screen_failed(report: dict, lang: str, state: str) -> bool | None:
    if report.get("passed") is None:
        return None
    failures = report.get("failures", [])
    for failure in failures:
        text = str(failure)
        if state in text and lang in text and ("390" in text or "390x844" in text):
            return True
    return False


def gate_label(value: bool | None) -> str:
    if value is None:
        return "NOT RUN"
    return "FAIL" if value else "PASS"


def main() -> None:
    hard = load_report("hard-report.json")
    parity = load_report("parity-report.json")
    rows = []
    records = []
    label_h = 64
    pair_w = 390 * 2
    for lang in LANGS:
        for state in STATES:
            ref = REF / "390x844" / lang / "dark" / f"{state}.png"
            native = NATIVE / "390x844" / lang / "dark" / f"{state}.png"
            if not ref.exists() or not native.exists():
                raise SystemExit(f"CONTACT_SHEET_FAIL missing ref={ref.exists()} native={native.exists()} {lang}/{state}")
            with Image.open(ref) as a, Image.open(native) as b:
                left = a.convert("RGB")
                right = b.convert("RGB")
                if left.size != (390,844) or right.size != (390,844):
                    raise SystemExit(f"CONTACT_SHEET_FAIL size {lang}/{state} ref={left.size} native={right.size}")
                hard_fail = screen_failed(hard,lang,state)
                parity_fail = screen_failed(parity,lang,state)
                row = Image.new("RGB",(pair_w,844+label_h),"white")
                row.paste(left,(0,label_h))
                row.paste(right,(390,label_h))
                draw=ImageDraw.Draw(row)
                draw.text((8,8),f"{lang.upper()} · {state} · prototype",fill="black")
                draw.text((398,8),f"{lang.upper()} · {state} · native",fill="black")
                draw.text((8,34),f"HARD {gate_label(hard_fail)} · PARITY {gate_label(parity_fail)}",fill="black")
                draw.line((390,0,390,844+label_h),fill="black",width=1)
                rows.append(row)
                records.append({
                    "language":lang,"state":state,
                    "hard":gate_label(hard_fail),"parity":gate_label(parity_fail),
                    "reference":ref.relative_to(ROOT).as_posix(),
                    "native":native.relative_to(ROOT).as_posix(),
                })
    sheet=Image.new("RGB",(pair_w,sum(r.height for r in rows)),"white")
    y=0
    for row in rows:
        sheet.paste(row,(0,y)); y+=row.height
    OUT.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(OUT,optimize=True)
    MANIFEST.write_text(json.dumps({"rows":records},indent=2)+"\n",encoding="utf-8")
    print(f"CONTACT_SHEET_PASS rows={len(records)} output={OUT}")


if __name__=="__main__":
    main()
