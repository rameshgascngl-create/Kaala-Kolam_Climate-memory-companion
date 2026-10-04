# Step 0 — Browser/API audit

**Scope:** `app/src/main/assets/www/index.html` from the supplied prototype. The CSP is:
`default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'`.
It does **not** need changing for `https://appassets.androidplatform.net`: the main document is supplied by `WebViewAssetLoader`; the CSP then permits the page's existing inline JavaScript/CSS and data images while blocking network subresources. The only `http://` literal in the HTML is the SVG namespace (`http://www.w3.org/2000/svg`), not a fetch. No CSP directive is loosened.

| Browser API / feature found | WebView difference | Native handling |
|---|---|---|
| `localStorage` (`kaala_kolam_v1`) | Requires DOM storage and a stable origin. Data is origin-scoped and normally survives app updates. | Enable `domStorageEnabled`; keep the fixed appassets HTTPS origin. No bridge. |
| `speechSynthesis` / `SpeechSynthesisUtterance` | Voice availability varies by WebView/TTS engine; Tamil may be absent. | Keep web speech when usable. Inject an origin-restricted Android TTS fallback only when the web API is missing or reports no voices; offer system TTS settings once if no offline Tamil voice exists. |
| `navigator.clipboard.writeText` | Secure-context support and clipboard policy can vary by WebView version. The HTML already falls back to selection + `document.execCommand('copy')`. | No native clipboard bridge required. |
| `requestAnimationFrame` / `cancelAnimationFrame` | Paused/throttled when the WebView is backgrounded. | Call `WebView.onPause()` / `onResume()` and stop speech on pause. |
| `prefers-reduced-motion` | Depends on WebView/platform accessibility support. | CSS already degrades safely; no native handling. |
| Blob/download links | **Not used.** | Skip `DownloadListener`/MediaStore. |
| `window.print` | **Not used.** | Skip native printing/share export. |
| `window.open` | **Not used.** | Multiple windows remain disabled. |
| `alert` / `confirm` / `prompt` | **Not used.** | No `WebChromeClient` dialog bridge needed. |
| Fullscreen API | **Not used.** | No native fullscreen handling. |
| Vibration API | **Not used.** | No vibration permission/bridge. |
| External `http(s)` / `mailto:` links | **No external URL is currently present in the HTML.** Future links could otherwise navigate the WebView. | Navigation policy allows only the appassets HTTPS host; future `http(s)`/`mailto:` links are handed to external apps. Other schemes are ignored. |

**HTML hook need:** the HTML has no native-queryable Home state. Add only `window.__appIsHome()` and `window.__appGoHome()` near boot. No other HTML edit is justified.
