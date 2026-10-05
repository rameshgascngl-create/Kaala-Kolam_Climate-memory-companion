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
STATUS = ROOT / "tests/fidelity/build-status.json"
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


def screen_from_path(value: str) -> str | None:
    part = value.split("/")[-1]
    if part.endswith(".png"):
        return part[:-4]
    pieces = value.split("/")
    if len(pieces) >= 3 and pieces[0].isdigit():
        return pieces[2]
    return None


def write_report(
    errors: list[str],
    informational: list[str],
    baseline_commit: str | None,
    image_count: int,
    geometry_count: int,
    screen_status: dict[str, str],
) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    by_screen: dict[str, list[str]] = {}
    info_by_screen: dict[str, list[str]] = {}
    for target, destination in ((errors, by_screen), (informational, info_by_screen)):
        for error in target:
            key = "unknown"
            for part in error.split():
                if part.count("/") >= 2:
                    key = part
                    break
            destination.setdefault(key, []).append(error)
    REPORT.write_text(
        json.dumps(
            {
                "gate": "PARITY",
                "passed": not errors,
                "failureCount": len(errors),
                "informationalFailureCount": len(informational),
                "baselineCommit": baseline_commit,
                "imageCount": image_count,
                "geometryCount": geometry_count,
                "screenStatus": screen_status,
                "failures": errors,
                "informational": informational,
                "byScreen": by_screen,
                "informationalByScreen": info_by_screen,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def fail_early(message: str) -> None:
    write_report([message], [], None, 0, 0, {})
    print(message)
    raise SystemExit(1)


if not BASE.exists():
    fail_early("PARITY_GATE_FAIL baseline missing: tests/fidelity/parity-baseline.json")
if not STATUS.exists():
    fail_early("PARITY_GATE_FAIL screen status missing: tests/fidelity/build-status.json")

status_doc = json.loads(STATUS.read_text(encoding="utf-8"))
screen_status = {str(k): str(v) for k, v in status_doc.get("screens", {}).items()}
valid_status = {"DONE", "PARTIAL", "PLACEHOLDER"}
if not screen_status or any(value not in valid_status for value in screen_status.values()):
    fail_early("PARITY_GATE_FAIL invalid tests/fidelity/build-status.json screen status")

base = json.loads(BASE.read_text(encoding="utf-8"))
if not base.get("changeReason"):
    fail_early("PARITY_GATE_FAIL baseline changeReason missing")

schema = int(base.get("schemaVersion", 1))
box_tolerance = float(base["boxToleranceDp"])
box_slack = float(base["boxRegressionSlackDp"])
ssim_slack = float(base["ssimRegressionSlack"])
baseline_commit = base.get("baselineCommit")
errors: list[str] = []
informational: list[str] = []

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

# SSIM is measured for every state. It is enforced only when the screen is DONE.
for rel, baseline_ssim in sorted(base_images.items()):
    state = Path(rel).stem
    status = screen_status.get(state)
    if status is None:
        errors.append(f"screen status missing {state}")
        continue
    ref = REF_IMAGES / rel
    native = NATIVE_IMAGES / rel
    if not ref.exists() or not native.exists():
        message = f"image missing {rel}"
        (errors if status == "DONE" else informational).append(message)
        continue
    current = score(ref, native)
    minimum = baseline_ssim - ssim_slack
    if current < minimum:
        message = (
            f"SSIM regression {rel} current={current:.6f} "
            f"baseline={baseline_ssim:.6f} min={minimum:.6f}"
        )
        (errors if status == "DONE" else informational).append(message)

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
    status = screen_status.get(state)
    if status is None:
        errors.append(f"screen status missing {state}")
        continue
    got_row = native.get(row_key)
    if got_row is None:
        message = f"layout row missing {width}/{lang}/{state}"
        (errors if status == "DONE" else informational).append(message)
        continue
    ref_boxes = {x["tag"]: x for x in ref_row["metrics"]}
    got_boxes = {x["tag"]: x for x in got_row["boxes"]}
    for tag in GEOMETRY_TAGS:
        if tag not in ref_boxes:
            continue
        geometry_count += 1
        if tag not in got_boxes:
            message = f"tag missing {width}/{lang}/{state}/{tag}"
            (errors if status == "DONE" else informational).append(message)
            continue
        current = box_error(ref_boxes[tag], got_boxes[tag])
        path_key = f"{width}|{lang}|{state}|{tag}"
        screen_key = f"{width}/{lang}/{state}/{tag}"
        if current > box_tolerance + 1e-6:
            message = f"box >4dp {screen_key} error={current:.3f}dp"
            (errors if status == "DONE" else informational).append(message)
            continue
        baseline_error = baseline_geometry.get(path_key)
        if baseline_error is not None and current > baseline_error + box_slack:
            message = (
                f"box regression {screen_key} current={current:.3f} "
                f"baseline={baseline_error:.3f} slack={box_slack:.3f}"
            )
            (errors if status == "DONE" else informational).append(message)

if len(native) != len(ref_rows):
    errors.append(f"layout matrix size reference={len(ref_rows)} native={len(native)}")

write_report(errors, informational, baseline_commit, len(base_images), geometry_count, screen_status)
for message in informational:
    print("PARITY_INFO", message)
if errors:
    print(f"PARITY_GATE_FAIL count={len(errors)} baseline={baseline_commit}")
    for error in errors:
        print("PARITY_FAIL", error)
    raise SystemExit(1)

done_count = sum(1 for value in screen_status.values() if value == "DONE")
print(
    f"PARITY_GATE_PASS enforced_done_screens={done_count} "
    f"informational_findings={len(informational)} images={len(base_images)} "
    f"geometry={geometry_count} box<=4dp ssim_regression_slack={ssim_slack}"
)
