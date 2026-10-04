# Verification report

Date: 4 October 2026

Status meanings: **PASS** = the stated command/check was actually run and passed; **FAIL** = it was run and failed; **BLOCKED** = the required environment or credential is unavailable.

| Check | Status | Command / evidence |
|---|---|---|
| Input SHA-256 fingerprints | PASS | `sha256sum` matched all three supplied fingerprints. |
| HTML original + permitted hook block only | PASS | `python3 scripts/verify_html_delta.py` restored original SHA-256 `ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9`. |
| CSP unchanged | PASS | `scripts/verify_html_delta.py` passed in GitHub Actions Run #2. |
| No `file://` / INTERNET capability in `app/src` | PASS | GitHub Actions Run #2, step “Assert no network or file URL capability”. |
| XML well-formed | PASS | Parsed all XML under `app/src/main` during static reconciliation; Android resource processing also completed in the successful build. |
| EN/TA string keys match | PASS | Static XML key comparison. |
| Manifest activity classes exist | PASS | `.MainActivity` and `.AboutActivity` compiled successfully. |
| Resource references resolve | PASS | `lintDebug` and `assembleDebug` completed successfully in GitHub Actions Run #2. |
| Gradle wrapper validation | PASS | GitHub Actions Run #2, `gradle/actions/wrapper-validation@v4`. |
| `./gradlew lintDebug` | PASS | GitHub Actions Run #2. |
| `./gradlew assembleDebug` | PASS | GitHub Actions Run #2; `BUILD SUCCESSFUL`. |
| Final APK declares zero permissions | PASS | Run #1 exposed AndroidX Core’s injected `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; commit `edeedb08d5a3ec0433f4f33284e8d3abfa47648e` removes both merged compatibility nodes. Run #2 `aapt2 dump permissions` assertion then passed. |
| Debug APK artifact | PASS | GitHub Actions Run #2 artifact `KaalaKolam-debug-apk`; unpacked APK size 6,333,891 bytes; SHA-256 `b5b54b41b35fe6a1445d02727097b08238a5250309df6c115fbad81e5fbbbc18`. |
| Lint HTML report artifact | PASS | GitHub Actions Run #2 artifact `KaalaKolam-lint-report`. |
| `./gradlew lintRelease` | BLOCKED | Not yet run in CI; the current debug CI gate uses `lintDebug`. |
| Signed `./gradlew bundleRelease` | BLOCKED | Production upload keystore/secrets are intentionally absent. |
| Cold start / console errors | BLOCKED | Requires emulator or physical Android device. |
| Six tabs | BLOCKED | Requires device/emulator. |
| Tamil rendering / word list | BLOCKED | Requires device/emulator and font rendering. |
| Clip Play/Pause/Next/Replay/half-speed/Back stop | BLOCKED | Requires device/emulator. |
| English and Tamil narration / missing-voice dialog | BLOCKED | Requires device/emulator and installed TTS engines. |
| localStorage persistence after kill and `adb install -r` | BLOCKED | Requires device/emulator and update-over-install testing. |
| KK1./KP1. copy/paste | BLOCKED | Requires device/emulator. |
| Rotation/theme/font/system-language/320 dp overflow | BLOCKED | Requires device/emulator. |
| Android 16 predictive Back and three-button Back | BLOCKED | Requires Android 16 device/emulator. |
| Airplane mode | BLOCKED | Requires device/emulator. |
| 600 dp+ tablet/foldable | BLOCKED | Requires suitable device/emulator. |
| Play pre-launch report | BLOCKED | Requires Play Console upload. |

## CI history

### Run #1 — failure isolated after successful build
- Wrapper validation: PASS.
- HTML delta: PASS.
- Offline/network assertion: PASS.
- `lintDebug`: PASS.
- `assembleDebug`: PASS.
- APK permission gate: FAIL because AndroidX Core merged the signature-only `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

The application itself does not call `ContextCompat.registerReceiver(... RECEIVER_NOT_EXPORTED)`. The compatibility permission and matching `uses-permission` node are therefore explicitly removed in the application manifest so the packaged APK meets the project’s zero-permission requirement.

### Run #2 — green build gate
- Wrapper validation: PASS.
- HTML delta: PASS.
- Offline/network assertion: PASS.
- `lintDebug`: PASS.
- `assembleDebug`: PASS.
- `aapt2 dump permissions`: PASS with no packaged permissions.
- Debug APK artifact upload: PASS.
- Lint report upload: PASS.

## Device QA sequence

Install the Run #2 debug APK and execute the eleven product checks from the project hand-off specification on an Android 16 emulator/device, including update-over-install persistence and 600 dp+ coverage. Do not convert any BLOCKED row to PASS without actually running the corresponding test.
