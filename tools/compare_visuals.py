#!/usr/bin/env python3
"""Compare native JVM screenshots with the HTML prototype reference matrix.

The metric is intentionally renderer-agnostic:
- RGB mean absolute error (MAE), normalized to 0..1
- percentage of pixels whose maximum channel error is greater than 16/255

A state passes only when both limits pass. Missing or differently-sized images
are an unconditional failure. Use --fail-on-diff only for a milestone whose
entire scoped matrix is expected to be complete.
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_WEB = ROOT / "tests" / "golden-web"
DEFAULT_NATIVE = ROOT / "app" / "build" / "visual-native"
DEFAULT_REPORT = ROOT / "docs" / "visual-parity-report.md"


@dataclass(frozen=True)
class Result:
    rel: Path
    state: str
    passed: bool
    mae: float | None
    changed: float | None
    note: str = ""


def compare_pair(
    reference: Path,
    native: Path,
    rel: Path,
    max_mae: float,
    max_changed: float,
    channel_tolerance: int,
) -> Result:
    state = rel.stem
    if not native.exists():
        return Result(rel, state, False, None, None, "native screenshot missing")

    with Image.open(reference) as ref_image, Image.open(native) as native_image:
        ref = ref_image.convert("RGB")
        got = native_image.convert("RGB")
        if ref.size != got.size:
            return Result(
                rel,
                state,
                False,
                None,
                None,
                f"size {got.width}x{got.height}, expected {ref.width}x{ref.height}",
            )

        diff = ImageChops.difference(ref, got)
        means = ImageStat.Stat(diff).mean[:3]
        mae = sum(means) / (3.0 * 255.0)

        changed_pixels = 0
        total_pixels = ref.width * ref.height
        for red, green, blue in diff.getdata():
            if max(red, green, blue) > channel_tolerance:
                changed_pixels += 1
        changed = changed_pixels / total_pixels if total_pixels else 1.0
        passed = mae <= max_mae and changed <= max_changed
        return Result(rel, state, passed, mae, changed)


def fmt(value: float | None) -> str:
    return "—" if value is None else f"{value * 100:.3f}%"


def build_report(
    results: list[Result],
    max_mae: float,
    max_changed: float,
    channel_tolerance: int,
) -> str:
    grouped: dict[str, list[Result]] = defaultdict(list)
    for result in results:
        grouped[result.state].append(result)

    lines = [
        "# Visual parity report",
        "",
        "Reference: HTML prototype captured with Playwright under tests/golden-web.",
        "Native renderer: Paparazzi JVM screenshots under app/build/visual-native.",
        "",
        "PASS thresholds:",
        f"- normalized RGB MAE <= {max_mae * 100:.3f}%",
        f"- changed pixels <= {max_changed * 100:.3f}% using max-channel tolerance {channel_tolerance}/255",
        "- dimensions must match exactly",
        "",
        "## State summary",
        "",
        "| State | Matrix | Result | Mean MAE | Worst MAE | Mean changed | Worst changed |",
        "|---|---:|---|---:|---:|---:|---:|",
    ]
    for state in sorted(grouped):
        items = grouped[state]
        numeric = [item for item in items if item.mae is not None and item.changed is not None]
        passed = len(items) == 8 and all(item.passed for item in items)
        mean_mae = sum(item.mae for item in numeric) / len(numeric) if numeric else None
        max_item_mae = max((item.mae for item in numeric), default=None)
        mean_changed = (
            sum(item.changed for item in numeric) / len(numeric) if numeric else None
        )
        max_item_changed = max((item.changed for item in numeric), default=None)
        lines.append(
            f"| {state} | {sum(item.passed for item in items)}/{len(items)} | "
            f"**{'PASS' if passed else 'FAIL'}** | {fmt(mean_mae)} | "
            f"{fmt(max_item_mae)} | {fmt(mean_changed)} | {fmt(max_item_changed)} |"
        )

    lines.extend(
        [
            "",
            "## Per-screen measurements",
            "",
            "| Viewport | Language | Theme | State | Result | MAE | Changed pixels | Note |",
            "|---|---|---|---|---|---:|---:|---|",
        ]
    )
    for result in results:
        parts = result.rel.parts
        viewport, language, theme = parts[0], parts[1], parts[2]
        lines.append(
            f"| {viewport} | {language} | {theme} | {result.state} | "
            f"**{'PASS' if result.passed else 'FAIL'}** | {fmt(result.mae)} | "
            f"{fmt(result.changed)} | {result.note} |"
        )

    total_pass = sum(result.passed for result in results)
    lines.extend(
        [
            "",
            f"Overall matrix: **{total_pass}/{len(results)} PASS**.",
            "",
            "This report is a visual-fidelity gate, not a release gate. Any PARTIAL or "
            "PLACEHOLDER screen remains non-authoritative regardless of a renderer result.",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--web", type=Path, default=DEFAULT_WEB)
    parser.add_argument("--native", type=Path, default=DEFAULT_NATIVE)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--max-mae", type=float, default=0.035)
    parser.add_argument("--max-changed", type=float, default=0.20)
    parser.add_argument("--channel-tolerance", type=int, default=16)
    parser.add_argument("--fail-on-diff", action="store_true")
    args = parser.parse_args()

    references = sorted(args.web.glob("*/*/*/*.png"))
    if len(references) != 72:
        raise SystemExit(f"Expected 72 web references, found {len(references)}")

    results: list[Result] = []
    for reference in references:
        rel = reference.relative_to(args.web)
        results.append(
            compare_pair(
                reference,
                args.native / rel,
                rel,
                args.max_mae,
                args.max_changed,
                args.channel_tolerance,
            )
        )

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        build_report(
            results,
            args.max_mae,
            args.max_changed,
            args.channel_tolerance,
        ),
        encoding="utf-8",
    )
    passed = sum(result.passed for result in results)
    print(f"VISUAL_PARITY_MATRIX pass={passed} fail={len(results) - passed}")
    if args.fail_on_diff and passed != len(results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
