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
| Elders | **PARTIAL** | Slices A-B plus storage evidence are committed. Slice C adds the generated-content-driven 10-question elderAsk flow: five-point scale plus Cannot say, confidence, optional autosaved story, progress/back/next/skip/finish, persisted question resume, 500-grapheme BreakIterator story limit, IME-safe TextFieldValue handling, API 34 process-death/rotation resume coverage, and EN/TA 320/390 dp 200% visual-state coverage for list/empty/setup/delete/limit. | Slice D: results/evidence/reaction/share UI; then focused Elders audit and final prototype parity/device QA. |
| Class | **PARTIAL** | Code-entry/sample-data scaffold exists. | Real code parsing, pooled aggregation, grouping/summary behaviour and prototype parity. |
| Council | **PARTIAL** | Role, scenario and budget scaffolds exist. | Full role/scenario consequences, selectable interventions, budget mechanics, deliberation result/export and prototype parity. |
| Predict | **PARTIAL** | The full eight-question sequence, stepped slider input, confidence choice, persisted answers, reveal cards, calibration buckets/message and reset flow are implemented against the M2 web-derived prediction specs. | Current-head unit/golden CI, HARD checks, accessibility/device QA and final prototype visual parity must pass before DONE. |
| 24 animated clips | **PLACEHOLDER** | 0/24 faithful native clip players; one generic Sun animation-preview scaffold exists. | Port all 24 step sequences, captions, progress/playback controls and bilingual labels. |
| 3 interactive demos | **PLACEHOLDER** | 0/3 faithful demos. | Port Water, Sea/Land Breeze and Greenhouse interactive demos. |
| 2 games | **PARTIAL** | Weather-or-Climate visual scaffold exists; Myth-or-Fact is absent. | Make Weather-or-Climate fully interactive across all questions/results and implement Myth-or-Fact. |
| Tamil-English word list | **PARTIAL** | 4/77 rows are represented in the native scaffold. | Port all 77 rows, search/filter behaviour, review-warning state and prototype layout. |
| About | **PARTIAL** | Native About route contains purpose, storage, version/credit, sources, limits and data controls. | Replace the contact placeholder where required, complete prototype visual parity and automated/device QA. |
| Backup / Restore | **PARTIAL** | Backup codec, copy-to-clipboard, restore validation, confirmation, repository replacement and reset flows exist; codec unit tests are present. | End-to-end UI/device QA, compatibility/round-trip evidence across realistic saved state, and final prototype/accessibility verification. |
| M2 domain logic + web golden fixtures | **DONE** | Run #136 on commit `4a8243f5693f2827f08a4a4678022e387e726e42` passed native static QA and the dedicated M2 web-golden test after deterministic fixture regeneration. Coverage includes share codes, deterministic sample records, class aggregation, council risk maths and prediction numeric specs. | None for the M2 domain/golden milestone; downstream feature screens must consume these rules without duplicating them. |

## Fidelity and localisation gates

- Reference captures use a fresh browser context per state and assert `scrollY == 0` plus a visible top element.
- Playwright and Compose use pinned subsetted OFL Noto families: serif display and sans body, including Tamil-specific families.
- CSS typography tokens are generated into Compose; generated files are committed deliberately and CI checks for drift rather than committing them.
- Legacy full-resolution pixel-difference matrices are informational only.
- HARD is the zero-failure structural gate: exact colour tokens, missing-glyph coverage, non-intentional text overflow/truncation, heading/label/button line counts, body line-count tolerance, and >=48 dp interactive semantics bounds. Persistent tab labels use an accessibility-first contract: fully visible at 320/390 dp and 100%/200% font scale with >=48 dp touch bounds; tab line-count comparison is informational where the web reference clips.
- Machine-readable screen status lives in `tests/fidelity/build-status.json`. HARD applies to every captured screen. PARITY is enforced only for screens marked **DONE**; **PARTIAL** and **PLACEHOLDER** parity findings remain visible as informational evidence in CI. A screen may move to DONE only after its function, HARD and PARITY checks pass.
- PARITY is the tagged-geometry/SSIM regression gate. Geometry uses matching runtime-only HTML tags and Compose testTags with a 4 dp absolute tolerance.
- A parity baseline or threshold may change only in an explicit reviewed commit whose commit message records the reason. CI must never change a baseline automatically.

## Remaining milestones, in order

Pixel tuning is paused except for HARD defects. Functional parity now has
priority.

1. Keep `compileDebugKotlin` green and make the HARD accessibility/structural gate green without weakening it. Persistent tabs must remain fully visible at 320/390 dp and 100%/200% font scale with >=48 dp touch bounds.
2. **M2 domain logic and golden fixtures** — port deterministic prototype rules into platform-independent Kotlin and verify them against golden web fixtures.
3. **Predict** — complete all eight questions, slider/input behaviour, confidence, reveal/scoring and calibration summary, with golden functional parity.
4. **Elders** — complete interview setup, questions, confidence/story capture, review and code export.
5. **Class** — complete code parsing, pooled aggregation, grouping and summaries.
6. **Council** — complete roles, scenarios, intervention selection, budget/consequences and result/export.
7. **Word list and games** — complete all 77 glossary rows, search/filter/review-warning behaviour, Weather-or-Climate and Myth-or-Fact.
8. **Clip engine + 24 clips** — implement the reusable player/animation engine, then port clips in four reviewed batches of six.
9. Resume non-HARD shared chrome/cards/chips and PARITY pixel tuning only after the functional milestones above are materially complete.
10. Complete About and Backup/Restore end-to-end/device/accessibility QA, then final device QA.
11. Consider release-signing/tagging only after every inventory item above is DONE.
