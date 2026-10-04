# Verification report

Date: 3 October 2026

Status meanings: **PASS** = command was run and passed; **FAIL** = command was run and failed; **BLOCKED** = this environment cannot execute the required build/device check.

| Check | Status | Command / evidence |
|---|---|---|
| Input SHA-256 fingerprints | PASS | `sha256sum` matched all three supplied fingerprints. |
| HTML original + permitted hook block only | PASS | `python3 scripts/verify_html_delta.py` restored SHA-256 `ae7b87a4...66508a9`. |
| CSP unchanged | PASS | Verified by `scripts/verify_html_delta.py`. |
| No permissions declared in manifest | PASS | XML source inspection: no `<uses-permission>` element. |
| No `file://` / `INTERNET` marker in `app/src` | PASS | `grep -R -n -E 'file://|INTERNET' app/src` returned no matches. |
| XML well-formed | PASS | Parsed all XML under `app/src/main` with Python `xml.etree.ElementTree`. |
| EN/TA string keys match | PASS | Static XML key comparison. |
| Manifest activity classes exist | PASS | `.MainActivity` and `.AboutActivity` source files present. |
| Resource references resolve statically | PASS | Static resource-name scan completed without unresolved local references. |
| Gradle wrapper distribution URL | PASS | `gradle-9.6.0-bin.zip`, matching AGP 9.4 documented default/minimum. Wrapper JAR provenance could not be independently downloaded in this environment. |
| `./gradlew assembleDebug` | BLOCKED | No Android SDK and external Gradle/Maven hosts are unreachable from the execution shell. |
| `./gradlew lintDebug` / `lintRelease` | BLOCKED | Same build-environment limitation. |
| `./gradlew bundleRelease` | BLOCKED | Same build-environment limitation; production signing secrets are intentionally absent. |
| `aapt2 dump permissions` on built APK | BLOCKED | No APK can be built locally here. CI performs this assertion after build. |
| Cold start / console errors | BLOCKED | No emulator or physical Android device available. |
| Six tabs | BLOCKED | Requires device/emulator. |
| Tamil rendering / word list | BLOCKED | Requires device/emulator and font rendering. |
| Clip Play/Pause/Next/Replay/half-speed/Back stop | BLOCKED | Requires device/emulator. |
| English and Tamil narration / missing-voice dialog | BLOCKED | Requires device/emulator and installed TTS engines. |
| localStorage persistence after kill and `adb install -r` | BLOCKED | Requires device/emulator and two installable APKs. |
| KK1./KP1. copy/paste | BLOCKED | Requires device/emulator. |
| Rotation/theme/font/system-language/320 dp overflow | BLOCKED | Requires device/emulator. |
| Android 16 predictive Back and three-button Back | BLOCKED | Requires Android 16 device/emulator. |
| Airplane mode | BLOCKED | Requires device/emulator. |
| 600 dp+ tablet/foldable | BLOCKED | Requires suitable device/emulator. |
| Play pre-launch report | BLOCKED | Requires Play Console upload. |

## Device QA sequence

When CI produces the debug APK, execute the eleven product checks from the project hand-off specification on an Android 16 emulator/device, including update-over-install persistence and 600 dp+ coverage. Do not convert any BLOCKED row to PASS without actually running the corresponding test.
