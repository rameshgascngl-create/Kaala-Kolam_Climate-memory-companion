#!/usr/bin/env python3
"""Enforced parity regression gate using tagged boxes and calibrated SSIM."""
from __future__ import annotations
import json
from pathlib import Path
from PIL import Image, ImageFilter
import numpy as np
from skimage.metrics import structural_similarity

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"tests/fidelity/parity-baseline.json"
REF_IMAGES=ROOT/"tests/golden-web"
NATIVE_IMAGES=ROOT/"app/build/visual-native"
REF_LAYOUT=ROOT/"tests/reference-metrics/fidelity-layout.json"
NATIVE_LAYOUT=ROOT/"app/build/fidelity/native-layout"


def prep(path: Path):
    with Image.open(path) as im:
        g=im.convert("L").filter(ImageFilter.GaussianBlur(2.0))
        size=(max(1,round(g.width*.25)),max(1,round(g.height*.25)))
        return np.asarray(g.resize(size,Image.Resampling.LANCZOS),dtype=np.float32)


def score(a: Path,b: Path)->float:
    return float(structural_similarity(prep(a),prep(b),data_range=255))


def layouts():
    out={}
    for p in NATIVE_LAYOUT.glob("*/*/*.json"):
        d=json.loads(p.read_text(encoding="utf-8"))
        out[(d["widthDp"],d["language"],d["state"])]=d
    return out


if not BASE.exists():
    raise SystemExit("PARITY_GATE_FAIL baseline missing: tests/fidelity/parity-baseline.json")
base=json.loads(BASE.read_text(encoding="utf-8"))
if not base.get("changeReason"):
    raise SystemExit("PARITY_GATE_FAIL baseline changeReason missing")
errors=[]

# SSIM regression, all 72 reference states.
base_images={x["path"]:x for x in base["images"]}
for rel,item in sorted(base_images.items()):
    ref=REF_IMAGES/rel
    native=NATIVE_IMAGES/rel
    if not ref.exists() or not native.exists():
        errors.append(f"image missing {rel}")
        continue
    current=score(ref,native)
    minimum=float(item["ssim"])-float(base["ssimRegressionSlack"])
    if current < minimum:
        errors.append(f"SSIM regression {rel} current={current:.6f} baseline={item['ssim']:.6f} min={minimum:.6f}")

# Geometry: must be <=4dp now and may not regress against a tighter baseline.
ref_rows={(r["widthDp"],r["language"],r["state"]):r for r in json.loads(REF_LAYOUT.read_text(encoding="utf-8"))["rows"]}
native=layouts()
baseline_geometry={(x["widthDp"],x["language"],x["state"],x["tag"]):x for x in base["geometry"]}
for key,item in sorted(baseline_geometry.items()):
    width,lang,state,tag=key
    rr=ref_rows.get((width,lang,state))
    nr=native.get((width,lang,state))
    if not rr or not nr:
        errors.append(f"layout row missing {width}/{lang}/{state}")
        continue
    rb={x["tag"]:x for x in rr["metrics"]}
    nb={x["tag"]:x for x in nr["boxes"]}
    if tag not in rb or tag not in nb:
        errors.append(f"tag missing {width}/{lang}/{state}/{tag}")
        continue
    ref=rb[tag]; got=nb[tag]
    current=max(
      abs(float(ref["left"])-float(got["leftDp"])),
      abs(float(ref["top"])-float(got["topDp"])),
      abs(float(ref["width"])-float(got["widthDp"])),
      abs(float(ref["height"])-float(got["heightDp"])),
    )
    if current > float(base["boxToleranceDp"])+1e-6:
        errors.append(f"box >4dp {width}/{lang}/{state}/{tag} error={current:.3f}dp")
    baseline=float(item["maxErrorDp"])
    if baseline <= float(base["boxToleranceDp"]) and current > baseline+float(base["boxRegressionSlackDp"]):
        errors.append(
          f"box regression {width}/{lang}/{state}/{tag} current={current:.3f} "
          f"baseline={baseline:.3f}"
        )

if errors:
    print(f"PARITY_GATE_FAIL count={len(errors)} baseline={base.get('baselineCommit')}")
    for e in errors: print("PARITY_FAIL",e)
    raise SystemExit(1)
print(
  f"PARITY_GATE_PASS images={len(base_images)} geometry={len(baseline_geometry)} "
  f"box<=4dp ssim_regression_slack={base['ssimRegressionSlack']}"
)
