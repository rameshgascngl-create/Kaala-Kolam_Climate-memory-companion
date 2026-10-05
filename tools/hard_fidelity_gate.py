#!/usr/bin/env python3
"""Enforced HARD fidelity gate.

Zero failures are permitted. It checks structural typography, overflow,
tab-label fit and semantics-node touch bounds against Playwright reference
metrics at 320 and 390 dp.
"""
from __future__ import annotations
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REF=ROOT/"tests/reference-metrics/fidelity-layout.json"
NATIVE=ROOT/"app/build/fidelity/native-layout"
TAB_NATIVE=ROOT/"app/build/fidelity/tab-hard"
REPORT=ROOT/"app/build/fidelity/hard-report.json"
ROUTES=("home","learn","elders","class","council","predict")


def write_report(msgs: list[str], checks: int, rows: int) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    by_screen={}
    for msg in msgs:
        key=msg.split(":",1)[0]
        by_screen.setdefault(key,[]).append(msg)
    REPORT.write_text(json.dumps({
        "gate":"HARD","passed":not msgs,"failureCount":len(msgs),
        "checks":checks,"rows":rows,"failures":msgs,"byScreen":by_screen,
    },ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

def fail(msgs: list[str], checks: int, rows: int) -> None:
    write_report(msgs,checks,rows)
    print(f"HARD_FIDELITY_FAIL count={len(msgs)}")
    for msg in msgs:
        print("HARD_FAIL",msg)
    raise SystemExit(1)


def load_native():
    out={}
    for path in sorted(NATIVE.glob("*/*/*.json")):
        data=json.loads(path.read_text(encoding="utf-8"))
        out[(data["widthDp"],data["language"],data["state"])]=data
    return out


def load_tab_native():
    out={}
    for path in sorted(TAB_NATIVE.glob("*/*/*.json")):
        data=json.loads(path.read_text(encoding="utf-8"))
        out[(data["widthDp"],data["language"],data["fontScalePercent"])]=data
    return out


ref=json.loads(REF.read_text(encoding="utf-8"))["rows"]
native=load_native()
tab_native=load_tab_native()
errors=[]
checks=0

for row in ref:
    key=(row["widthDp"],row["language"],row["state"])
    got=native.get(key)
    if got is None:
        errors.append(f"{key}: native metric file missing")
        continue
    boxes={x["tag"]:x for x in got["boxes"]}
    texts={x["tag"]:x for x in got["texts"]}
    refm={x["tag"]:x for x in row["metrics"]}

    # Required core tags.
    required=["chrome.topbar","chrome.brand","chrome.tabbar","screen.heading"]
    required += [f"tab.{r}" for r in ROUTES]
    required += [f"tab-label.{r}" for r in ROUTES]
    for tag in required:
        if tag not in boxes and tag not in texts:
            errors.append(f"{key}: required tag missing {tag}")

    # Exact line counts for headings/labels/buttons, +/-1 for body.
    for tag,metric in refm.items():
        kind=metric.get("kind")
        expected=metric.get("lineCount")
        if expected is None or kind=="box":
            continue
        native_tag=tag+".label" if kind=="button" else tag
        actual=texts.get(native_tag)
        if actual is None:
            # first-card has no text metric; only text-bearing reference tags reach here.
            errors.append(f"{key}: native text metric missing {native_tag}")
            continue
        is_tab_label=tag.startswith("tab-label.")
        native_overflow=actual["didOverflowWidth"] or actual["didOverflowHeight"]
        reference_overflow=bool(metric.get("overflowX")) or bool(metric.get("overflowY"))
        if native_overflow and not is_tab_label and not reference_overflow:
            errors.append(
                f"{key}: native overflow {native_tag} "
                f"width={actual['didOverflowWidth']} height={actual['didOverflowHeight']}"
            )
        elif native_overflow and reference_overflow:
            print(
                f"HARD_INFO intentional-reference-overflow {key} {native_tag} "
                f"nativeW={actual['didOverflowWidth']} nativeH={actual['didOverflowHeight']}"
            )
        delta=abs(int(actual["lineCount"])-int(expected))
        allowed=1 if kind=="body" else 0
        if is_tab_label:
            if delta:
                print(
                    f"HARD_INFO tab-line-count {key} {native_tag} "
                    f"ref={expected} native={actual['lineCount']}"
                )
        elif delta>allowed:
            errors.append(
                f"{key}: line-count {native_tag} ref={expected} "
                f"native={actual['lineCount']} allowed={allowed}"
            )
        checks+=1

    # Semantics-node bounds: every recorded interactive node must be >=48dp in
    # both axes. Visual children retain their 44/46dp prototype dimensions.
    interactive=[x for x in got["boxes"] if x.get("interactive")]
    if len(interactive)<10:
        errors.append(f"{key}: too few recorded interactive semantics nodes ({len(interactive)})")
    for metric in interactive:
        if metric["widthDp"] < 47.99 or metric["heightDp"] < 47.99:
            errors.append(
                f"{key}: touch-target {metric['tag']} "
                f"{metric['widthDp']:.2f}x{metric['heightDp']:.2f}dp"
            )
        checks+=1

if len(native)!=len(ref):
    errors.append(f"matrix size reference={len(ref)} native={len(native)}")

# Accessibility-first tab HARD gate. The 320 px Tamil HTML reference has
# intrinsic tab widths totalling 355 px inside 308 px of available nav width,
# so its line count/geometry is informational rather than normative. Native
# must instead keep all six labels fully visible and every touch target >=48 dp
# at 320/390 dp and 100%/200% font scale.
expected_tab_rows=8
if len(tab_native)!=expected_tab_rows:
    errors.append(f"tab-hard matrix size expected={expected_tab_rows} native={len(tab_native)}")
for tab_key,got in sorted(tab_native.items()):
    width_dp,language,font_scale=tab_key
    boxes={x["tag"]:x for x in got["boxes"]}
    texts={x["tag"]:x for x in got["texts"]}
    for route in ROUTES:
        label_tag=f"tab-label.{route}"
        touch_tag=f"touch.tab.{route}"
        visual_tag=f"tab.{route}"
        label=texts.get(label_tag)
        touch=boxes.get(touch_tag)
        visual=boxes.get(visual_tag)
        if label is None:
            errors.append(f"{tab_key}: tab-label missing {label_tag}")
        else:
            if label["didOverflowWidth"] or label["didOverflowHeight"]:
                errors.append(
                    f"{tab_key}: tab-label-fit {label_tag} lines={label['lineCount']} "
                    f"overflowW={label['didOverflowWidth']} overflowH={label['didOverflowHeight']}"
                )
            checks+=1
        if touch is None:
            errors.append(f"{tab_key}: touch-target missing {touch_tag}")
        else:
            if touch["widthDp"] < 47.99 or touch["heightDp"] < 47.99:
                errors.append(
                    f"{tab_key}: touch-target {touch_tag} "
                    f"{touch['widthDp']:.2f}x{touch['heightDp']:.2f}dp"
                )
            checks+=1
        if visual is None:
            errors.append(f"{tab_key}: tab visual missing {visual_tag}")
        else:
            right=visual["leftDp"]+visual["widthDp"]
            if visual["leftDp"] < -0.01 or right > width_dp+0.01:
                errors.append(
                    f"{tab_key}: tab-visible {visual_tag} "
                    f"left={visual['leftDp']:.2f} right={right:.2f} viewport={width_dp}"
                )
            checks+=1

if errors:
    fail(errors, checks, len(ref))
write_report([], checks, len(ref))
print(f"HARD_FIDELITY_PASS rows={len(ref)} checks={checks} failures=0")
