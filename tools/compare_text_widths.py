#!/usr/bin/env python3
"""Compare Chromium and Android Paint text widths for the same pinned fonts."""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WEB = ROOT / "tests/reference-metrics/text-widths.json"
ANDROID = ROOT / "app/build/fidelity/android-text-widths.json"
TOLERANCE = 0.01


def main() -> None:
    web = {x["id"]: x for x in json.loads(WEB.read_text(encoding="utf-8"))["samples"]}
    android = {x["id"]: x for x in json.loads(ANDROID.read_text(encoding="utf-8"))["samples"]}
    if set(web) != set(android):
        raise SystemExit(f"TEXT_WIDTH_PROBE_FAIL key mismatch web={sorted(web)} android={sorted(android)}")
    failures = []
    for key in sorted(web):
        w = float(web[key]["widthPx"])
        a = float(android[key]["widthPx"])
        delta = abs(a - w) / w if w else 1.0
        print(
            f"TEXT_WIDTH_PROBE sample={key} chromium={w:.4f} android={a:.4f} "
            f"delta={delta*100:.3f}% chromium_family={web[key].get('platformFamily')} "
            f"android_family={android[key].get('resolvedFamily')}"
        )
        if delta > TOLERANCE:
            failures.append((key, delta))
    if failures:
        raise SystemExit(
            "TEXT_WIDTH_PROBE_FAIL tolerance=1% "
            + " ".join(f"{key}={delta*100:.3f}%" for key, delta in failures)
        )
    print(f"TEXT_WIDTH_PROBE_PASS samples={len(web)} tolerance=1%")


if __name__ == "__main__":
    main()
