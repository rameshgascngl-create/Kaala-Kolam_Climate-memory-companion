# Kaala Kolam (காலக்கோலம்) — Android project

Offline Android shell for the finished single-file `Kaala_Kolam_Climate_Memory_Companion.html` application.

**Package:** `edu.gascnagercoil.kaalakolam`  
**Version:** `1.0.0` (`versionCode 1`)  
**Minimum Android:** API 24  
**compileSdk / targetSdk:** API 36  
**Credit:** Department of Zoology, GASC, Nagercoil / Created by R. Ramesh

## Architecture

- Kotlin, one main Activity plus a small native About screen; no Compose.
- The HTML is at `app/src/main/assets/www/index.html` and is served only through `WebViewAssetLoader` at `https://appassets.androidplatform.net/assets/www/index.html`.
- No `file://`, file access, content access, `INTERNET` permission, analytics, advertising or third-party service SDK.
- The original CSP is unchanged.
- The only HTML delta is one permitted native hook block defining `window.__appIsHome`, `window.__appGoHome` and `window.__appBack`. See `docs/html-changes.diff`.
- WebView debugging is enabled only in debug builds.
- Native Android TextToSpeech is exposed only through the origin-restricted `AndroidTTS` WebMessage bridge; no `addJavascriptInterface` is used.

## Toolchain

The project pins **Android Gradle Plugin 9.4.0** and **Gradle 9.8.0**. AGP 9.x uses built-in Kotlin support, so the obsolete `org.jetbrains.kotlin.android` plugin is not applied. API 36 is used because Google Play requires Android 16 / API 36 or higher for new apps and updates from 31 August 2026.

Use JDK 17 or later and install Android SDK Platform 36 plus the corresponding build tools.

## Build

```bash
./gradlew assembleDebug
```

Expected debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Before a release build, configure signing as below, then run:

```bash
./gradlew bundleRelease
```

Expected bundle:

```text
app/build/outputs/bundle/release/app-release.aab
```

Run lint with:

```bash
./gradlew lintRelease
```

## Release signing

Create the production upload key **outside this repository**:

```bash
mkdir -p "$HOME/keys"
keytool -genkeypair -v \
  -keystore "$HOME/keys/kaalakolam-release.jks" \
  -alias kaalakolam \
  -keyalg RSA -keysize 4096 -validity 10000
```

Supply the following through `~/.gradle/gradle.properties` or equivalently named environment variables:

```properties
KK_STORE_FILE=/absolute/path/to/kaalakolam-release.jks
KK_STORE_PASSWORD=REPLACE_LOCALLY
KK_KEY_ALIAS=kaalakolam
KK_KEY_PASSWORD=REPLACE_LOCALLY
```

Do not place passwords or the production keystore in the repository. The project `.gitignore` excludes `*.jks`, `*.keystore` and `keystore.properties`.

Exact local backup command:

```bash
mkdir -p "$HOME/KaalaKolam-keystore-backup" && \
cp -p "$HOME/keys/kaalakolam-release.jks" "$HOME/KaalaKolam-keystore-backup/kaalakolam-release.jks" && \
sha256sum "$HOME/keys/kaalakolam-release.jks" "$HOME/KaalaKolam-keystore-backup/kaalakolam-release.jks"
```

Also copy the keystore to a separate encrypted/offline medium. Losing the upload key can prevent straightforward future updates unless Play App Signing recovery is available for the account.

## Install and update testing

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To verify permissions after an APK is built:

```bash
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

The output must contain no `uses-permission: name='android.permission.INTERNET'` entry and, for this project, no requested permissions at all.

To verify state persistence across an update, enter an Elders interview, close the app, build a later APK with the same application ID and signing key, then use `adb install -r`. Do not clear app data between installs.

## Updating the HTML later

Replace `app/src/main/assets/www/index.html` with the new single-file HTML, preserving the CSP and localStorage key. Re-add only this block immediately before `boot();` if the source HTML does not already contain it:

```javascript
/* ===== Android shell hooks (used only by the native wrapper) ===== */
window.__appIsHome=function(){return S.view==='home'};
window.__appGoHome=function(){if(S.view!=='home')go('home')};
window.__appBack=function(){
  try{killPlayer();stopSpeechSafe()}catch(e){}
  if(S.view==='learn'&&S.lmode!=='home'){S.lmode='home';G=null;save();render();window.scrollTo(0,0);return true}
  if(S.view!=='home'){go('home');return true}
  return false
};
```

Do not change the `appassets.androidplatform.net` origin. It is intentionally stable so WebView `localStorage` remains associated with the same origin after app updates.

## Store-readiness files

- `docs/audit-report.md`
- `docs/test-report.md`
- `docs/html-changes.diff`
- `docs/data-safety.md`
- `docs/store-listing.md`
- `docs/privacy-policy.html`
- `docs/disclaimer.md`
- `docs/minimum-functionality.md`
- `docs/playstore-icon-512.png`

Before Play submission, merge only after review, enable GitHub Pages from `main` / `docs`, confirm the configured privacy-policy URL returns HTTP 200, fill the policy's publication-date and developer/grievance-contact placeholders, and complete the device tests in `docs/test-report.md`.
