# Verification report

Date: 4 October 2026

Status meanings: **PASS** = the stated command/check was executed and passed; **FAIL** = it was executed and failed; **BLOCKED** = the required environment or external prerequisite was unavailable. No BLOCKED item is treated as PASS.

## Recovery checkpoint

The local recovery directory no longer contains Git metadata, so local `git branch --show-current`, `git status --short` and `git log -1 --oneline` are **BLOCKED** there with `fatal: not a git repository (or any of the parent directories): .git`. The authoritative repository state is the connected GitHub branch `android-hardening`.

The previously interrupted “Fixing Static Scanner and Locale Strings” work was recovered rather than repeated. The persisted `scripts/static_qa.py` strips comments and quoted strings before searching for an executable `addJavascriptInterface(...)` call, and English/Tamil resources both contain the same `privacy_url` key.

## Corrected static tests rerun individually

| Check | Result | Exact command | Relevant output |
|---|---|---|---|
| Comment-safe forbidden API scanner | **PASS** | `python3 /mnt/data/kk_verify_static_recovery.py /mnt/data/kk_recovered_qa --check forbidden-api` | `PASS: no executable addJavascriptInterface(...) call found; comments ignored` |
| EN/TA resource-key parity, including `privacy_url` | **PASS** | `python3 /mnt/data/kk_verify_static_recovery.py /mnt/data/kk_recovered_qa --check locale-keys` | `PASS: EN/TA string keys match (21 keys); privacy_url present in both` |

## Repository static QA — GitHub Actions Run #9

Command:

`python3 scripts/static_qa.py`

Result: **PASS**

Relevant output:

```text
PASS: original HTML fingerprint
PASS: HTML equals original plus only the permitted hook block
PASS: CSP and localStorage key unchanged
PASS: no file:// or INTERNET marker in app/src
PASS: no executable addJavascriptInterface(...) call
PASS: AndroidTTS, AndroidTheme and AndroidLocale bridges are origin-restricted
PASS: 16 Android XML files are well-formed
PASS: source manifest contains only removal markers, no requested permission
PASS: EN/TA string keys match exactly (21 keys)
PASS: native toolbar flag and optional signing inputs are configured
STATIC_QA_PASS
```

The app HTML still removes back to the original 361,881-byte source with SHA-256 `ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9`. The packaged debug APK contains `assets/www/index.html` with SHA-256 `006270e923d5456ee2b4c2175601ac76ec6f04f3b7d1b29eac4efa13d2e40f08`.

## Browser QA

Command:

`python3 /mnt/data/kk_browser_qa.py`

Result for the non-origin-dependent browser checks: **PASS**

Relevant output:

```text
PASS: {"tts_absent": {...}, "tts_zero_voices": {...},
"dom": {"theme": ["dark", "light"], "words": 77, "clips": 24},
"overflow": [[320, "en"], [320, "ta"], [360, "en"], [360, "ta"]]}
```

Demonstrated browser checks:

- **PASS** — native speech API absent: Learn read-aloud and narrated clip send `speak`, receive `start`/`end`, and the clip advances without a page error.
- **PASS** — native `speechSynthesis` present but reporting zero voices: the bridge fallback behaves the same way without the former `SpeechSynthesisVoice`/utterance mismatch.
- **PASS** — Back unwinds topic → Learn list → Home → exit, and stops active narration.
- **PASS** — `data-theme` flips dark → light.
- **PASS** — all 24 lesson clips expose their player and enter playing state.
- **PASS** — word list contains 77 entries.
- **PASS** — no horizontal overflow at 320 px or 360 px in English or Tamil at the app's largest text size.
- **BLOCKED** — real origin-backed `localStorage` persistence in this Chromium environment. Navigation to both `file://` and `https://appassets.androidplatform.net/...` is rejected with `ERR_BLOCKED_BY_ADMINISTRATOR`. The test harness did not weaken the CSP to work around this.

## Android build QA — GitHub Actions Run #9

### Debug lint

Command:

`./gradlew --no-daemon lintDebug`

Result: **PASS**

Relevant output:

```text
BUILD SUCCESSFUL in 1m 44s
29 actionable tasks: 29 executed
```

### Debug APK

Command:

`./gradlew --no-daemon assembleDebug`

Result: **PASS**

The CI job then executed:

```sh
AAPT2="$(find "$ANDROID_HOME/build-tools" -type f -name aapt2 | sort -V | tail -n1)"
APK="app/build/outputs/apk/debug/app-debug.apk"
"$AAPT2" dump permissions "$APK"
```

Result: **PASS**

Relevant output:

```text
package: edu.gascnagercoil.kaalakolam.debug
PASS: packaged APK requests no permissions
```

Run #9 debug APK:

- size: 7,283,796 bytes
- SHA-256: `e674127c0752a9256cf8de013b806aa6d256d3e1e7885df06d54d29226fd5aae`

### Release lint and unsigned QA bundle

Command:

`./gradlew --no-daemon assembleDebug lintRelease bundleRelease`

Result: **PASS**

Relevant output:

```text
> Task :app:lintRelease
> Task :app:minifyReleaseWithR8
> Task :app:packageReleaseBundle
> Task :app:bundleRelease
BUILD SUCCESSFUL in 1m 10s
83 actionable tasks: 46 executed, 37 up-to-date
```

Run #9 release QA AAB:

- size: 1,774,034 bytes
- SHA-256: `7246b52b99130634e594f0d8a085c42a8ad99dda0d20d3f5110facbcb9181a54`
- signing status: **unsigned QA artefact**; no production keystore/secrets were supplied and no signing-certificate entries were found in the bundle.

Release compilation emits deprecation warnings for Android system-bar colour/contrast APIs used for the requested API 24–29+ compatibility behaviour. These are warnings, not build or lint failures.

## Device/emulator QA

The following remain **BLOCKED** because this workspace has no Android SDK/emulator/physical device. They must not be marked PASS without an executed device test:

| Required check | Status |
|---|---|
| Cold start; Home visible; no runtime console error | **BLOCKED** |
| Six tabs on Android: Home, Learn, Elders, Class, Council, Predict | **BLOCKED** |
| Tamil rendering without tofu | **BLOCKED** |
| Clip Play/Pause/Next/Replay/½× and Back stop on Android | **BLOCKED** |
| English/Tamil narration with installed device voices | **BLOCKED** |
| Missing-Tamil-voice dialog appears once per session | **BLOCKED** |
| Elders data survives process kill | **BLOCKED** |
| Elders data survives `adb install -r` update | **BLOCKED** |
| Rotation, font scale and system dark-mode behaviour | **BLOCKED** |
| Light-theme status/navigation icon visibility on API 24 and 29+ | **BLOCKED** |
| Three-button navigation-bar behaviour | **BLOCKED** |
| Android 16 gesture Back and three-button Back | **BLOCKED** |
| Native overflow/About/dialog language follows in-app EN/TA toggle | **BLOCKED** |
| Airplane-mode runtime use | **BLOCKED** |
| 600 dp+ tablet/foldable layout | **BLOCKED** |
| Play pre-launch report | **BLOCKED** |

## Publication/release prerequisites

- **BLOCKED / owner action:** after merge, enable **Settings → Pages → Deploy from branch `main`, folder `/docs`**, then independently confirm the privacy-policy URL returns HTTP 200. It is not claimed live before that check.
- **BLOCKED / owner input:** replace `[ADD DATE BEFORE PUBLISHING]` and `[ADD EMAIL BEFORE PUBLISHING]` in the privacy policy.
- **BLOCKED / owner input:** create and securely back up the production upload keystore, then add the release secrets for a signed production AAB.
- **BLOCKED / human review:** Tamil draft terminology/store text, emergency numbers and scientific figures in the HTML.
- **Optional owner decision:** add a repository licence if wanted.

PR `android-hardening → main` must remain unmerged until the owner reviews this verification record and the device-QA results.
