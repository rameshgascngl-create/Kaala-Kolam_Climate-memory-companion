#!/usr/bin/env python3
"""Write machine-readable CI results and a compact GitHub job summary.

This reporter is deliberately independent of raw Actions logs. It consumes the
step outcomes supplied by the workflow and, when present, reads generated
metric artefacts directly from the workspace.
"""
from __future__ import annotations

import argparse
import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read_json(path: Path):
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def parse_steps(raw: str) -> list[dict]:
    obj = json.loads(raw)
    rows = []
    for name, outcome in obj.items():
        value = (outcome or "skipped").lower()
        status = "PASS" if value == "success" else "FAIL" if value in {"failure", "cancelled"} else "SKIP"
        rows.append({"step": name, "outcome": value, "status": status})
    return rows


def collect_metrics() -> dict:
    metrics: dict[str, object] = {}

    web = read_json(ROOT / "tests/golden-web/manifest.json")
    if web:
        metrics["golden_web_count"] = int(web.get("captureCount", 0))

    tamil = read_json(ROOT / "tests/golden-tamil/manifest.json")
    if tamil:
        metrics["golden_tamil_count"] = int(tamil.get("captureCount", 0))

    chromium = read_json(ROOT / "tests/reference-metrics/text-widths.json")
    android = read_json(ROOT / "app/build/fidelity/android-text-widths.json")
    if chromium and android:
        web_rows = {x["id"]: x for x in chromium.get("samples", [])}
        android_rows = {x["id"]: x for x in android.get("samples", [])}
        deltas = {}
        for key in sorted(set(web_rows) & set(android_rows)):
            w = float(web_rows[key].get("canvasWidthPx", web_rows[key].get("widthPx", 0)))
            a = float(android_rows[key].get("widthPx", 0))
            if w:
                deltas[key] = round(abs(a - w) / w * 100.0, 4)
        if deltas:
            metrics["text_width_delta_pct"] = deltas
            metrics["text_width_max_delta_pct"] = max(deltas.values())

    ssim = ROOT / "docs/ssim-calibration.md"
    if ssim.exists():
        vals = {}
        for label, value in re.findall(r"\|\s*([^|]+?)\s*\|\s*(0\.\d+)\s*\|", ssim.read_text(encoding="utf-8")):
            vals[label.strip()] = float(value)
        if vals:
            metrics["ssim_calibration"] = vals

    hard = read_json(ROOT / "app/build/fidelity/hard-report.json")
    if hard:
        metrics["hard"] = {
            "passed": bool(hard.get("passed")),
            "failureCount": int(hard.get("failureCount", 0)),
            "checks": int(hard.get("checks", 0)),
            "rows": int(hard.get("rows", 0)),
        }

    parity = read_json(ROOT / "app/build/fidelity/parity-report.json")
    if parity:
        metrics["parity"] = {
            "passed": bool(parity.get("passed")),
            "failureCount": int(parity.get("failureCount", 0)),
            "baselineCommit": parity.get("baselineCommit"),
        }

    contact = read_json(ROOT / "app/build/fidelity/contact-sheet-390x844.json")
    if contact:
        metrics["contact_sheet_rows"] = len(contact.get("rows", []))

    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    if apk.exists():
        metrics["debug_apk_bytes"] = apk.stat().st_size

    return metrics


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--job", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--steps-json", required=True)
    args = parser.parse_args()

    steps = parse_steps(args.steps_json)
    counts = {
        "pass": sum(x["status"] == "PASS" for x in steps),
        "fail": sum(x["status"] == "FAIL" for x in steps),
        "skip": sum(x["status"] == "SKIP" for x in steps),
    }
    payload = {
        "schemaVersion": 1,
        "job": args.job,
        "headSha": args.head,
        "steps": steps,
        "counts": counts,
        "metrics": collect_metrics(),
    }

    out_dir = ROOT / "app/build/ci-results" / args.job
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / "ci-results.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as stream:
            stream.write(f"## {args.job}\n\n")
            stream.write(f"HEAD: {args.head}\n\n")
            stream.write("| Step | Result | Outcome |\n|---|---|---|\n")
            for row in steps:
                stream.write(f"| {row['step']} | **{row['status']}** | {row['outcome']} |\n")
            stream.write("\n")
            stream.write(
                f"Counts: PASS {counts['pass']} · FAIL {counts['fail']} · SKIP {counts['skip']}\n\n"
            )
            if payload["metrics"]:
                stream.write("Metrics:\n\n")
                for key, value in payload["metrics"].items():
                    stream.write(f"- {key}: {json.dumps(value, ensure_ascii=False)}\n")
                stream.write("\n")

    print(
        f"CI_RESULTS_WRITTEN job={args.job} pass={counts['pass']} "
        f"fail={counts['fail']} skip={counts['skip']} out={out}"
    )


if __name__ == "__main__":
    main()
