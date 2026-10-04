#!/usr/bin/env python3
from __future__ import annotations
import argparse
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

ROOT=Path(__file__).resolve().parents[1]
def metric(ref_path:Path,native_path:Path):
    if not native_path.exists(): return False,None,None,"native screenshot missing"
    with Image.open(ref_path) as a, Image.open(native_path) as b:
        ref=a.convert("RGB"); got=b.convert("RGB")
        if ref.size!=got.size: return False,None,None,f"size {got.size}, expected {ref.size}"
        diff=ImageChops.difference(ref,got)
        mae=sum(ImageStat.Stat(diff).mean[:3])/(3*255)
        total=ref.width*ref.height
        changed=sum(1 for px in diff.getdata() if max(px)>16)/total
        return mae<=.035 and changed<=.20,mae,changed,""

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--web",type=Path,default=ROOT/"tests/golden-tamil")
    ap.add_argument("--native",type=Path,default=ROOT/"app/build/visual-tamil")
    ap.add_argument("--report",type=Path,default=ROOT/"docs/tamil-visual-parity-report.md")
    args=ap.parse_args()
    refs=sorted(args.web.glob("*/*/*/*.png"))
    if len(refs)!=80: raise SystemExit(f"Expected 80 Tamil references, found {len(refs)}")
    rows=[]; passed=0
    for ref in refs:
        rel=ref.relative_to(args.web); ok,mae,changed,note=metric(ref,args.native/rel)
        passed+=int(ok); rows.append((rel,ok,mae,changed,note))
    out=["# Tamil screenshot parity report","", "This localisation stress report does not claim that the separate visual redesign is complete.","",
         "| Viewport | Font scale | Theme | Screen | Result | MAE | Changed pixels | Note |",
         "|---|---:|---|---|---|---:|---:|---|"]
    for rel,ok,mae,changed,note in rows:
        vp,scale,theme,file=rel.parts
        fmt=lambda v:"—" if v is None else f"{v*100:.3f}%"
        out.append(f"| {vp} | {scale}% | {theme} | {Path(file).stem} | **{'PASS' if ok else 'FAIL'}** | {fmt(mae)} | {fmt(changed)} | {note} |")
    out += ["",f"Overall Tamil matrix: **{passed}/80 PASS; {80-passed}/80 FAIL**.","",
            "FAIL here means native pixels are outside the renderer-independent thresholds; it does not invalidate the Tamil text/content tests."]
    args.report.write_text("\n".join(out)+"\n",encoding="utf-8")
    print(f"TAMIL_VISUAL_MATRIX pass={passed} fail={80-passed}")
if __name__=="__main__": main()
