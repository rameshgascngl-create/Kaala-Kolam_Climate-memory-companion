#!/usr/bin/env python3
from __future__ import annotations

import collections
import json
import re
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/content"
EN_XML = ROOT / "app/src/main/res/values/strings.xml"
TA_XML = ROOT / "app/src/main/res/values-ta/strings.xml"
OVERRIDES = json.loads((ROOT / "tools/content_overrides.json").read_text(encoding="utf-8"))
ALLOW_FILE = ROOT / "tools/latin_allowlist.txt"
ALLOW = {
    line.strip() for line in ALLOW_FILE.read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.lstrip().startswith("#")
}
SOURCE_TOKENS = {"IPCC", "AR6", "SR1.5", "IMD", "MoES", "NOAA", "WMO", "UNEP"}
LATIN = re.compile(r"[A-Za-z][A-Za-z0-9]*(?:\.[A-Za-z0-9]+)*")
NUMBER = re.compile(r"(?<![\w])(?:[+−±-]?\d+(?:,\d{3})*(?:\.\d+)?%?)")
TAMIL = re.compile(r"[\u0B80-\u0BFF]")
MOJIBAKE = ("Ã", "Â", "â€", "ðŸ")

def fail(message: str) -> None:
    raise SystemExit("TAMIL_CONTENT_FAIL: " + message)

def walk(value, path=""):
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for i, child in enumerate(value):
            yield from walk(child, f"{path}[{i}]")
    elif isinstance(value, dict):
        for key, child in value.items():
            yield from walk(child, f"{path}.{key}" if path else key)

def bilingual(value, path=""):
    if isinstance(value, list):
        for i, child in enumerate(value):
            yield from bilingual(child, f"{path}[{i}]")
    elif isinstance(value, dict):
        if isinstance(value.get("en"), str) or isinstance(value.get("ta"), str):
            yield path, value.get("en"), value.get("ta")
        for key, child in value.items():
            yield from bilingual(child, f"{path}.{key}" if path else key)

def numbers(text: str) -> collections.Counter[str]:
    return collections.Counter(NUMBER.findall(text))

def allowed_latin_word(word: str) -> bool:
    if word in ALLOW:
        return True
    # Hyphenated Tamil suffixes are outside this regex; punctuation is not part of the token.
    return False

payloads = {}
for path in sorted(CONTENT.glob("*.json")):
    raw = path.read_text(encoding="utf-8")
    if unicodedata.normalize("NFC", raw) != raw:
        fail(f"{path}: file is not NFC-normalised")
    if "\ufffd" in raw or any(marker in raw for marker in MOJIBAKE):
        fail(f"{path}: Unicode replacement/mojibake marker found")
    payloads[path.name] = json.loads(raw)

for name, payload in payloads.items():
    if name in {"manifest.json", "gaps.json"}:
        continue
    for path, en, ta in bilingual(payload, name):
        if not isinstance(en, str) or not en.strip():
            fail(f"{path}: empty English in bilingual object")
        if not isinstance(ta, str) or not ta.strip():
            fail(f"{path}: empty Tamil in bilingual object")
        if not TAMIL.search(ta) and ta not in ALLOW:
            fail(f"{path}: Tamil value has no Tamil letters: {ta!r}")
        en_nums, ta_nums = numbers(en), numbers(ta)
        if en_nums.get("500,000") and ta_nums.get("5") and "இலட்சம்" in ta and ("km³" in en or "km3" in en):
            en_nums["500,000"] -= 1
            ta_nums["5"] -= 1
            if not en_nums["500,000"]:
                del en_nums["500,000"]
            if not ta_nums["5"]:
                del ta_nums["5"]
        if en_nums != ta_nums:
            fail(f"{path}: number mismatch en={dict(en_nums)} ta={dict(ta_nums)}")
        for token in SOURCE_TOKENS:
            if token in en and token not in ta:
                fail(f"{path}: source token {token!r} was not preserved")
        for word in LATIN.findall(ta):
            if len(word) >= 4 and not allowed_latin_word(word):
                fail(f"{path}: Latin word outside allow-list: {word!r}")

# Tamil resource values are subject to the same Unicode and Latin policy.
en_root = ET.parse(EN_XML).getroot()
ta_root = ET.parse(TA_XML).getroot()
en_strings = {n.get("name"): "".join(n.itertext()).strip() for n in en_root.findall("string")}
for node in ta_root.findall("string"):
    key = node.get("name")
    value = "".join(node.itertext()).strip()
    if not value:
        fail(f"values-ta/{key}: empty")
    if unicodedata.normalize("NFC", value) != value:
        fail(f"values-ta/{key}: not NFC-normalised")
    if "\ufffd" in value or any(marker in value for marker in MOJIBAKE):
        fail(f"values-ta/{key}: Unicode replacement/mojibake marker found")
    if value not in ALLOW and not re.fullmatch(r"(?:பதிப்பு\s*)?\d+(?:\.\d+){1,3}", value):
        for word in LATIN.findall(value):
            if len(word) >= 4 and not allowed_latin_word(word):
                fail(f"values-ta/{key}: Latin word outside allow-list: {word!r}")
    en = en_strings.get(key)
    if en:
        en_nums, ta_nums = numbers(en), numbers(value)
        if en_nums != ta_nums:
            fail(f"values-ta/{key}: number mismatch en={dict(en_nums)} ta={dict(ta_nums)}")

# Banned-stem policy, with only exact path/value allow-rules.
patterns = {term: re.compile(pattern) for term, pattern in OVERRIDES["bannedPatterns"].items()}
allow_rules = OVERRIDES["allowRules"]
allowed_hits = set()
violations = []
for asset_name, payload in payloads.items():
    if asset_name in {"manifest.json", "gaps.json"}:
        continue
    for path, value in walk(payload):
        logical = path
        if asset_name == "topics-2.json" and logical.startswith("topics["):
            close = logical.index("]")
            logical = f"topics[{int(logical[7:close]) + 12}]" + logical[close + 1:]
        for term, pattern in patterns.items():
            if not pattern.search(value):
                continue
            match = next((r for r in allow_rules if r.get("term") == term and r.get("path") == logical and r.get("exactValue") == value and re.search(r.get("pattern", patterns[term].pattern), value)), None)
            if match:
                allowed_hits.add((term, logical, value))
            else:
                violations.append((asset_name, logical, term, value))
if violations:
    fail("banned stem violations: " + repr(violations[:10]))
expected_hits = {(r["term"], r["path"], r["exactValue"]) for r in allow_rules}
if allowed_hits != expected_hits:
    fail(f"allow-rule exercise mismatch missing={expected_hits-allowed_hits} unexpected={allowed_hits-expected_hits}")

print(f"TAMIL_CONTENT_PASS assets={len(payloads)} allowlist={len(ALLOW)}")
