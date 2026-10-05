#!/usr/bin/env python3
"""Hard gate: Compose colour tokens must exactly equal the audited HTML CSS."""
from __future__ import annotations
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
HTML=(ROOT/"Kaala_Kolam_Climate_Memory_Companion.html").read_text(encoding="utf-8")
THEME=(ROOT/"app/src/main/java/edu/gascnagercoil/kaalakolam/ui/theme/Theme.kt").read_text(encoding="utf-8")
KEYS=("ground","ground2","ground3","line","flour","flour2","faint","turmeric","vermilion","sea","ink")


def css_vars(selector: str) -> dict[str,str]:
    if selector=="dark":
        m=re.search(r":root\s*\{(.*?)\}",HTML,re.S)
    else:
        m=re.search(r':root\[data-theme="light"\]\s*\{(.*?)\}',HTML,re.S)
    if not m: raise SystemExit(f"HARD_COLOUR_FAIL missing CSS {selector}")
    return {k:v.lower() for k,v in re.findall(r"--([\w-]+)\s*:\s*(#[0-9a-fA-F]{6})",m.group(1))}


def theme_block(name: str) -> dict[str,str]:
    m=re.search(rf"val {name} = PrototypePalette\((.*?)\n\s*\)",THEME,re.S)
    if not m: raise SystemExit(f"HARD_COLOUR_FAIL missing Theme {name}")
    return {k:("#"+v[-6:].lower()) for k,v in re.findall(r"(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)",m.group(1))}


dark=css_vars("dark")
light=dict(dark); light.update(css_vars("light"))
for label,css,name in (("dark",dark,"Dark"),("light",light,"Light")):
    got=theme_block(name)
    for key in KEYS:
        if got.get(key)!=css.get(key):
            raise SystemExit(f"HARD_COLOUR_FAIL {label}.{key} css={css.get(key)} compose={got.get(key)}")

scale=[dark[k] for k in ("s-2","s-1","s0","s1","s2")]
m=re.search(r"val ScaleColours = listOf\\(([^\\n]+)\\)",THEME)
if not m: raise SystemExit("HARD_COLOUR_FAIL missing ScaleColours")
got_scale=["#"+v[-6:].lower() for v in re.findall(r"Color\\(0x([0-9A-Fa-f]{8})\\)",m.group(1))]
if got_scale!=scale:
    raise SystemExit(f"HARD_COLOUR_FAIL scale css={scale} compose={got_scale}")
print(f"HARD_COLOUR_PASS dark={len(KEYS)} light={len(KEYS)} scale={len(scale)} exact")
