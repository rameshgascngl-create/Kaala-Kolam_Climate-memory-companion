# Native build status

Branch: **native-v2**

No APK, AAB, tag or build from this branch is release, final or authoritative while any item below is **PARTIAL** or **PLACEHOLDER**.

Status meanings:

- **DONE** — implemented and its required HARD/PARITY evidence passes.
- **PARTIAL** — meaningful native implementation exists, but behaviour/content/fidelity is incomplete or a required gate has not passed.
- **PLACEHOLDER** — the prototype feature is not yet faithfully implemented natively.

## Six primary areas

| Area | Status | Current native state | Missing before DONE |
|---|---|---|---|
| Home | **PARTIAL** | Native hero, entry cards, kolam/progress, route cards, note and footer exist. | HARD/PARITY must pass; shared chrome/card geometry still needs correction. |
| Learn | **PARTIAL** | Learn home, one topic-detail scaffold, one game scaffold and a short word-list scaffold exist. | Full 24-topic content/clip parity, all demos, both functional games, full word list, topic navigation/player controls and fidelity gates. |
| Elders | **PARTIAL** | Introductory screen and start-interview control exist. | Interview setup/questions, confidence/story capture, review, code export/import and prototype parity. |
| Class | **PARTIAL** | Code-entry/sample-data scaffold exists. | Real code parsing, pooled aggregation, grouping/summary behaviour and prototype parity. |
| Council | **PARTIAL** | Role, scenario and budget scaffolds exist. | Full role/scenario consequences, selectable interventions, budget mechanics, deliberation result/export and prototype parity. |
| Predict | **PARTIAL** | One-question visual scaffold, confidence controls and range drawing exist. | Functional slider, all 8 questions, reveal/scoring/calibration summary and prototype parity. |

## Learn feature inventory

The audited HTML prototype contains **24 topics/clips**, **3 interactive demos**, **2 games** and **77 word-list rows**.

| Feature group | Status | Native coverage | Missing before DONE |
|---|---|---:|---|
| 24 animated clips | **PLACEHOLDER** | 0/24 faithful clip players; one generic Sun animation-preview scaffold is present. | Port all 24 clip step sequences, captions, progress/playback controls and bilingual labels. |
| 3 demos | **PLACEHOLDER** | 0/3 | Port Water, Sea/Land Breeze and Greenhouse interactive demos. |
| 2 games | **PARTIAL** | Weather-or-Climate visual scaffold only; Myth-or-Fact absent. | Make Weather-or-Climate fully interactive across all questions/results and implement Myth-or-Fact. |
| Tamil-English word list | **PARTIAL** | 4/77 rows | Port all 77 rows, search/filter behaviour, review-warning state and prototype layout. |

## Fidelity and localisation gates

- Reference captures use a fresh browser context per state and assert `scrollY == 0` plus a visible top element.
- Playwright and Compose use pinned subsetted OFL Noto families: serif display and sans body, including Tamil-specific families.
- CSS typography tokens are generated into Compose; no Material typography substitution is accepted as visual parity.
- Legacy full-resolution pixel-difference matrices are informational only.
- HARD is the zero-failure structural gate: colour tokens, text overflow/truncation, heading/label/button line counts, body line-count tolerance, tab fit, missing glyphs and >=48 dp interactive semantics bounds.
- PARITY is the tagged-geometry/SSIM regression gate. Geometry uses matching runtime-only HTML tags and Compose testTags with a 4 dp absolute tolerance.
- A parity baseline may change only as a deliberate reviewed change with a recorded reason; it must never be edited merely to hide a regression.

## Remaining milestones, in order

1. Make the font-width probe pass at <=1% for all four pinned-font samples.
2. Complete log-independent CI result artefacts and summaries for every job.
3. Run and enforce HARD and PARITY as separate jobs; establish the initial documented parity baseline without weakening the 4 dp absolute requirement.
4. Generate the 390x844 EN/TA six-screen contact sheet with per-screen HARD/PARITY results.
5. Correct shared chrome first: top bar height/placement, then bottom tab-bar offset and label fit.
6. Correct shared cards and chips.
7. Re-run both 320 and 390 dp matrices and correct screen-specific geometry/line-count defects.
8. Port the remaining 24 clips, 3 demos, second game, full first game, full 77-row word list and incomplete Elders/Class/Council/Predict workflows.
9. Run device/accessibility QA only after automated HARD/PARITY gates are green.
10. Release-signing/tagging can be considered only after every inventory item above is DONE.
