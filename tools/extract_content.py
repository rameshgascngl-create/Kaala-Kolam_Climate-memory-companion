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
    "deepDive": 24,
    "councilDescription": 7,
    "eldersCrossCheck": 30,
    "predictExplanation": 16,
}
TABLE_EXPOSURE = r"""
window.__KK_EXTRACT__ = {
  ui: UI,
  places: PLACES,
  decades: DECADES,
  verdicts: VERD,
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
    return payload


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
    if counts != EXPECTED_GAP_COUNTS or len(gaps) != 77:
        raise SystemExit(
            "GAP_COUNT_FAIL expected="
            + json.dumps(EXPECTED_GAP_COUNTS, sort_keys=True)
            + " actual="
            + json.dumps(counts, sort_keys=True)
            + f" total={len(gaps)}"
        )
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

    bilingual_errors = validate_bilingual(data)
    if bilingual_errors:
        raise SystemExit("BILINGUAL_FAIL:\n" + "\n".join(bilingual_errors[:50]))

    banned_errors = validate_banned_terms(data, overrides)
    if banned_errors:
        raise SystemExit("BANNED_TERM_FAIL:\n" + "\n".join(banned_errors[:50]))

    gaps = build_gaps(data)
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
        "elders.json": {"schemaVersion": 1, "source": source, "elderQuestions": data["elderQuestions"]},
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

    draft_review = {
        "schemaVersion": 1,
        "status": "DRAFT",
        "shippedWithApp": False,
        "notice": "Human-review worksheet only. No machine-translated Tamil has been generated or shipped.",
        "source": source,
        "count": gaps["count"],
        "entries": [
            {
                "logicalPath": item["logicalPath"],
                "kind": item["kind"],
                "en": item["en"],
                "taDraft": None,
                "reviewState": "NEEDS_OWNER_TRANSLATION",
            }
            for item in gaps["gaps"]
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
