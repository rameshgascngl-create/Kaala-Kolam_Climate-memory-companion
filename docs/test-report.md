# Step 7 — Verification report

**Environment limitation:** this workspace has JDK 21 but no Android SDK, emulator, physical device or cached Gradle/AGP distribution. The shell cannot resolve `services.gradle.org`, so Gradle cannot download Gradle 9.8.0 or Android dependencies. Device, emulator, lint and Play pre-launch tests therefore cannot be truthfully marked PASS. Static checks that do not require the Android toolchain were run.

| # | Required verification | Result | Evidence / limitation |
|---|---|---|---|
| 1 | Cold start shows Home; no console errors | **BLOCKED** | Requires built APK + emulator/device. Debug `WebChromeClient` logs console messages as `KaalaKolamJS`. |
| 2 | Six tabs open: Home, Learn, Elders, Class, Council, Predict | **BLOCKED** | Requires device interaction. Static HTML contains all six routes. |
| 3 | Tamil renders without tofu; Learn titles and `தமிழ்–ஆங்கிலச் சொற்பட்டி` display | **BLOCKED** | Requires Android font/render test. HTML UTF-8 parses and JS syntax passes. |
| 4 | Learn animation: Play/Pause/Next/Replay/½×; leaving stops it | **BLOCKED** | Requires device interaction and frame timing. `requestAnimationFrame` code is present and JS syntax passes. |
| 5 | English/Tamil narration works; no crash without Tamil voice | **BLOCKED** | Requires device TTS engines. Native fallback is origin-restricted and shows a once-per-session Tamil-voice dialog. |
| 6 | Elders data survives Recents kill and `adb install -r` update | **BLOCKED** | Requires two installed builds. Fixed appassets origin + `domStorageEnabled` are configured. |
| 7 | Copy/paste `KK1.` and `KP1.` Class codes | **BLOCKED** | Requires Android clipboard interaction. HTML has Clipboard API + `execCommand('copy')` fallback. |
| 8 | Rotate/theme/font/system language; no reload/state loss/overflow at 320 dp | **BLOCKED** | Requires emulator/device. Manifest handles orientation, screen size, UI mode, locale, layout direction and font scale. |
| 9 | Back button follows required Home logic | **BLOCKED** | Requires device back dispatch. Static code checks `WebView.canGoBack()`, then `__appIsHome()`, then `__appGoHome()`, else exits. |
| 10 | Airplane mode works; `aapt dump permissions` confirms no INTERNET | **BLOCKED** for runtime/aapt; **PASS** manifest static check | Manifest contains no `<uses-permission>` element. |
| 11 | Android lint + Play pre-launch checks: zero errors | **BLOCKED** | Gradle cannot resolve because `services.gradle.org` DNS is unavailable; Play pre-launch requires Play Console upload. |

## Static checks actually run

| Check | Result |
|---|---|
| Parse all project XML resources/manifest | **PASS** |
| JavaScript syntax: main inline HTML script (`node --check`) | **PASS** |
| JavaScript syntax: `tts_polyfill.js` (`node --check`) | **PASS** |
| CSP unchanged from supplied HTML | **PASS** |
| Only permitted HTML hooks present; old `__appBack` removed | **PASS** |
| No `file://`, `allowFileAccess=true` or `allowContentAccess=true` | **PASS** |
| Play Store icon is exactly 512×512 PNG | **PASS** |
| Gradle wrapper execution | **BLOCKED** — `UnknownHostException: services.gradle.org` |
