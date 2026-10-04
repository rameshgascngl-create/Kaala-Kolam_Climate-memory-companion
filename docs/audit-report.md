# Kaala Kolam Android import audit

Date: 3 October 2026

## Inputs

| Input | Expected SHA-256 | Observed SHA-256 | Result |
|---|---|---|---|
| KaalaKolam_Android_Final_Source.zip | `e297078f6c7e7af0309713901fd7694aae7cf6d40e526b880058a75085c40c44` | same | PASS |
| KaalaKolam_Android_Prototype.zip | `cfff3d038cf460c3a1edfb176411cd7558109f5b093b11e68c3796ed21e58e31` | same | PASS |
| Kaala_Kolam_Climate_Memory_Companion.html | `ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9` | same | PASS |

Base selected: **Final Source ZIP (A)** because its checksum matches and it already contains the API 36 / AGP 9.4 structure, WebViewAssetLoader shell, origin-restricted WebMessageListener TTS fallback, native About surface, adaptive icons and store-readiness documents. The prototype was inspected as a secondary source; no feature from it was required after reconciliation.

## Browser API audit

- `localStorage`: used under key `kaala_kolam_v1`; no native bridge needed. The fixed `https://appassets.androidplatform.net` origin preserves storage across normal updates.
- `speechSynthesis`, `SpeechSynthesisUtterance`, `getVoices`, `speak`, `cancel`, `onend/onerror`: Android WebView support can vary. Native Android `TextToSpeech` fallback is therefore provided through an origin-restricted `WebViewCompat.WebMessageListener` and document-start polyfill.
- `navigator.clipboard.writeText`: WebView availability may vary; the page already contains a `document.execCommand('copy')` fallback. No native clipboard bridge is needed.
- `requestAnimationFrame`: WebView supports it; lifecycle pausing is handled by `WebView.onPause()`/`onResume()` and page navigation destroys active clip players.
- `prefers-reduced-motion`: supported by WebView CSS media queries; no native handling required.
- Blob/download links: not used.
- `window.print`: not used.
- `window.open`: not used.
- `alert` / `confirm` / `prompt`: not used.
- fullscreen API: not used.
- vibration API: not used.
- external web URLs in the page: none. Native privacy/help links are deliberately handed to the system browser.

## CSP

The source CSP is unchanged:

`default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'`

It remains compatible with `WebViewAssetLoader` because the HTML, inline CSS/JS and data images require no network fetches. No CSP relaxation is required.

## Toolchain

Google Play's current requirement was rechecked on 3 October 2026: new apps and updates must target Android 16 / API 36 or higher from 31 August 2026. AGP 9.4.0 documents Gradle 9.6.0 minimum/default, Build Tools 36.0.0 and JDK 17. The project therefore uses compileSdk/targetSdk 36, AGP 9.4.0 and Gradle 9.6.0 with built-in Kotlin support.
