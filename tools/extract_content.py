#!/usr/bin/env python3
"""Extract Kaala Kolam content tables from the audited HTML in a sandboxed Chromium page.

The source is treated as untrusted: no network requests are permitted and boot() is not run.
The script instruments a temporary in-memory copy only, exposing data tables for extraction.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from playwright.sync_api import sync_playwright

EXPECTED_V1_SHA256 = "ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9"

APPROVED_REPLACEMENTS = {
    "ஆல்கா": "நுண்பாசி",
    "கார்பன் டையாக்சைடு": "கார்பன் டை ஆக்சைடு",
    "தரைக் காற்று": "நிலக்காற்று",
    "கடல் காற்று": "கடற்காற்று",
    "பனிப்பாறை": "பனியாறு",
    "குளிர்சாதனம்": "குளிரூட்டி",
    "சதுப்புநிலக் காடுகள்": "அலையாத்திக் காடுகள்",
    "தழுவல்": "தகவமைப்பு",
    "அழுத்தம்": "காற்றழுத்தம்",
    "காப்பகம்": "பாதுகாப்பு மையம்",
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


def normalise(value: Any) -> Any:
    if isinstance(value, str):
        for old, new in APPROVED_REPLACEMENTS.items():
            value = value.replace(old, new)
        return value
    if isinstance(value, list):
        return [normalise(v) for v in value]
    if isinstance(value, dict):
        return {k: normalise(v) for k, v in value.items()}
    return value


def validate_bilingual(value: Any, path: str = "$") -> list[str]:
    errors: list[str] = []
    if isinstance(value, dict):
        if "en" in value or "ta" in value:
            en = value.get("en")
            ta = value.get("ta")

            def present(v: Any) -> bool:
                if isinstance(v, str):
                    return bool(v.strip())
                if isinstance(v, (list, dict)):
                    return len(v) > 0
                return v is not None

            if not present(en):
                errors.append(f"{path}: empty/missing English field")
            if not present(ta):
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


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("html", type=Path)
    ap.add_argument("output_dir", type=Path)
    ap.add_argument("--source-label", default="v1.0 audited HTML")
    ap.add_argument("--expected-sha256")
    ap.add_argument("--chromium-executable", type=Path)
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
    data = normalise(data)
    bilingual_errors = validate_bilingual(data)
    if bilingual_errors:
        raise SystemExit("BILINGUAL_FAIL:\n" + "\n".join(bilingual_errors[:50]))

    counts = count_payload(data)
    source = {
        "label": args.source_label,
        "sha256": digest,
        "bytes": len(raw),
        "generator": "tools/extract_content.py",
    }
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
    }
    for batch_index in range(4):
        chunk = clip_items[batch_index * 6:(batch_index + 1) * 6]
        assets[f"clips-{batch_index + 1}.json"] = {
            "schemaVersion": 1,
            "source": source,
            "batch": batch_index + 1,
            "clips": dict(chunk),
        }

    args.output_dir.mkdir(parents=True, exist_ok=True)
    output_hashes = {}
    for name, payload in assets.items():
        serialised = json.dumps(payload, ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n"
        for banned in APPROVED_REPLACEMENTS:
            if banned in serialised:
                raise SystemExit(f"BANNED_TERM_FAIL: {banned} in {name}")
        if "�" in serialised:
            raise SystemExit(f"UTF8_FAIL: U+FFFD found in {name}")
        for suspicious in ("Ã", "Â", "â€", "ðŸ"):
            if suspicious in serialised:
                raise SystemExit(f"MOJIBAKE_FAIL: suspicious sequence {suspicious!r} in {name}")
        out = args.output_dir / name
        out.write_text(serialised, encoding="utf-8", newline="\n")
        output_hashes[name] = hashlib.sha256(serialised.encode("utf-8")).hexdigest()

    print(f"SOURCE_SHA256={digest}")
    print(f"SOURCE_BYTES={len(raw)}")
    print("COUNTS=" + json.dumps(counts, ensure_ascii=False, sort_keys=True))
    for name in sorted(output_hashes):
        print(f"OUTPUT={args.output_dir / name} SHA256={output_hashes[name]}")
    print("M0_EXTRACT_PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
