#!/usr/bin/env python3
"""Enforced PARITY regression gate using tagged boxes and calibrated SSIM.

The absolute 4 dp geometry limit is never relaxed by the baseline. The baseline
only prevents already-good geometry/SSIM from regressing. Broken geometry is
not legitimised by baseline creation.
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageFilter
import numpy as np
from skimage.metrics import structural_similarity

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "tests/fidelity/parity-baseline.json"
REF_IMAGES = ROOT / "tests/golden-web"
NATIVE_IMAGES = ROOT / "app/build/visual-native"
REF_LAYOUT = ROOT / "tests/reference-metrics/fidelity-layout.json"
NATIVE_LAYOUT = ROOT / "app/build/fidelity/native-layout"
REPORT = ROOT / "app/build/fidelity/parity-report.json"

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


def score(a: Path, b: Path) -> float:
    return float(structural_similarity(prep(a), prep(b), data_range=255))


def layouts() -> dict:
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


def write_report(errors: list[str], baseline_commit: str | None, image_count: int, geometry_count: int) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    by_screen: dict[str, list[str]] = {}
    for error in errors:
        key = "unknown"
        for part in error.split():
            if part.count("/") >= 2:
                key = part
                break
        by_screen.setdefault(key, []).append(error)
    REPORT.write_text(
        json.dumps(
            {
                "gate": "PARITY",
                "passed": not errors,
                "failureCount": len(errors),
                "baselineCommit": baseline_commit,
                "imageCount": image_count,
                "geometryCount": geometry_count,
                "failures": errors,
                "byScreen": by_screen,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def fail_early(message: str) -> None:
    write_report([message], None, 0, 0)
    print(message)
    raise SystemExit(1)


if not BASE.exists():
    fail_early("PARITY_GATE_FAIL baseline missing: tests/fidelity/parity-baseline.json")

base = json.loads(BASE.read_text(encoding="utf-8"))
if not base.get("changeReason"):
    fail_early("PARITY_GATE_FAIL baseline changeReason missing")

schema = int(base.get("schemaVersion", 1))
box_tolerance = float(base["boxToleranceDp"])
box_slack = float(base["boxRegressionSlackDp"])
ssim_slack = float(base["ssimRegressionSlack"])
baseline_commit = base.get("baselineCommit")
errors: list[str] = []

# Schema 2 uses compact dictionaries. Schema 1 remains readable for older
# deliberate baselines.
if schema >= 2:
    base_images = {str(k): float(v) for k, v in base.get("images", {}).items()}
    baseline_geometry = {
        str(k): float(v) for k, v in base.get("geometryWithinTolerance", {}).items()
    }
else:
    base_images = {x["path"]: float(x["ssim"]) for x in base.get("images", [])}
    baseline_geometry = {
        f'{x["widthDp"]}|{x["language"]}|{x["state"]}|{x["tag"]}': float(x["maxErrorDp"])
        for x in base.get("geometry", [])
        if float(x["maxErrorDp"]) <= box_tolerance
    }

# SSIM regression: all 72 reference states must be represented.
for rel, baseline_ssim in sorted(base_images.items()):
    ref = REF_IMAGES / rel
    native = NATIVE_IMAGES / rel
    if not ref.exists() or not native.exists():
        errors.append(f"image missing {rel}")
        continue
    current = score(ref, native)
    minimum = baseline_ssim - ssim_slack
    if current < minimum:
        errors.append(
            f"SSIM regression {rel} current={current:.6f} "
            f"baseline={baseline_ssim:.6f} min={minimum:.6f}"
        )

if len(base_images) != 72:
    errors.append(f"baseline image count={len(base_images)} expected=72")

# Absolute geometry is checked for every current tagged anchor, regardless of
# whether that anchor was good enough to enter the initial regression baseline.
ref_rows = {
    (row["widthDp"], row["language"], row["state"]): row
    for row in json.loads(REF_LAYOUT.read_text(encoding="utf-8"))["rows"]
}
native = layouts()
geometry_count = 0
for row_key, ref_row in sorted(ref_rows.items()):
    width, lang, state = row_key
    got_row = native.get(row_key)
    if got_row is None:
        errors.append(f"layout row missing {width}/{lang}/{state}")
        continue
    ref_boxes = {x["tag"]: x for x in ref_row["metrics"]}
    got_boxes = {x["tag"]: x for x in got_row["boxes"]}
    for tag in GEOMETRY_TAGS:
        if tag not in ref_boxes:
            continue
        geometry_count += 1
        if tag not in got_boxes:
            errors.append(f"tag missing {width}/{lang}/{state}/{tag}")
            continue
        current = box_error(ref_boxes[tag], got_boxes[tag])
        path_key = f"{width}|{lang}|{state}|{tag}"
        screen_key = f"{width}/{lang}/{state}/{tag}"
        if current > box_tolerance + 1e-6:
            errors.append(f"box >4dp {screen_key} error={current:.3f}dp")
            continue
        baseline_error = baseline_geometry.get(path_key)
        if baseline_error is not None and current > baseline_error + box_slack:
            errors.append(
                f"box regression {screen_key} current={current:.3f} "
                f"baseline={baseline_error:.3f} slack={box_slack:.3f}"
            )

if len(native) != len(ref_rows):
    errors.append(f"layout matrix size reference={len(ref_rows)} native={len(native)}")

write_report(errors, baseline_commit, len(base_images), geometry_count)
if errors:
    print(f"PARITY_GATE_FAIL count={len(errors)} baseline={baseline_commit}")
    for error in errors:
        print("PARITY_FAIL", error)
    raise SystemExit(1)

print(
    f"PARITY_GATE_PASS images={len(base_images)} geometry={geometry_count} "
    f"box<=4dp ssim_regression_slack={ssim_slack}"
)
