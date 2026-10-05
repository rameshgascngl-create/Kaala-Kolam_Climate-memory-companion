#!/usr/bin/env python3
"""Build an explicit parity baseline from current reference/native evidence.

This tool is for deliberate baseline creation only. CI parity checks never call
it. Every committed baseline must carry a human-readable changeReason.
"""
from __future__ import annotations
import argparse, json
from pathlib import Path
from PIL import Image, ImageFilter
import numpy as np
from skimage.metrics import structural_similarity

ROOT=Path(__file__).resolve().parents[1]
REF_IMAGES=ROOT/"tests/golden-web"
NATIVE_IMAGES=ROOT/"app/build/visual-native"
REF_LAYOUT=ROOT/"tests/reference-metrics/fidelity-layout.json"
NATIVE_LAYOUT=ROOT/"app/build/fidelity/native-layout"
DEFAULT_OUT=ROOT/"tests/fidelity/parity-baseline.json"
GEOMETRY_TAGS=(
 "chrome.topbar","chrome.brand","tool.text-size","tool.language","tool.theme",
 "chrome.tabbar","tab.home","tab.learn","tab.elders","tab.class","tab.council","tab.predict",
 "screen.heading","screen.body.primary","screen.first-card","screen.primary-button",
)


def prep(path: Path):
    with Image.open(path) as im:
        g=im.convert("L").filter(ImageFilter.GaussianBlur(2.0))
        size=(max(1,round(g.width*.25)),max(1,round(g.height*.25)))
        return np.asarray(g.resize(size,Image.Resampling.LANCZOS),dtype=np.float32)


def ssim(a: Path,b: Path)->float:
    return float(structural_similarity(prep(a),prep(b),data_range=255))


def native_layout():
    out={}
    for p in NATIVE_LAYOUT.glob("*/*/*.json"):
        d=json.loads(p.read_text(encoding="utf-8"))
        out[(d["widthDp"],d["language"],d["state"])]=d
    return out


def box_error(ref,got):
    vals=[
      abs(float(ref["left"])-float(got["leftDp"])),
      abs(float(ref["top"])-float(got["topDp"])),
      abs(float(ref["width"])-float(got["widthDp"])),
      abs(float(ref["height"])-float(got["heightDp"])),
    ]
    return max(vals),vals


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",type=Path,default=DEFAULT_OUT)
    ap.add_argument("--commit",required=True)
    ap.add_argument("--reason",required=True)
    args=ap.parse_args()
    if not args.reason.strip():
        raise SystemExit("PARITY_BASELINE_BUILD_FAIL empty reason")

    images=[]
    refs=sorted(REF_IMAGES.glob("*/*/*/*.png"))
    if len(refs)!=72:
        raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL expected 72 refs got {len(refs)}")
    for ref in refs:
        rel=ref.relative_to(REF_IMAGES)
        native=NATIVE_IMAGES/rel
        if not native.exists():
            raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL missing native {rel}")
        images.append({"path":rel.as_posix(),"ssim":round(ssim(ref,native),6)})

    nlayout=native_layout()
    geometry=[]
    for row in json.loads(REF_LAYOUT.read_text(encoding="utf-8"))["rows"]:
        key=(row["widthDp"],row["language"],row["state"])
        got=nlayout.get(key)
        if not got:
            raise SystemExit(f"PARITY_BASELINE_BUILD_FAIL missing native layout {key}")
        rb={x["tag"]:x for x in row["metrics"]}
        gb={x["tag"]:x for x in got["boxes"]}
        for tag in GEOMETRY_TAGS:
            if tag not in rb or tag not in gb:
                continue
            maximum,values=box_error(rb[tag],gb[tag])
            geometry.append({
              "widthDp":key[0],"language":key[1],"state":key[2],"tag":tag,
              "maxErrorDp":round(maximum,4),
              "componentErrorsDp":[round(x,4) for x in values],
            })

    data={
      "schemaVersion":1,
      "baselineCommit":args.commit,
      "changeReason":args.reason.strip(),
      "preprocess":{"grayscale":True,"gaussianBlurPx":2.0,"scale":0.25,"resample":"Lanczos"},
      "boxToleranceDp":4.0,
      "boxRegressionSlackDp":0.25,
      "ssimRegressionSlack":0.002,
      "images":images,
      "geometry":geometry,
    }
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8")
    print(f"PARITY_BASELINE_BUILD_PASS images={len(images)} geometry={len(geometry)} out={args.out}")


if __name__=="__main__": main()
