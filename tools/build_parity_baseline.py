#!/usr/bin/env python3
"""Build a deliberate compact PARITY baseline from current measured evidence.

CI never calls this script. A baseline creation/update is a reviewed repository
change and requires a non-empty reason. Geometry already outside the absolute
4 dp contract is counted but deliberately excluded from the regression
baseline, so baseline creation cannot turn an existing geometry defect into a
PASS.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageFilter
import numpy as np
from skimage.metrics import structural_similarity

ROOT = Path(__file__).resolve().parents[1]
REF_IMAGES = ROOT / "tests/golden-web"
NATIVE_IMAGES = ROOT / "app/build/visual-native"
REF_LAYOUT = ROOT / "tests/reference-metrics/fidelity-layout.json"
NATIVE_LAYOUT = ROOT / "app/build/fidelity/native-layout"
DEFAULT_OUT = ROOT / "tests/fidelity/parity-baseline.json"

GEOMETRY_TAGS = (
    "chrome.topbar", "chrome.brand", "tool.text-size", "tool.language", "tool.theme",
    "chrome.tabbar", "tab.home", "tab.learn", "tab.elders", "tab.class",
    "tab.council", "tab.predict", "screen.heading", "screen.body.primary",
    "screen.first-card", "screen.primary-button",
)


def prep(path: Path) -> np.ndarray:
    with Image.open(path) as im:
        gray = im.convert("L").filter(ImageFilter.GaussianBlur(2.0))
        size = (max(1, round(gray.width * .25)), max(1, round(gray.height * .25)))
        return np.asarray(gray.resize(size, Image.Resampling.LANCZOS), dtype=np.float32)


def ssim(a: Path, b: Path) -> float:
    return float(structural_similarity(prep(a), prep(b), data_range=255))


def native_layout() -> dict:
    out = {}
    for path in NATIVE_LAYOUT.glob("*/*/*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        out[(data["widthDp"], data["language"], data["state"])] = data
    return out


def box_error(ref: dict, got: dict) -> float:
    return max(
        abs(float(ref["left"]) - float(got["leftDp"])),
        abs(float(ref["top"]) - float(got["topDp"])),
        abs(float(ref["width"]) - float(got["widthDp"])),
        abs(float(ref["height"]) - float(got["heightDp"])),
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--reason", required=True)
    args = parser.parse_args()
    if not args.reason.strip():
        raise SystemExit("PARITY_BASELINE_BUILD_FAIL empty reason")

    refs = sorted(REF_IMAGES.glob("*/*/*/*.png"))
    if len(refs) != 72:
        raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL expected 72 refs got {len(refs)}")

    images: dict[str, float] = {}
    for ref in refs:
        rel = ref.relative_to(REF_IMAGES)
        native = NATIVE_IMAGES / rel
        if not native.exists():
            raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL missing native {rel}")
        images[rel.as_posix()] = round(ssim(ref, native), 6)

    layouts = native_layout()
    geometry_good: dict[str, float] = {}
    excluded_over_tolerance = 0
    geometry_total = 0
    for row in json.loads(REF_LAYOUT.read_text(encoding="utf-8"))["rows"]:
        key = (row["widthDp"], row["language"], row["state"])
        got = layouts.get(key)
        if not got:
            raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL missing native layout {key}")
        ref_boxes = {x["tag"]: x for x in row["metrics"]}
        got_boxes = {x["tag"]: x for x in got["boxes"]}
        for tag in GEOMETRY_TAGS:
            if tag not in ref_boxes or tag not in got_boxes:
                continue
            geometry_total += 1
            maximum = box_error(ref_boxes[tag], got_boxes[tag])
            key_string = f"{key[0]}|{key[1]}|{key[2]}|{tag}"
            if maximum <= 4.0:
                geometry_good[key_string] = round(maximum, 4)
            else:
                excluded_over_tolerance += 1

    data = {
        "schemaVersion": 2,
        "baselineCommit": args.commit,
        "changeReason": args.reason.strip(),
        "preprocess": {
            "grayscale": True,
            "gaussianBlurPx": 2.0,
            "scale": 0.25,
            "resample": "Lanczos",
        },
        "boxToleranceDp": 4.0,
        "boxRegressionSlackDp": 0.25,
        "ssimRegressionSlack": 0.002,
        "images": images,
        "geometryWithinTolerance": geometry_good,
        "geometryTotalMeasured": geometry_total,
        "geometryExcludedOverTolerance": excluded_over_tolerance,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print(
        f"PARITY_BASELINE_BUILD_PASS images={len(images)} "
        f"geometry_good={len(geometry_good)} geometry_over4={excluded_over_tolerance} "
        f"out={args.out}"
    )


if __name__ == "__main__":
    main()
