# Verification report

Date: 4 October 2026

Status meanings:

- **PASS** — the stated command/check was executed and passed.
- **FAIL** — the stated command/check was executed and failed.
- **BLOCKED** — the required environment, origin, device or credential was unavailable. BLOCKED is not treated as PASS.

## Static and source-integrity QA

| Check | Status | Exact command / evidence |
|---|---|---|
| Original HTML fingerprint, permitted hook block only, CSP, storage key, no `file://`/INTERNET, bridge origin restrictions, XML parse, permission policy, EN/TA key parity, toolbar flag and optional signing inputs | **PASS** | `python3 scripts/static_qa.py` in GitHub Actions Run #9. Output ended `STATIC_QA_PASS`. |
| Comment-safe forbidden API scan | **PASS** | Recovery test used Python comment/string stripping followed by `re.compile(r'\\baddJavascriptInterface\\s*\\(')` over `app/src/main/java`. Output: `PASS: no executable addJavascriptInterface(...) call found in app/src/main/java; comment/string mentions ignored`. |
| EN/TA resource-key parity including `privacy_url` | **PASS** | Recovery test parsed both `strings.xml` files with `xml.etree.ElementTree`. Output: `EN keys: 21`, `TA keys: 21`, `Missing in TA: []`, `Missing in EN: []`, and matching privacy URLs. |
| Gradle wrapper validation | **PASS** | GitHub Actions Run #9, `gradle/actions/wrapper-validation@v4`. |
| Gradle distribution checksum | **PASS** | `gradle-wrapper.properties` pins Gradle 9.8.0 binary SHA-256 `bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`; wrapper-validation passed. |

## Browser QA

The exact branch HTML bytes were tested in Chromium with the Android TTS bridge simulated and `tts_polyfill.js` injected before the app script.

Command executed:

```bash
python3 /mnt/data/kk_browser/run_browser_qa.py
```

| Browser check | Status | Relevant output |
|---|---|---|
| Native `speechSynthesis` absent | **PASS** | `PASS TTS absent: read-aloud speak/start/end; narrated clip advanced to step 2; page errors=0` |
| Native `speechSynthesis` present with zero voices | **PASS** | `PASS TTS zero: read-aloud speak/start/end; narrated clip advanced to step 2; page errors=0` |
| Back: topic → Learn list → Home → exit; playing/narrating clip stops | **PASS** | `PASS UI: ... Back topic->Learn->Home->exit and stopped speech ...` |
| Theme dataset dark → light | **PASS** | Included in `PASS UI` result. |
| All lesson clips | **PASS** | `24/24 clips entered playing state`. |
| Word list | **PASS** | `word list=77`. |
| 320 px EN, maximum app text size | **PASS** | No horizontal overflow; document and viewport widths both 320. |
| 320 px TA, maximum app text size | **PASS** | No horizontal overflow; document and viewport widths both 320. |
| 360 px EN, maximum app text size | **PASS** | No horizontal overflow; document and viewport widths both 360. |
| 360 px TA, maximum app text size | **PASS** | No horizontal overflow; document and viewport widths both 360. |
| Stable-origin browser `localStorage` persistence | **BLOCKED** | Chromium returned `net::ERR_BLOCKED_BY_ADMINISTRATOR` for both `file://` and intercepted `https://appassets.androidplatform.net/` navigation in this workspace. The test was not marked PASS. |

## Android build QA

GitHub Actions Run #9 executed against commit `3ed2f7fd6b8ee235eb8b13a2699625227ee60180`.

| Build check | Status | Exact command / relevant output |
|---|---|---|
| Debug lint | **PASS** | `./gradlew --no-daemon lintDebug`; `BUILD SUCCESSFUL`. |
| Debug APK | **PASS** | `./gradlew --no-daemon assembleDebug`; `BUILD SUCCESSFUL in 43s`. |
| Packaged APK permissions | **PASS** | `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk`; output showed `package: edu.gascnagercoil.kaalakolam.debug` followed by `PASS: packaged APK requests no permissions`. |
| Required release-variant gate | **PASS** | `./gradlew --no-daemon assembleDebug lintRelease bundleRelease`; `lintRelease`, `minifyReleaseWithR8`, `packageReleaseBundle` and `bundleRelease` ran; `BUILD SUCCESSFUL in 1m 10s`. |
| Debug APK artifact | **PASS** | Run #9 artifact `KaalaKolam-debug-apk` uploaded successfully. |
| Debug lint report | **PASS** | Run #9 artifact `KaalaKolam-lint-report` uploaded successfully. |
| Release QA evidence | **PASS** | Run #9 artifact `KaalaKolam-release-qa` contains the release lint report and QA AAB. This is not a production-signed release artefact. |
| APK permission evidence | **PASS** | Run #9 artifact `KaalaKolam-apk-permissions` uploaded successfully. |

### Resolved build failure

An earlier CI run failed at `:app:checkDebugAarMetadata` because `androidx.core:core(-ktx):1.19.0` requires compileSdk 37 while this project is fixed at compileSdk 36. Core was changed to 1.17.0, the latest stable Core line before the 1.18.0 compileSdk increase to API 36.1. Subsequent Runs #8 and #9 passed `lintDebug` and `assembleDebug`.

## Device QA

The following checks require an Android emulator or physical device and remain **BLOCKED** in this workspace:

| Required device check | Status |
|---|---|
| Cold start on Home; no console errors | **BLOCKED** |
| Six tabs: Home, Learn, Elders, Class, Council, Predict | **BLOCKED** |
| Tamil rendering and word-list typography | **BLOCKED** |
| Clip Play/Pause/Next/Replay/½× and stop-on-Back behaviour on-device | **BLOCKED** |
| English and Tamil narration with installed Android TTS voices | **BLOCKED** |
| Missing-Tamil-voice dialog appears once per session | **BLOCKED** |
| Elders data survives process kill | **BLOCKED** |
| Elders data survives `adb install -r` update | **BLOCKED** |
| `KK1.` / `KP1.` clipboard flow | **BLOCKED** |
| Rotation, font scale and system dark-mode behaviour | **BLOCKED** |
| 320 dp device width | **BLOCKED** |
| Light-theme status/navigation icons on API 24 and API 29+ | **BLOCKED** |
| Light theme with three-button navigation | **BLOCKED** |
| Android 16 Back with gesture and three-button navigation | **BLOCKED** |
| Native overflow/About/dialog language follows in-app EN/TA toggle | **BLOCKED** |
| Airplane-mode runtime test | **BLOCKED** |
| 600 dp tablet or foldable | **BLOCKED** |
| Play pre-launch report | **BLOCKED** |

## Publication and signing checks

| Check | Status | Evidence / limitation |
|---|---|---|
| Production upload-keystore signing | **BLOCKED** | No production keystore or release secrets were supplied; none were created or committed. |
| Tag-triggered signed release workflow | **BLOCKED** | No release tag/manual signed run was executed. The workflow is configured to restore a keystore only when all required secrets are present. |
| Privacy-policy URL HTTP 200 | **BLOCKED** | Local fetch command `curl -L -sS --max-time 20 https://rameshgascngl-create.github.io/Kaala-Kolam_Climate-memory-companion/privacy-policy.html` returned `curl: (6) Could not resolve host: rameshgascngl-create.github.io`. Also, `docs/` is not yet on `main` because this PR is intentionally unmerged. Do not claim the URL works until Pages is enabled after merge and an HTTP 200 fetch succeeds. |

## Manual owner steps

1. Review PR #2; do not merge solely on static/CI evidence without completing the required device QA.
2. After an approved merge, enable **Settings → Pages → Deploy from branch `main`, folder `/docs`**, then fetch the configured privacy-policy URL and confirm HTTP 200.
3. Replace `[ADD DATE BEFORE PUBLISHING]` and `[ADD EMAIL BEFORE PUBLISHING]` in `docs/privacy-policy.html`.
4. Create the upload keystore outside the repository, back it up securely, and add release secrets only when a production-signed build is required.
5. Complete the human Tamil review of draft terminology/store text.
6. Verify emergency numbers and scientific figures in the HTML.
7. Add a repository licence only if the owner wants one.
