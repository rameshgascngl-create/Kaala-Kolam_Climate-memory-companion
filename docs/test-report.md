# Native v2 verification report

Date: 4 October 2026  
Branch: `native-v2`

Status meanings:

- **PASS** — the stated command or check was executed and passed.
- **FAIL** — the stated command or check was executed and failed.
- **BLOCKED** — the required environment, device, network path or credential was unavailable. BLOCKED is never treated as PASS.

## M0 — Recon and content extraction

| Check | Status | Evidence |
|---|---|---|
| Audited v1.0 input size | **PASS** | `stat -c '%n %s bytes' /mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html` → 361881 bytes |
| Audited v1.0 SHA-256 | **PASS** | `sha256sum ...` → `ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9` |
| Corrected v1.1 source | **BLOCKED / NOT PRESENT AS SPECIFIED** | `correction-v1-1` exists at the inspected checkpoint but still contains the audited v1.0 source plus wrapper hardening; no corrected v1.1 content source was found, so the specification requires use of A |
| Reference APK fingerprint | **PASS** | 7,283,796 bytes; SHA-256 `f5264502e7adf62d5f1f1a6b952647f84105a91c288bde54a045fdc17287c7b8`; APK not executed |
| Local Git clone | **BLOCKED** | `git clone ...` → `Could not resolve host: github.com` |
| Remote branch creation | **PASS** | `native-v2` created from main SHA `e33da90a2e93db139b61d0377ed23bd34e653efd`; no main write, merge or force-push |
| Playwright extraction | **PASS** | `python3 tools/extract_content.py ...` → `M0_EXTRACT_PASS` |
| Content-integrity tests | **PASS** | `python3 tools/test_content_asset.py` → 5 tests, `OK` |
| Topics | **PASS** | 24 |
| Clips | **PASS** | 24; four JSON batches of 6 |
| Word-list entries | **PASS** | 77 |
| Elder questions | **PASS** | 10 |
| Council options | **PASS** | 7 |
| Council groups | **PASS** | 4 |
| Council scenarios | **PASS** | 3 |
| Council roles | **PASS** | 6 |
| Predictions | **PASS** | 8 |
| Interactive demos | **PASS** | 3 |
| Games | **PASS** | 2 |
| Non-empty EN + TA on exposed bilingual objects | **PASS** | recursive content test |
| Approved Tamil terminology / banned-term gate | **PASS** | generated assets contain none of the ten banned forms |
| UTF-8, U+FFFD and mojibake gate | **PASS** | content-integrity test |
| Draft word flags retained | **PASS** | 8 source entries retain draft flag for later `⚠` UI rendering |
| GitHub-hosted extraction checkpoint | **PASS** | Actions run `37192429989`; all extraction/verification/commit steps concluded success |
| Remote generated-assets commit | **PASS** | `b1cb63dc127223633326e02a13bab96750569b27` |
| Remote manifest re-read | **PASS** | source SHA, source byte count and all counts match M0 evidence |
| Temporary M0 workflow removed | **PASS** | remote fetch of `.github/workflows/m0-content-extraction.yml` at current branch returns 404 |

### M0 content source decision

Input A is the authoritative content and logic source because a genuine corrected v1.1 content source was not found on `correction-v1-1` at M0. This decision must be rechecked before domain golden-fixture generation in M2.

### M0 generated assets

`app/src/main/assets/content/` contains:

- `manifest.json`
- `shared.json`
- `elders.json`
- `predictions.json`
- `council.json`
- `games.json`
- `words.json`
- `topics-1.json`
- `topics-2.json`
- `clips-1.json`
- `clips-2.json`
- `clips-3.json`
- `clips-4.json`

**M0 STATUS: PASS**, with local Git clone retained as an environmental **BLOCKED** item.

M1 has not started.

## M0.5 — content gaps and Tamil terminology policy

| Check | Status | Evidence |
|---|---|---|
| Generated assets changed only through extractor override policy | **PASS** | `tools/content_overrides.json` is read by `tools/extract_content.py`; regenerated output used for verification |
| School-teacher `காப்பகம்` correction | **PASS** | generated `council.json` contains `பள்ளியே ஒரே பாதுகாப்பு மையம்...` and no `காப்பகம்` |
| Standalone generic `அழுத்தம்` decision | **PASS** | explicit Tamil-range word-boundary allow rule limited to `words[16][2]` and exact gloss `காற்று பரப்பின் மேல் செலுத்தும் அழுத்தம்.` |
| Banned-term grep | **PASS** | no hits for the other nine banned forms; boundary-aware grep has exactly one standalone `அழுத்தம்` hit in `words.json` |
| English-only long-form inventory | **PASS** | generated `gaps.json`: 24 deep-dive + 7 Council + 30 Elders + 16 Predict = 77 |
| Gap enforcement | **PASS** | test derives actual long-form untranslated fields and requires exact equality with `gaps.json` |
| Native gap label contract | **PASS (metadata only)** | `gaps.json` records exact `uiLabel` = `English only`; M1 UI has not started |
| Tamil draft separation | **PASS** | `docs/tamil-drafts-DRAFT.json`: `status=DRAFT`, `shippedWithApp=false`, 77 entries, all `taDraft=null` |
| Machine-translated Tamil in shipped assets | **PASS** | none generated; owner-review queue remains separate |
| Content test suite | **PASS** | `python tools/test_content_asset.py` → 8 tests, `OK` |
| Remote content commit | **PASS** | `15e1355c72c70f69228950534ded5cb27922d949` |
| Remote byte-for-byte re-read | **PASS** | all six M0.5 content/policy files matched the locally tested versions exactly |
| Commit authorship | **PASS** | author and committer: `Ramesh R <rameshgascngl@gmail.com>` |
| History rewrite | **PASS — none used** | pre-M0.5 HEAD is direct parent; compare reports ahead 1, behind 0; ref update used `force=false` |

See `docs/m0.5-report.md` for commands and output excerpts.

**M0.5 STATUS: PASS. M1 has not started.**
