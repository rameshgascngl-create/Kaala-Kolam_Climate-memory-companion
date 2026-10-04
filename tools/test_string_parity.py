#!/usr/bin/env python3
from __future__ import annotations

import collections
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EN = ROOT / "app/src/main/res/values/strings.xml"
TA = ROOT / "app/src/main/res/values-ta/strings.xml"
ALLOW = ROOT / "tools/latin_allowlist.txt"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z]")
VERSION = re.compile(r"^(?:Version|பதிப்பு)?\s*v?\d+(?:\.\d+){1,3}(?:[-+][\w.-]+)?$", re.I)

allowed = {
    line.strip() for line in ALLOW.read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}

def fail(message: str) -> None:
    raise SystemExit("STRING_PARITY_FAIL: " + message)

def text_of(node: ET.Element) -> str:
    return "".join(node.itertext()).strip()

def parse(path: Path):
    root = ET.parse(path).getroot()
    strings = {}
    plurals = {}
    for node in root:
        name = node.get("name")
        if not name:
            continue
        if node.get("translatable") == "false":
            fail(f"{path}: translatable=false is forbidden for visible resources: {name}")
        if node.tag == "string":
            value = text_of(node)
            if not value:
                fail(f"{path}: empty string {name}")
            strings[name] = value
        elif node.tag == "plurals":
            items = {item.get("quantity"): text_of(item) for item in node.findall("item")}
            if not items or any(not q or not v for q, v in items.items()):
                fail(f"{path}: invalid plurals {name}")
            plurals[name] = items
    return strings, plurals

en, en_pl = parse(EN)
ta, ta_pl = parse(TA)
if set(en) != set(ta):
    fail(f"string key mismatch only-en={sorted(set(en)-set(ta))} only-ta={sorted(set(ta)-set(en))}")
if set(en_pl) != set(ta_pl):
    fail(f"plurals key mismatch only-en={sorted(set(en_pl)-set(ta_pl))} only-ta={sorted(set(ta_pl)-set(en_pl))}")

for key in sorted(en):
    e, t = en[key], ta[key]
    if e == t and e not in allowed and not VERSION.fullmatch(e):
        fail(f"English-identical Tamil value not allow-listed: {key}={e!r}")
    if collections.Counter(PLACEHOLDER.findall(e)) != collections.Counter(PLACEHOLDER.findall(t)):
        fail(f"format placeholder mismatch for {key}: en={e!r} ta={t!r}")

for key in sorted(en_pl):
    if set(en_pl[key]) != set(ta_pl[key]):
        fail(f"plural quantities mismatch for {key}")
    for quantity in en_pl[key]:
        if collections.Counter(PLACEHOLDER.findall(en_pl[key][quantity])) != collections.Counter(PLACEHOLDER.findall(ta_pl[key][quantity])):
            fail(f"plural placeholder mismatch for {key}/{quantity}")

print(f"STRING_PARITY_PASS strings={len(en)} plurals={len(en_pl)}")
