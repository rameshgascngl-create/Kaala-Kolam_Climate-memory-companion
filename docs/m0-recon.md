# M0 — Recon and content extraction

Date: 4 October 2026

Status meanings: **PASS** = executed and passed; **FAIL** = executed and failed; **BLOCKED** = environment prevented execution. BLOCKED is not PASS.

## Repository

- Target branch: `native-v2`.
- Base branch: `main` at `e33da90a2e93db139b61d0377ed23bd34e653efd` when M0 started.
- `native-v2` was created from that exact SHA through the GitHub repository API.
- No push to `main`, no force-push, no merge.
- Local `git clone` was attempted and is **BLOCKED** because the execution container cannot resolve `github.com`.

Command:

```bash
GIT_TERMINAL_PROMPT=0 timeout 30s git clone --branch main --single-branch \
  https://github.com/rameshgascngl-create/Kaala-Kolam_Climate-memory-companion.git \
  /mnt/data/kaala_native_v2_repo
```

Relevant output:

```text
fatal: unable to access 'https://github.com/rameshgascngl-create/Kaala-Kolam_Climate-memory-companion.git/': Could not resolve host: github.com
CLONE_EXIT=128
```

Remote Git writes for M0 therefore use the connected GitHub API against the real repository.

## Inputs

### A — audited v1.0 HTML

**PASS**

```bash
stat -c '%n %s bytes' /mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html
sha256sum /mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html
```

```text
/mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html 361881 bytes
ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9  /mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html
```

This exactly matches the required fingerprint.

### B — corrected v1.1 HTML from `correction-v1-1`

**NOT PRESENT AS A CORRECTED v1.1 SOURCE at M0.**

The GitHub branch check returned `correction-v1-1` at `b5eb2996095b4c9177d32260cadfc4780cf733c8`, commit `Record executed hardening verification`. Its README still identifies version `1.0.0`, and its runtime HTML is the v1.0-era wrapper asset. The branch also retains the exact audited original HTML as `Kaala_Kolam_Climate_Memory_Companion.html` for source-integrity checks.

Per the specification, input A is the content and logic source for M0. Recheck this decision before M2 if a genuine corrected v1.1 content commit appears.

### C — Run #12 reference APK

**PASS — present and fingerprinted; not executed.**

```bash
stat -c '%n %s bytes' /mnt/data/KaalaKolam-v1.0.0-run12-DEVICE-QA-DEBUG.apk
sha256sum /mnt/data/KaalaKolam-v1.0.0-run12-DEVICE-QA-DEBUG.apk
```

```text
/mnt/data/KaalaKolam-v1.0.0-run12-DEVICE-QA-DEBUG.apk 7283796 bytes
f5264502e7adf62d5f1f1a6b952647f84105a91c288bde54a045fdc17287c7b8  /mnt/data/KaalaKolam-v1.0.0-run12-DEVICE-QA-DEBUG.apk
```

The APK was not executed and was not used as app code.

## Content extraction

`tools/extract_content.py` loads the audited HTML into Chromium through Playwright in an offline browser context, aborts every request, replaces only the terminal `boot()` call in an in-memory copy with a temporary table exposure, and never boots the web UI. It emits JSON assets only.

It also applies the approved Tamil replacements and fails on banned terms, U+FFFD, common mojibake markers, missing bilingual fields or a source SHA mismatch.

Command:

```bash
python3 tools/extract_content.py \
  /mnt/data/native_v2_inputs/Kaala_Kolam_Climate_Memory_Companion.html \
  app/src/main/assets/content \
  --source-label 'v1.0 audited HTML; correction-v1-1 has no corrected v1.1 content commit as of M0' \
  --expected-sha256 ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9
```

Result: **PASS**

```text
SOURCE_SHA256=ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9
SOURCE_BYTES=361881
COUNTS={"clips": 24, "councilGroups": 4, "councilOptions": 7, "councilRoles": 6, "councilScenarios": 3, "demos": 3, "elderQuestions": 10, "games": 2, "predictions": 8, "topics": 24, "words": 77}
M0_EXTRACT_PASS
```

`N predictions = 8`.

Generated assets are deliberately split so no runtime WebView/HTML parsing is required:

```text
manifest.json
shared.json
elders.json
predictions.json
council.json
games.json
words.json
topics-1.json
topics-2.json
clips-1.json
clips-2.json
clips-3.json
clips-4.json
```

The four clip files contain six clips each, matching the requested four implementation batches.

## Content asset tests

```bash
python3 tools/test_content_asset.py
```

Result: **PASS**

```text
test_banned_tamil_terms_absent ... ok
test_counts ... ok
test_every_bilingual_object_has_both_languages ... ok
test_utf8_is_clean ... ok
test_word_draft_flags_preserved ... ok
Ran 5 tests
OK
```

Eight of the 77 word-list entries retain the source draft flag; the native UI must display `⚠` for each flagged term.

## M0 status

| Check | Status |
|---|---|
| A size and SHA-256 | PASS |
| B corrected v1.1 source | NOT PRESENT AS SPECIFIED; A selected |
| C reference APK fingerprint | PASS |
| Playwright offline extraction | PASS |
| 24 topics | PASS |
| 24 clips | PASS |
| 77 words | PASS |
| 10 elder questions | PASS |
| 7 council options | PASS |
| 4 groups | PASS |
| 3 scenarios | PASS |
| 6 roles | PASS |
| 8 predictions | PASS |
| 3 demos | PASS |
| 2 games | PASS |
| Every exposed bilingual object has both EN and TA content | PASS |
| Approved Tamil terminology / banned-term test | PASS |
| UTF-8 / U+FFFD / mojibake checks | PASS |
| Local Git clone | BLOCKED — DNS resolution in container |

## Remote checkpoint

GitHub Actions run **37192429989** executed the committed extractor on `native-v2` and completed successfully. Its steps for audited-source verification, Playwright installation, extraction, content tests, SHA-256 listing, commit and push all concluded **success**.

The generated asset commit is:

```text
b1cb63dc127223633326e02a13bab96750569b27  M0: commit extracted native content assets
```

A remote API re-read of `app/src/main/assets/content/manifest.json` at that branch head returned the exact audited source SHA-256, 361881-byte source size, and counts recorded above. The temporary extraction workflow removed itself in the generated-assets commit; a remote fetch of `.github/workflows/m0-content-extraction.yml` now returns 404.

`native-v2` is **6 commits ahead of main and 0 behind** at this checkpoint. The comparison contains only the M0 extractor/tests/report and the 13 generated JSON assets.

**M0 STATUS: PASS**, with one environmental limitation retained as **BLOCKED**: local `git clone` cannot resolve `github.com`. This does not affect the GitHub-hosted M0 evidence. Per the milestone rule, M1 has not started.
