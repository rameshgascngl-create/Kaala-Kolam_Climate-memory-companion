# Kaala Kolam (காலக்கோலம்) — native Android v2

Kaala Kolam is being rebuilt on branch `native-v2` as a **purely native** Android application using Kotlin, Jetpack Compose and Material 3.

**Package:** `edu.gascnagercoil.kaalakolam`  
**Version:** `2.0.0` (`versionCode 3`)  
**Minimum Android:** API 24  
**compileSdk / targetSdk:** API 36  
**Credit:** Department of Zoology, GASC, Nagercoil / Created by R. Ramesh

## Runtime architecture

- Single-activity Kotlin application.
- Jetpack Compose + Material 3 UI.
- Navigation Compose.
- MVVM with unidirectional state flow.
- Typed, versioned local state stored with AndroidX DataStore.
- Long-form learning content generated deterministically from the audited HTML source into JSON assets.
- No WebView, HTML, JavaScript, WebViewAssetLoader or JavaScript bridge at runtime.
- No Internet or other Android permissions.
- No analytics, advertising, crash reporter or third-party tracking SDK.
- `android:allowBackup="false"` and cleartext traffic disabled.

The audited source HTML is treated as **untrusted extraction input only**. It is not packaged under `app/src/main` and is never executed as application runtime code.

## Source content

The selected M0/M0.5 source is:

`Kaala_Kolam_Climate_Memory_Companion.html`

- bytes: `361881`
- SHA-256: `ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9`

The generator is `tools/extract_content.py`. Human-approved source corrections are declared in `tools/content_overrides.json`; generated JSON must not be hand-edited.

`app/src/main/assets/content/gaps.json` records the 77 long-form English-only gaps that still await approved Tamil. The native UI renders the exact label **English only** for every one of those gap entries. Draft Tamil work remains outside shipped assets in `docs/tamil-drafts-DRAFT.json`.

## Current milestone

M1 provides the project skeleton, theme, adaptive navigation, English/Tamil app-locale control, About screen, versioned DataStore state, Reset, KB1 backup/restore foundation, and the 77-field English-only rendering contract.

Later feature parity belongs to its scheduled milestones; M1 placeholder screens are not evidence that M2–M6 functionality is complete.

## Build

Use JDK 17 and Android SDK Platform 36:

```bash
./gradlew --no-daemon lintDebug testDebugUnitTest assembleDebug
```

Expected debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release minification and resource shrinking are enabled. Production signing material must never be committed.

## Verification

Content-policy tests:

```bash
python3 tools/test_content_asset.py
```

Native static policy gates:

```bash
python3 tools/native_static_qa.py
```

After a debug build, the merged manifest must remain permission-free:

```bash
python3 tools/assert_merged_manifest.py <merged-debug-manifest>
```

The GitHub Actions workflow performs these gates plus debug lint, JVM unit tests and APK assembly. Executed evidence is recorded in `docs/test-report.md`.

## Privacy

The bilingual privacy-policy source is `docs/privacy-policy.html`. Its date and contact are placeholders until the owner supplies them. Do not claim a public URL works until the deployed page is fetched successfully.

## Tamil review

Tamil is draft pending human review. Approved terminology and explicit, context-specific exceptions are enforced by stem-aware tests. Two heat-shelter phrases remain an owner wording decision documented in `docs/owner-decisions.md`.

## Branch policy

Work for the native rebuild is committed only to `native-v2`. Do not force-push or merge to `main`; the final hand-off is a pull request for owner review.
