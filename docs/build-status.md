# Native build status

Branch: **native-v2**

No APK, AAB, tag or build from this branch is release, final or authoritative while any item below is **PARTIAL** or **PLACEHOLDER**.

Status meanings:

- **DONE** — implemented and its required automated evidence passes.
- **PARTIAL** — meaningful native implementation exists, but behaviour, coverage or required fidelity/device evidence is incomplete.
- **PLACEHOLDER** — the prototype feature is not yet faithfully implemented natively.

## Product areas and feature inventory

| Area / feature | Status | Current native state | Missing before DONE |
|---|---|---|---|
| Home | **PARTIAL** | Native hero, entry cards, kolam/progress, route cards, note and footer exist. | HARD/PARITY must pass; shared chrome/card geometry still needs correction and device QA. |
| Learn | **PARTIAL** | Learn home, one topic-detail scaffold, one game scaffold and a short word-list scaffold exist. | Full 24-topic/clip parity, all demos, both functional games, full word list, topic navigation/player controls and fidelity gates. |
| Elders | **PARTIAL** | Introductory screen and start-interview control exist. | Interview setup/questions, confidence/story capture, review, code export/import and prototype parity. |
| Class | **PARTIAL** | Code-entry/sample-data scaffold exists. | Real code parsing, pooled aggregation, grouping/summary behaviour and prototype parity. |
| Council | **PARTIAL** | Role, scenario and budget scaffolds exist. | Full role/scenario consequences, selectable interventions, budget mechanics, deliberation result/export and prototype parity. |
| Predict | **PARTIAL** | One-question visual scaffold, confidence controls and range drawing exist. | Functional slider, all 8 questions, reveal/scoring/calibration summary and prototype parity. |
| 24 animated clips | **PLACEHOLDER** | 0/24 faithful native clip players; one generic Sun animation-preview scaffold exists. | Port all 24 step sequences, captions, progress/playback controls and bilingual labels. |
| 3 interactive demos | **PLACEHOLDER** | 0/3 faithful demos. | Port Water, Sea/Land Breeze and Greenhouse interactive demos. |
| 2 games | **PARTIAL** | Weather-or-Climate visual scaffold exists; Myth-or-Fact is absent. | Make Weather-or-Climate fully interactive across all questions/results and implement Myth-or-Fact. |
| Tamil-English word list | **PARTIAL** | 4/77 rows are represented in the native scaffold. | Port all 77 rows, search/filter behaviour, review-warning state and prototype layout. |
| About | **PARTIAL** | Native About route contains purpose, storage, version/credit, sources, limits and data controls. | Replace the contact placeholder where required, complete prototype visual parity and automated/device QA. |
| Backup / Restore | **PARTIAL** | Backup codec, copy-to-clipboard, restore validation, confirmation, repository replacement and reset flows exist; codec unit tests are present. | End-to-end UI/device QA, compatibility/round-trip evidence across realistic saved state, and final prototype/accessibility verification. |

## Fidelity and localisation gates

- Reference captures use a fresh browser context per state and assert `scrollY == 0` plus a visible top element.
- Playwright and Compose use pinned subsetted OFL Noto families: serif display and sans body, including Tamil-specific families.
- CSS typography tokens are generated into Compose; generated files are committed deliberately and CI checks for drift rather than committing them.
- Legacy full-resolution pixel-difference matrices are informational only.
- HARD is the zero-failure structural gate: exact colour tokens, missing-glyph coverage, text overflow/truncation evidence, heading/label/button line counts, body line-count tolerance, tab fit and >=48 dp interactive semantics bounds.
- PARITY is the tagged-geometry/SSIM regression gate. Geometry uses matching runtime-only HTML tags and Compose testTags with a 4 dp absolute tolerance.
- A parity baseline or threshold may change only in an explicit reviewed commit whose commit message records the reason. CI must never change a baseline automatically.

## Remaining milestones, in order

1. Keep `compileDebugKotlin` green as the fast prerequisite gate and fix any compiler defect before visual work continues.
2. Make every HARD check run from valid artefacts and fix real HARD defects without weakening criteria; preserve 44/46 px visible controls while expanding semantics/touch bounds to >=48 dp.
3. Correct shared chrome first: top bar height/placement, then bottom tab-bar offset and tab-label fit.
4. Correct shared cards and chips.
5. Re-run both 320 and 390 dp matrices and correct screen-specific geometry/line-count defects.
6. Resolve PARITY regressions against the reviewed baseline; do not update the baseline to hide a regression.
7. Complete the 24 clips, 3 demos, second game, full first game, all 77 word-list rows and incomplete Elders/Class/Council/Predict workflows.
8. Complete About and Backup/Restore end-to-end/device/accessibility QA.
9. Run final device QA only after automated compile, HARD and PARITY gates are green.
10. Consider release-signing/tagging only after every inventory item above is DONE.
