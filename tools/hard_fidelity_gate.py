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
ROUTES=("home","learn","elders","class","council","predict")


def fail(msgs: list[str]) -> None:
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


ref=json.loads(REF.read_text(encoding="utf-8"))["rows"]
native=load_native()
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

    # Reference overflow itself is a hard failure.
    for tag,metric in refm.items():
        if metric.get("overflowX") or metric.get("overflowY"):
            errors.append(f"{key}: reference overflow {tag}")

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
        if actual["didOverflowWidth"] or actual["didOverflowHeight"]:
            errors.append(
                f"{key}: native overflow {native_tag} "
                f"width={actual['didOverflowWidth']} height={actual['didOverflowHeight']}"
            )
        delta=abs(int(actual["lineCount"])-int(expected))
        allowed=1 if kind=="body" else 0
        if delta>allowed:
            errors.append(
                f"{key}: line-count {native_tag} ref={expected} "
                f"native={actual['lineCount']} allowed={allowed}"
            )
        checks+=1

    # Tab labels must fit without visual overflow and never exceed the prototype
    # two-line contract.
    for route in ROUTES:
        tag=f"tab-label.{route}"
        metric=texts.get(tag)
        if metric is None:
            errors.append(f"{key}: tab-label missing {tag}")
            continue
        if metric["didOverflowWidth"] or metric["didOverflowHeight"] or metric["lineCount"]>2:
            errors.append(
                f"{key}: tab-label-fit {tag} lines={metric['lineCount']} "
                f"overflowW={metric['didOverflowWidth']} overflowH={metric['didOverflowHeight']}"
            )

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

if errors:
    fail(errors)
print(f"HARD_FIDELITY_PASS rows={len(ref)} checks={checks} failures=0")
