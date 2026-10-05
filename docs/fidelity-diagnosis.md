# Fidelity diagnosis

Branch: `native-v2`

Diagnostic baseline before the reference-harness correction:
`24b06691b6c61b9cd8cc7821a8face5d31aee8a3` (Native Android CI Run #70).

The purpose of this document is to separate structural defects from renderer
noise before any fidelity baseline is enforced. A green report-only workflow is
not evidence of visual fidelity.

## What TAMIL_VISUAL_MATRIX measures

`tools/compare_tamil_visuals.py` is a screenshot-difference metric. For each
Tamil Playwright reference and Paparazzi native screenshot it checks:

- identical pixel dimensions;
- normalised RGB mean absolute error (MAE) <= 3.5%;
- changed pixels <= 20%, where a pixel is counted as changed when its maximum
  channel error exceeds 16/255.

Therefore `TAMIL_VISUAL_MATRIX pass=2 fail=78` in Run #70 did **not** mean
78 clipping, truncation, overflow or missing-glyph defects. It meant that 78
whole screenshots exceeded one or both pixel-difference thresholds.

The matrix cannot identify clipping, line-count errors, touch-target failures,
missing glyphs, geometry mismatches or an invalid reference capture. Those need
separate structural gates.

## Primary-cause accounting: 66 visual-matrix failures

The categories below are exclusive primary causes and sum to 66. Secondary
causes, especially spacing and typography reflow, overlap.

| Primary cause | Failures | Diagnosis |
|---|---:|---|
| Font not applied | 0 | Pinned font plumbing is present on both sides. |
| Wrap or line-count mismatch | 26 | Primarily Tamil reflow in Home/Learn/Elders/Class/Council/Predict plus the two 320-dp Tamil learn-game states. |
| Spacing or geometry | 24 | The six ordinary English screens across 2 widths x 2 themes. |
| Colour | 0 | Core prototype colour tokens occur exactly in both renderers. |
| Missing element or content | 8 | All learn-topic states; native does not yet reproduce the full prototype animation/player structure. |
| Clipping | 0 identified by the pixel matrix | The screenshot metric cannot prove clipping absence. |
| Other | 8 | All learn-words states; the old Playwright harness retained scroll position between states. |
| **Total** | **66** | |

## Primary-cause accounting: 78 Tamil-matrix failures

| Primary cause | Failures | Diagnosis |
|---|---:|---|
| Font not applied | 0 | Pinned font resources/injection are present. |
| Wrap or line-count mismatch | 54 | Home/Learn/Elders/Class/Council/Predict across all Tamil stress combinations plus six failing learn-game combinations. |
| Spacing | 0 as exclusive primary cause | It remains a substantial secondary contributor. |
| Colour | 0 | Exact palette tokens are present. |
| Missing element or content | 16 | learn-topic and learn-deep, eight states each. |
| Clipping | 0 identified by the screenshot metric | Must be checked structurally. |
| Other | 8 | learn-words references were contaminated by retained Playwright scroll state. |
| **Total** | **78** | |

## Reference-harness defect

The old reference capture reused one browser context/page across multiple states.
The learn-game locator click could scroll the page, and the subsequent
learn-words capture inherited that scroll position. The result could place the
learn-words back control under the sticky header.

The corrected harness uses a fresh browser context for every state and requires:

1. `window.scrollY == 0`;
2. a state-specific visible top element;
3. that element's top edge to be at or below the sticky header and within the
   viewport.

The shipped HTML remains unchanged.

## Pinned fonts

The Playwright harness injects harness-only `@font-face` rules using the same
subsetted OFL font files that are packaged for Compose.

Expected resolved families:

| Role | English | Tamil |
|---|---|---|
| Display | Noto Serif | Noto Serif Tamil |
| Body | Noto Sans | Noto Sans Tamil |

The native theme maps 400/500/600/700 weights explicitly to the matching font
resources. Run #70 reported
`PINNED_FONT_COVERAGE_PASS files=16 tamil_codepoints=46 bytes=639308`.

A separate width probe is required before the fonts are treated as metrically
equivalent across Chromium and Android text measurement.

## Shared geometry diagnosis

The common chrome is a shared cause and must be corrected before individual
screens:

- reference top bar: approximately 64 px;
- native top bar: approximately 62 px;
- bottom tab boundary is also offset;
- ordinary screens show a repeated vertical displacement that compounds
  screenshot differences.

Cards and chips are the next shared layer after top and bottom chrome.

## Touch targets

The HTML prototype contains visual controls whose visible bounds are 44 or 46
px/dp. The accessibility requirement is >=48 dp for interactive semantics.
The intended correction is to preserve prototype visual dimensions while
expanding the semantic/touch target using
`minimumInteractiveComponentSize` or an equivalent mechanism.

## SSIM calibration

Candidate preprocessing was evaluated before making SSIM a gate. Images were
converted to grayscale, blurred before downscaling, then compared with SSIM.

| Preprocessing | Worst SSIM for HTML compared with itself after a 1 px shift |
|---|---:|
| 50% scale, Gaussian blur 1 px | 0.9317 |
| 50% scale, Gaussian blur 2 px | 0.9664 |
| 25% scale, Gaussian blur 2 px | **0.9781** |
| 25% scale, Gaussian blur 3 px | **0.9823** |

The initial candidate is 25% linear scale plus a 2 px Gaussian blur before
downscaling. This is not yet a fidelity threshold. It must pass negative
controls (Home vs Learn, and Home vs Home with one forced extra wrapped line)
before it can be used for regression gating.

## Gate design

### HARD — zero failures

Must fail the build on any of:

- clipping;
- truncation;
- overflow;
- missing glyphs;
- tab-label fit failure;
- non-exact colour tokens;
- heading/label/button line-count mismatch at 320 and 390 dp;
- body-paragraph line-count difference greater than one line;
- interactive semantic bounds below 48 dp.

### PARITY — regression gate

Must compare tagged element boxes with a 4 dp tolerance and use SSIM on blurred,
downscaled grayscale images. HTML tags are injected only by the Playwright
harness at runtime; shipped HTML is unchanged. Compose uses matching
`testTag` values.

The committed baseline records measured geometry and SSIM. CI fails when the
new result regresses beyond the documented tolerance. A baseline must never be
edited solely to hide a regression; any accepted baseline change must record
its reason in the pull request.

## Correction order

1. Reference-harness determinism.
2. Pinned-font metric probe and SSIM negative controls.
3. HARD and PARITY gates.
4. Contact-sheet evidence.
5. Shared chrome: top bar, tab bar.
6. Shared cards and chips.
7. Individual screens.
8. Remaining implementation gaps, including topic/player parity.
