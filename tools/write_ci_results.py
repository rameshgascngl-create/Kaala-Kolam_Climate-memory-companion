#!/usr/bin/env python3
"""Write machine-readable CI results and a compact GitHub job summary.

The result schema is independent of raw Actions logs. Required steps and jobs
use only PASS, FAIL, or BLOCKED. Informational steps retain their measured
status but do not by themselves make the enclosing job FAIL.
"""
from __future__ import annotations

import argparse
import json
import os
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALID = {"PASS", "FAIL", "BLOCKED"}


def read_json(path: Path):
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def parse_steps(raw: str) -> list[dict]:
    obj = json.loads(raw)
    rows = []
    for name, outcome in obj.items():
        value = (outcome or "skipped").lower()
        if value == "success":
            status = "PASS"
        elif value in {"failure", "cancelled"}:
            status = "FAIL"
        else:
            status = "BLOCKED"
        rows.append(
            {
                "step": name,
                "outcome": value,
                "status": status,
                "required": not name.endswith(" (informational)"),
            }
        )
    return rows


def structural_failure_counts(failures: list[str]) -> dict[str, int]:
    counts = {
        "clipping": 0,
        "truncation": 0,
        "overflow": 0,
        "missing_glyphs": 0,
        "tab_label_fit": 0,
        "line_counts": 0,
        "touch_targets_48dp": 0,
        "colour_tokens": 0,
        "other": 0,
    }
    for failure in failures:
        text = failure.lower()
        if "touch-target" in text:
            key = "touch_targets_48dp"
        elif "tab-label-fit" in text:
            key = "tab_label_fit"
        elif "line-count" in text:
            key = "line_counts"
        elif "truncat" in text:
            key = "truncation"
        elif "clip" in text:
            key = "clipping"
        elif "overflow" in text:
            key = "overflow"
        elif "glyph" in text:
            key = "missing_glyphs"
        elif "colour" in text or "color" in text:
            key = "colour_tokens"
        else:
            key = "other"
        counts[key] += 1
    return counts


def collect_metrics(steps: list[dict]) -> dict:
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
        samples = {}
        for key in sorted(set(web_rows) & set(android_rows)):
            web_row = web_rows[key]
            android_row = android_rows[key]
            chromium_px = float(web_row.get("canvasWidthPx", web_row.get("widthPx", 0)))
            android_px = float(android_row.get("widthPx", 0))
            delta_pct = abs(android_px - chromium_px) / chromium_px * 100.0 if chromium_px else None
            samples[key] = {
                "text": web_row.get("text"),
                "chromiumPx": round(chromium_px, 6),
                "androidPx": round(android_px, 6),
                "deltaPct": None if delta_pct is None else round(delta_pct, 4),
                "chromiumFamily": web_row.get("platformFamily"),
                "androidFamily": android_row.get("resolvedFamily"),
            }
        if samples:
            metrics["text_width_samples"] = samples
            values = [x["deltaPct"] for x in samples.values() if x["deltaPct"] is not None]
            metrics["text_width_max_delta_pct"] = max(values) if values else None

    ssim = ROOT / "docs/ssim-calibration.md"
    if ssim.exists():
        values = {}
        for label, value in re.findall(
            r"\|\s*([^|]+?)\s*\|\s*(-?0\.\d+)\s*\|",
            ssim.read_text(encoding="utf-8"),
        ):
            values[label.strip()] = float(value)
        if values:
            metrics["ssim_calibration"] = values

    hard = read_json(ROOT / "app/build/fidelity/hard-report.json")
    if hard:
        failures = [str(x) for x in hard.get("failures", [])]
        counts = structural_failure_counts(failures)
        step_map = {x["step"]: x["status"] for x in steps}
        if step_map.get("Missing glyph coverage") == "FAIL":
            counts["missing_glyphs"] += 1
        if step_map.get("Exact colour tokens") == "FAIL":
            counts["colour_tokens"] += 1
        metrics["hard"] = {
            "passed": bool(hard.get("passed")),
            "failureCount": int(hard.get("failureCount", len(failures))),
            "checks": int(hard.get("checks", 0)),
            "rows": int(hard.get("rows", 0)),
            "failureCounts": counts,
            "failures": failures,
        }

    parity = read_json(ROOT / "app/build/fidelity/parity-report.json")
    if parity:
        metrics["parity"] = {
            "passed": bool(parity.get("passed")),
            "failureCount": int(parity.get("failureCount", 0)),
            "informationalFailureCount": int(parity.get("informationalFailureCount", 0)),
            "baselineCommit": parity.get("baselineCommit"),
            "screenStatus": parity.get("screenStatus", {}),
            "failures": parity.get("failures", []),
            "informational": parity.get("informational", []),
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
    parser.add_argument("--blocked-reason", default="")
    args = parser.parse_args()

    steps = parse_steps(args.steps_json)
    counts = Counter(row["status"] for row in steps)
    blocked_reason = args.blocked_reason.strip() or None
    required = [row for row in steps if row["required"]]

    if blocked_reason:
        job_status = "BLOCKED"
    elif any(row["status"] == "FAIL" for row in required):
        job_status = "FAIL"
    elif any(row["status"] == "BLOCKED" for row in required):
        job_status = "BLOCKED"
    else:
        job_status = "PASS"

    if job_status not in VALID:
        raise SystemExit(f"CI_RESULTS_FAIL invalid status {job_status}")

    payload = {
        "schemaVersion": 2,
        "job": args.job,
        "headSha": args.head,
        "status": job_status,
        "blockedReason": blocked_reason,
        "steps": steps,
        "counts": {key: counts.get(key, 0) for key in ("PASS", "FAIL", "BLOCKED")},
        "metrics": collect_metrics(steps),
    }

    out_dir = ROOT / "app/build/ci-results" / args.job
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / "ci-results.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as stream:
            stream.write(f"## {args.job}: {job_status}\n\n")
            stream.write(f"HEAD: `{args.head}`\n\n")
            if blocked_reason:
                stream.write(f"Blocked: {blocked_reason}\n\n")
            stream.write("| Step | Required | Result | Outcome |\n|---|---|---|---|\n")
            for row in steps:
                stream.write(
                    f"| {row['step']} | {'yes' if row['required'] else 'no'} | "
                    f"**{row['status']}** | {row['outcome']} |\n"
                )
            stream.write("\n")
            stream.write(
                f"Counts: PASS {counts.get('PASS', 0)} · FAIL {counts.get('FAIL', 0)} · "
                f"BLOCKED {counts.get('BLOCKED', 0)}\n\n"
            )
            if payload["metrics"]:
                stream.write("Metrics:\n\n")
                for key, value in payload["metrics"].items():
                    stream.write(f"- {key}: {json.dumps(value, ensure_ascii=False)}\n")
                stream.write("\n")

    print(
        f"CI_RESULTS_WRITTEN job={args.job} status={job_status} "
        f"pass={counts.get('PASS', 0)} fail={counts.get('FAIL', 0)} "
        f"blocked={counts.get('BLOCKED', 0)} out={out}"
    )


if __name__ == "__main__":
    main()
