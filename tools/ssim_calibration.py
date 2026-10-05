#!/usr/bin/env python3
"""Calibrate SSIM preprocessing with positive and negative controls."""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageFilter
import numpy as np
from skimage.metrics import structural_similarity

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "tests/golden-web/390x844/en/dark/home.png"
LEARN = ROOT / "tests/golden-web/390x844/en/dark/learn.png"
EXTRA = ROOT / "tests/ssim-controls/home-extra-wrap.png"
REPORT = ROOT / "docs/ssim-calibration.md"

SCALE = 0.25
BLUR_RADIUS = 2.0


def preprocess_image(image: Image.Image) -> np.ndarray:
    gray = image.convert("L").filter(ImageFilter.GaussianBlur(BLUR_RADIUS))
    size = (max(1, round(gray.width * SCALE)), max(1, round(gray.height * SCALE)))
    return np.asarray(gray.resize(size, Image.Resampling.LANCZOS), dtype=np.float32)


def load(path: Path) -> np.ndarray:
    with Image.open(path) as image:
        return preprocess_image(image)


def shift_one(path: Path) -> np.ndarray:
    with Image.open(path) as source:
        gray = source.convert("L")
        shifted = Image.new("L", gray.size, gray.getpixel((0, 0)))
        shifted.paste(gray, (1, 0))
        return preprocess_image(shifted)


def score(a: np.ndarray, b: np.ndarray) -> float:
    return float(structural_similarity(a, b, data_range=255))


def main() -> None:
    home = load(HOME)
    shift = shift_one(HOME)
    learn = load(LEARN)
    extra = load(EXTRA)
    scores = {
        "home_vs_home_1px_shift": score(home, shift),
        "home_vs_learn": score(home, learn),
        "home_vs_home_extra_wrap": score(home, extra),
    }
    for key, value in scores.items():
        print(f"SSIM_CALIBRATION {key}={value:.6f}")
    if scores["home_vs_home_1px_shift"] < 0.97:
        raise SystemExit("SSIM_CALIBRATION_FAIL 1px-shift positive control below 0.97")
    if scores["home_vs_home_1px_shift"] - scores["home_vs_home_extra_wrap"] < 0.01:
        raise SystemExit("SSIM_CALIBRATION_FAIL extra-wrap negative control not separated")
    if scores["home_vs_home_1px_shift"] - scores["home_vs_learn"] < 0.05:
        raise SystemExit("SSIM_CALIBRATION_FAIL Home-vs-Learn negative control not separated")
    REPORT.write_text(
        "# SSIM calibration\n\n"
        "Preprocessing: grayscale; Gaussian blur radius 2 px at source resolution; "
        "downscale to 25% with Lanczos.\n\n"
        "| Control | SSIM |\n|---|---:|\n"
        f"| Home vs Home shifted 1 px | {scores['home_vs_home_1px_shift']:.6f} |\n"
        f"| Home vs Learn | {scores['home_vs_learn']:.6f} |\n"
        f"| Home vs Home with one extra wrapped line | {scores['home_vs_home_extra_wrap']:.6f} |\n\n"
        "The preprocessing is accepted only because the one-pixel positive control "
        "remains high while both negative controls are measurably separated.\n",
        encoding="utf-8",
    )
    print("SSIM_CALIBRATION_PASS preprocessing=grayscale+blur2+scale25")


if __name__ == "__main__":
    main()
