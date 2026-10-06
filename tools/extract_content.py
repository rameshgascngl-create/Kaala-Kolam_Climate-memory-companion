#!/usr/bin/env python3
"""Extract Kaala Kolam content tables from the audited HTML in sandboxed Chromium.

The HTML is untrusted input: network requests are blocked and boot() is not run.
Human-approved content corrections come only from tools/content_overrides.json;
generated JSON must never be edited by hand.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any, Iterable

from playwright.sync_api import sync_playwright

EXPECTED_V1_SHA256 = "ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9"
DEFAULT_OVERRIDES = Path(__file__).with_name("content_overrides.json")
DEFAULT_DRAFT_REVIEW = Path(__file__).resolve().parents[1] / "docs" / "tamil-drafts-DRAFT.json"
EXPECTED_GAP_COUNTS = {
    "deepDive": 0,
    "councilDescription": 0,
    "eldersCrossCheck": 0,
    "predictExplanation": 0,
}
TABLE_EXPOSURE = r"""
window.__KK_EXTRACT__ = {
  ui: UI,
  places: PLACES,
  decades: DECADES,
  verdicts: VERD,
  scaleColours: SC,
  scaleTextColours: TXT,
  scaleGlyphs: GLY,
  elderQuestions: Q,
  predictions: PRED,
  councilGroups: GR,
  councilOptions: OPT,
  councilScenarios: SCN,
  councilRoles: ROLES,
  categories: CATS,
  topics: TOP,
  games: GAMES,
  levels: LV,
  words: WORDS,
  demoIds: Object.keys(DEMOS),
  clips: CLIPS
};
"""


def build_elder_slice_b(data: dict[str, Any]) -> dict[str, Any]:
    """Build the Slice-B UI copy from the audited prototype tables/copy.

    Kotlin consumes this generated payload rather than duplicating bilingual
    strings in composables. The lists come directly from PLACES and DECADES.
    """
    return {
        "back": data["ui"]["back"],
        "consent": {
            "en": "Say this aloud first: “I am learning how our weather and nature have changed. May I ask you some questions? I will not write your name.”",
            "ta": "முதலில் இப்படிச் சொல்லுங்கள்: “நம் வானிலையும் இயற்கையும் எப்படி மாறியுள்ளன என்று கற்கிறேன். சில கேள்விகள் கேட்கலாமா? உங்கள் பெயரை எழுத மாட்டேன்.”",
        },
        "decadeLabel": {
            "en": "Decade they were born",
            "ta": "பிறந்த பத்தாண்டு",
        },
        "decades": data["decades"],
        "defaultNickname": {
            "en": "Elder",
            "ta": "மூத்தவர்",
        },
        "delete": {
            "en": "Delete",
            "ta": "நீக்கு",
        },
        "deleteConfirm": {
            "en": "Tap again to delete",
            "ta": "உறுதி செய்ய மீண்டும் தொடு",
        },
        "empty": {
            "en": "No interviews yet. Try asking your grandparent first.",
            "ta": "இன்னும் நேர்காணல்கள் இல்லை. முதலில் உங்கள் தாத்தா அல்லது பாட்டியிடம் கேட்டுப் பாருங்கள்.",
        },
        "intro": {
            "en": "Sit with someone who has lived in your place for decades. Ask permission first. Listen more than you talk. You will record how things changed, how sure they are, and a story if they offer one.",
            "ta": "பல பத்தாண்டுகள் உங்கள் ஊரில் வாழ்ந்தவருடன் அமருங்கள். முதலில் அனுமதி கேளுங்கள். பேசுவதைவிட அதிகம் கேளுங்கள். மாற்றங்கள், அவர் எவ்வளவு உறுதியாகச் சொல்கிறார், அவர் விரும்பினால் ஒரு கதை ஆகியவற்றைப் பதிவு செய்வீர்கள்.",
        },
        "limitMessage": {
            "en": "You can keep up to 50 interviews. Delete one before starting another.",
            "ta": "அதிகபட்சம் 50 நேர்காணல்களை வைத்திருக்கலாம். புதியதைத் தொடங்குவதற்கு முன் ஒன்றை நீக்குங்கள்.",
        },
        "listTitle": {
            "en": "Elder interviews",
            "ta": "மூத்தோர் நேர்காணல்",
        },
        "newTitle": {
            "en": "New interview",
            "ta": "புதிய நேர்காணல்",
        },
        "nicknameLabel": {
            "en": "Nickname (not a real name)",
            "ta": "செல்லப்பெயர் (உண்மைப் பெயர் வேண்டாம்)",
        },
        "nicknamePlaceholder": {
            "en": "e.g. Paati, Colachel",
            "ta": "எ.கா. பாட்டி, கொளச்சல்",
        },
        "placeLabel": {
            "en": "Where they have lived most",
            "ta": "அவர் அதிகம் வாழ்ந்த இடம்",
        },
        "places": data["places"],
        "bornPrefix": {
            "en": "born ",
            "ta": "பிறந்த பத்தாண்டு ",
        },
        "start": data["ui"]["start"],
    }



def build_elder_slice_c(data: dict[str, Any]) -> dict[str, Any]:
    """Build the Slice-C interview-question UI copy from the audited prototype."""
    ui = data["ui"]
    return {
        "askAloud": {
            "en": "Ask this aloud:",
            "ta": "இதை உரக்கக் கேளுங்கள்:",
        },
        "answerGroupLabel": {
            "en": "Their answer",
            "ta": "அவர் பதில்",
        },
        "ratingLabels": [ui[key] for key in ("less2", "less1", "same", "more1", "more2")],
        "scaleColours": data["scaleColours"],
        "scaleTextColours": data["scaleTextColours"],
        "scaleGlyphs": data["scaleGlyphs"],
        "cannotSay": ui["dk"],
        "memoryClarity": {
            "en": "How clear is this memory?",
            "ta": "இந்த நினைவு எவ்வளவு தெளிவு?",
        },
        "confidenceLabels": [ui[key] for key in ("c1", "c2", "c3")],
        "storyLabel": {
            "en": "A story or example they gave (optional)",
            "ta": "அவர் சொன்ன கதை அல்லது எடுத்துக்காட்டு (விருப்பம்)",
        },
        "back": ui["back"],
        "next": ui["next"],
        "skip": ui["skip"],
        "finish": ui["finish"],
        "webStoryMaxUtf16": 400,
    }


def load_overrides(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schemaVersion") != 1:
        raise SystemExit("OVERRIDE_FAIL: unsupported schemaVersion")
    if not isinstance(payload.get("replacements"), list):
        raise SystemExit("OVERRIDE_FAIL: replacements must be a list")
    if not isinstance(payload.get("bannedTerms"), list):
        raise SystemExit("OVERRIDE_FAIL: bannedTerms must be a list")
    if not isinstance(payload.get("allowRules"), list):
        raise SystemExit("OVERRIDE_FAIL: allowRules must be a list")
    if not isinstance(payload.get("pathReplacements", []), list):
        raise SystemExit("OVERRIDE_FAIL: pathReplacements must be a list")
    if not isinstance(payload.get("draftTamil", []), list):
        raise SystemExit("OVERRIDE_FAIL: draftTamil must be a list")
    return payload



def apply_draft_tamil(data: dict[str, Any], entries: list[dict[str, Any]]) -> dict[str, Any]:
    collections = {
        "deepDive": data["topics"],
        "councilDescription": data["councilOptions"],
        "eldersCrossCheck": data["elderQuestions"],
        "predictExplanation": data["predictions"],
    }
    seen: set[str] = set()
    for entry in entries:
        key, kind, item_id, field = entry.get("key"), entry.get("class"), entry.get("id"), entry.get("field")
        english, tamil = entry.get("en"), entry.get("ta")
        if not all(isinstance(v, str) and v for v in (key, kind, item_id, field, english, tamil)):
            raise SystemExit(f"DRAFT_FAIL: malformed draft entry {entry!r}")
        if entry.get("status") != "DRAFT" or entry.get("reviewState") != "NEEDS_OWNER_REVIEW":
            raise SystemExit(f"DRAFT_FAIL: review state is not DRAFT for {key}")
        if key in seen:
            raise SystemExit(f"DRAFT_FAIL: duplicate key {key}")
        seen.add(key)
        if kind not in collections:
            raise SystemExit(f"DRAFT_FAIL: unknown class {kind!r} for {key}")
        target = next((item for item in collections[kind] if item.get("id") == item_id), None)
        if target is None:
            raise SystemExit(f"DRAFT_FAIL: id {item_id!r} not found for {key}")
        current = target.get(field)
        current_en = current.get("en") if isinstance(current, dict) else current
        if current_en != english:
            raise SystemExit(f"DRAFT_FAIL: English source mismatch for {key}")
        if not re.search(r"[\u0B80-\u0BFF]", tamil):
            raise SystemExit(f"DRAFT_FAIL: Tamil letters missing for {key}")
        target[field] = {"en": english, "ta": tamil}
    if len(seen) != 77:
        raise SystemExit(f"DRAFT_FAIL: expected 77 unique entries, found {len(seen)}")
    return data

def apply_replacements(value: Any, replacements: list[dict[str, str]]) -> Any:
    if isinstance(value, str):
        for rule in replacements:
            value = value.replace(rule["from"], rule["to"])
        return value
    if isinstance(value, list):
        return [apply_replacements(v, replacements) for v in value]
    if isinstance(value, dict):
        return {k: apply_replacements(v, replacements) for k, v in value.items()}
    return value



def apply_path_replacements(value: Any, rules: list[dict[str, str]]) -> Any:
    by_path: dict[str, list[dict[str, str]]] = {}
    for rule in rules:
        by_path.setdefault(rule["path"], []).append(rule)

    def visit(node: Any, path: str = "") -> Any:
        if isinstance(node, str):
            result = node
            for rule in by_path.get(path, []):
                if rule["from"] not in result:
                    raise SystemExit(
                        f"OVERRIDE_FAIL: path replacement source not found at {path}: {rule['from']!r}"
                    )
                result = result.replace(rule["from"], rule["to"])
            return result
        if isinstance(node, list):
            return [visit(child, f"{path}[{index}]") for index, child in enumerate(node)]
        if isinstance(node, dict):
            return {
                key: visit(child, f"{path}.{key}" if path else key)
                for key, child in node.items()
            }
        return node

    result = visit(value)
    for rule in rules:
        target = rule["path"]
        # Each rule must have been applied exactly at its declared path.
        if not any(path == target and rule["to"] in text for path, text in walk_strings(result)):
            raise SystemExit(f"OVERRIDE_FAIL: path replacement not observed at {target}")
    return result

def walk_strings(value: Any, path: str = "") -> Iterable[tuple[str, str]]:
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk_strings(child, f"{path}[{index}]")
    elif isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{path}.{key}" if path else key
            yield from walk_strings(child, child_path)


def validate_banned_terms(data: dict[str, Any], overrides: dict[str, Any]) -> list[str]:
    banned_patterns = overrides.get("bannedPatterns", {})
    allow_rules = overrides.get("allowRules", [])
    errors: list[str] = []
    exercised: set[int] = set()
    compiled = {
        term: re.compile(banned_patterns.get(term, re.escape(term)))
        for term in overrides["bannedTerms"]
    }
    for path, text in walk_strings(data):
        for term, pattern in compiled.items():
            if not pattern.search(text):
                continue
            allowed = False
            for index, rule in enumerate(allow_rules):
                if (
                    rule.get("term") == term
                    and rule.get("path") == path
                    and rule.get("exactValue") == text
                    and re.search(rule.get("pattern", re.escape(term)), text)
                ):
                    allowed = True
                    exercised.add(index)
                    break
            if not allowed:
                errors.append(f"{path}: banned term {term!r} in {text!r}")
    missing = [
        f"allowRules[{index}] was not exercised: {rule.get('term')} at {rule.get('path')}"
        for index, rule in enumerate(allow_rules)
        if index not in exercised
    ]
    return errors + missing


def present(value: Any) -> bool:
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, (list, dict)):
        return len(value) > 0
    return value is not None


def validate_bilingual(value: Any, path: str = "$") -> list[str]:
    errors: list[str] = []
    if isinstance(value, dict):
        if "en" in value or "ta" in value:
            if not present(value.get("en")):
                errors.append(f"{path}: empty/missing English field")
            if not present(value.get("ta")):
                errors.append(f"{path}: empty/missing Tamil field")
        for key, child in value.items():
            errors.extend(validate_bilingual(child, f"{path}.{key}"))
    elif isinstance(value, list):
        for idx, child in enumerate(value):
            errors.extend(validate_bilingual(child, f"{path}[{idx}]"))
    return errors


def count_payload(data: dict[str, Any]) -> dict[str, int]:
    return {
        "topics": len(data["topics"]),
        "clips": len(data["clips"]),
        "words": len(data["words"]),
        "elderQuestions": len(data["elderQuestions"]),
        "councilOptions": len(data["councilOptions"]),
        "councilGroups": len(data["councilGroups"]),
        "councilScenarios": len(data["councilScenarios"]),
        "councilRoles": len(data["councilRoles"]),
        "predictions": len(data["predictions"]),
        "demos": len(data["demoIds"]),
        "games": len(data["games"]),
    }


def english_only_text(value: Any, logical_path: str) -> str | None:
    if isinstance(value, str):
        if not value.strip():
            raise SystemExit(f"GAP_FAIL: empty English long-form field at {logical_path}")
        return value
    if isinstance(value, dict):
        en = value.get("en")
        ta = value.get("ta")
        if present(en) and present(ta):
            return None
        if present(en) and not present(ta):
            return str(en)
    raise SystemExit(f"GAP_FAIL: unsupported long-form field shape at {logical_path}")


def build_gaps(data: dict[str, Any]) -> dict[str, Any]:
    gaps: list[dict[str, Any]] = []

    for index, topic in enumerate(data["topics"]):
        logical = f"topics[{index}].d"
        english = english_only_text(topic.get("d"), logical)
        if english is not None:
            asset = "topics-1.json" if index < 12 else "topics-2.json"
            local_index = index if index < 12 else index - 12
            gaps.append({
                "kind": "deepDive",
                "logicalPath": logical,
                "asset": asset,
                "pointer": f"/topics/{local_index}/d",
                "en": english,
            })

    for index, option in enumerate(data["councilOptions"]):
        logical = f"council.options[{index}].de"
        english = english_only_text(option.get("de"), logical)
        if english is not None:
            gaps.append({
                "kind": "councilDescription",
                "logicalPath": logical,
                "asset": "council.json",
                "pointer": f"/options/{index}/de",
                "en": english,
            })

    for index, question in enumerate(data["elderQuestions"]):
        for field in ("sci", "conf", "check"):
            logical = f"elders[{index}].{field}"
            english = english_only_text(question.get(field), logical)
            if english is not None:
                gaps.append({
                    "kind": "eldersCrossCheck",
                    "logicalPath": logical,
                    "asset": "elders.json",
                    "pointer": f"/elderQuestions/{index}/{field}",
                    "en": english,
                })

    for index, prediction in enumerate(data["predictions"]):
        for field in ("truth", "why"):
            logical = f"predictions[{index}].{field}"
            english = english_only_text(prediction.get(field), logical)
            if english is not None:
                gaps.append({
                    "kind": "predictExplanation",
                    "logicalPath": logical,
                    "asset": "predictions.json",
                    "pointer": f"/predictions/{index}/{field}",
                    "en": english,
                })

    counts = {
        kind: sum(1 for item in gaps if item["kind"] == kind)
        for kind in EXPECTED_GAP_COUNTS
    }
    return {
        "schemaVersion": 1,
        "status": "REVIEW_REQUIRED",
        "uiLabel": "English only",
        "count": len(gaps),
        "countsByKind": counts,
        "gaps": gaps,
    }


def write_json(path: Path, payload: Any) -> str:
    serialised = json.dumps(payload, ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n"
    if "�" in serialised:
        raise SystemExit(f"UTF8_FAIL: U+FFFD found in {path}")
    for suspicious in ("Ã", "Â", "â€", "ðŸ"):
        if suspicious in serialised:
            raise SystemExit(f"MOJIBAKE_FAIL: suspicious sequence {suspicious!r} in {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(serialised, encoding="utf-8", newline="\n")
    return hashlib.sha256(serialised.encode("utf-8")).hexdigest()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("html", type=Path)
    ap.add_argument("output_dir", type=Path)
    ap.add_argument("--source-label", default="v1.0 audited HTML")
    ap.add_argument("--expected-sha256")
    ap.add_argument("--chromium-executable", type=Path)
    ap.add_argument("--overrides", type=Path, default=DEFAULT_OVERRIDES)
    ap.add_argument("--draft-review-output", type=Path, default=DEFAULT_DRAFT_REVIEW)
    args = ap.parse_args()

    raw = args.html.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    expected = args.expected_sha256
    if expected and digest != expected:
        raise SystemExit(f"SHA256_MISMATCH expected={expected} actual={digest}")

    text = raw.decode("utf-8", errors="strict")
    if "�" in text:
        raise SystemExit("UTF8_FAIL: U+FFFD found in source")
    marker = "boot();\n})();"
    if text.count(marker) != 1:
        raise SystemExit(f"EXPOSURE_FAIL: expected one terminal boot marker, found {text.count(marker)}")
    instrumented = text.replace(marker, TABLE_EXPOSURE + "\n})();", 1)

    with sync_playwright() as p:
        launch_args: dict[str, Any] = {"headless": True, "args": ["--no-sandbox"]}
        if args.chromium_executable is not None:
            launch_args["executable_path"] = str(args.chromium_executable)
        elif Path("/usr/bin/chromium").exists():
            launch_args["executable_path"] = "/usr/bin/chromium"
        browser = p.chromium.launch(**launch_args)
        context = browser.new_context(offline=True, java_script_enabled=True)
        page = context.new_page()
        page.route("**/*", lambda route: route.abort())
        page.set_content(instrumented, wait_until="domcontentloaded", timeout=30_000)
        data = page.evaluate("() => window.__KK_EXTRACT__")
        browser.close()

    if not isinstance(data, dict):
        raise SystemExit("EXTRACTION_FAIL: exposed payload missing")

    overrides = load_overrides(args.overrides)
    data = apply_replacements(data, overrides["replacements"])
    data = apply_path_replacements(data, overrides.get("pathReplacements", []))
    data = apply_draft_tamil(data, overrides.get("draftTamil", []))
    data["elderSliceB"] = build_elder_slice_b(data)
    data["elderSliceC"] = build_elder_slice_c(data)

    bilingual_errors = validate_bilingual(data)
    if bilingual_errors:
        raise SystemExit("BILINGUAL_FAIL:\n" + "\n".join(bilingual_errors[:50]))

    banned_errors = validate_banned_terms(data, overrides)
    if banned_errors:
        raise SystemExit("BANNED_TERM_FAIL:\n" + "\n".join(banned_errors[:50]))

    gaps = build_gaps(data)
    if gaps["count"] != 0 or gaps["countsByKind"] != EXPECTED_GAP_COUNTS:
        raise SystemExit("GAP_COUNT_FAIL expected=0 actual=" + str(gaps["count"]) + " kinds=" + json.dumps(gaps["countsByKind"], sort_keys=True))
    counts = count_payload(data)
    source = {
        "label": args.source_label,
        "sha256": digest,
        "bytes": len(raw),
        "generator": "tools/extract_content.py",
    }
    gaps["source"] = source

    clip_items = list(data.pop("clips").items())
    topics = data.pop("topics")
    assets: dict[str, Any] = {
        "manifest.json": {"schemaVersion": 1, "source": source, "counts": counts},
        "shared.json": {"schemaVersion": 1, "source": source, "data": {k: data[k] for k in ("ui", "places", "decades", "verdicts", "categories", "levels", "demoIds")}},
        "elders.json": {"schemaVersion": 1, "source": source, "elderQuestions": data["elderQuestions"], "sliceB": data["elderSliceB"], "sliceC": data["elderSliceC"]},
        "predictions.json": {"schemaVersion": 1, "source": source, "predictions": data["predictions"]},
        "council.json": {"schemaVersion": 1, "source": source, "groups": data["councilGroups"], "options": data["councilOptions"], "scenarios": data["councilScenarios"], "roles": data["councilRoles"]},
        "games.json": {"schemaVersion": 1, "source": source, "games": data["games"]},
        "words.json": {"schemaVersion": 1, "source": source, "words": data["words"]},
        "topics-1.json": {"schemaVersion": 1, "source": source, "topics": topics[:12]},
        "topics-2.json": {"schemaVersion": 1, "source": source, "topics": topics[12:]},
        "gaps.json": gaps,
    }
    for batch_index in range(4):
        chunk = clip_items[batch_index * 6:(batch_index + 1) * 6]
        assets[f"clips-{batch_index + 1}.json"] = {
            "schemaVersion": 1,
            "source": source,
            "batch": batch_index + 1,
            "clips": dict(chunk),
        }

    output_hashes: dict[str, str] = {}
    for name, payload in assets.items():
        output_hashes[name] = write_json(args.output_dir / name, payload)

    draft_entries = overrides.get("draftTamil", [])
    draft_review = {
        "schemaVersion": 1,
        "status": "DRAFT",
        "shippedWithApp": True,
        "notice": "These Tamil strings are shipped as DRAFT and require owner review; none is described as reviewed.",
        "source": source,
        "count": len(draft_entries),
        "entries": [
            {"key": item["key"], "kind": item["class"], "id": item["id"], "field": item["field"], "en": item["en"], "taDraft": item["ta"], "status": "DRAFT", "reviewState": "NEEDS_OWNER_REVIEW"}
            for item in draft_entries
        ],
    }
    draft_hash = write_json(args.draft_review_output, draft_review)

    print(f"SOURCE_SHA256={digest}")
    print(f"SOURCE_BYTES={len(raw)}")
    print("COUNTS=" + json.dumps(counts, ensure_ascii=False, sort_keys=True))
    print("GAPS=" + json.dumps(gaps["countsByKind"], ensure_ascii=False, sort_keys=True) + f" TOTAL={gaps['count']}")
    for name in sorted(output_hashes):
        print(f"OUTPUT={args.output_dir / name} SHA256={output_hashes[name]}")
    print(f"DRAFT_REVIEW={args.draft_review_output} SHA256={draft_hash}")
    print("M0_5_EXTRACT_PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
