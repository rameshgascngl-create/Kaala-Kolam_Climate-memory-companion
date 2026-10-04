#!/usr/bin/env python3
"""Static policy gates for the purely native Kaala Kolam Android application."""
from __future__ import annotations

import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
SRC = APP / "src" / "main"
JAVA = SRC / "java"
CONTENT = SRC / "assets" / "content"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def fail(message: str) -> None:
    raise SystemExit("NATIVE_STATIC_QA_FAIL: " + message)


manifest = ET.parse(SRC / "AndroidManifest.xml").getroot()
TOOLS_NS = "{http://schemas.android.com/tools}"
permission_nodes = manifest.findall("uses-permission") + manifest.findall("permission")
requested_permission_nodes = [
    node for node in permission_nodes
    if node.get(TOOLS_NS + "node") != "remove"
]
if requested_permission_nodes:
    fail("source manifest requests a permission")

application = manifest.find("application")
if application is None:
    fail("application node missing")
if application.get(ANDROID_NS + "allowBackup") != "false":
    fail("allowBackup must be false")
if application.get(ANDROID_NS + "usesCleartextTraffic") != "false":
    fail("usesCleartextTraffic must be false")

exported = []
for component_name in ("activity", "service", "receiver", "provider"):
    for component in application.findall(component_name):
        if component.get(ANDROID_NS + "exported") == "true":
            exported.append((component_name, component.get(ANDROID_NS + "name")))
if exported != [("activity", ".MainActivity")]:
    fail(f"unexpected exported components: {exported!r}")

main_activity = application.find("activity[@android:name='.MainActivity']", {"android": "http://schemas.android.com/apk/res/android"})
if main_activity is None:
    fail("launcher MainActivity missing")
actions = {
    node.get(ANDROID_NS + "name")
    for intent in main_activity.findall("intent-filter")
    for node in intent.findall("action")
}
categories = {
    node.get(ANDROID_NS + "name")
    for intent in main_activity.findall("intent-filter")
    for node in intent.findall("category")
}
if "android.intent.action.MAIN" not in actions or "android.intent.category.LAUNCHER" not in categories:
    fail("MainActivity is not the launcher")

source_text = "\n".join(
    path.read_text(encoding="utf-8")
    for path in sorted(JAVA.rglob("*.kt"))
)
for forbidden in ("android.webkit", "WebView", "WebViewAssetLoader", "addJavascriptInterface"):
    if forbidden in source_text:
        fail(f"forbidden native source token found: {forbidden}")
if "android.permission.INTERNET" in source_text:
    fail("INTERNET permission reference found in source")

runtime_web = [
    str(path.relative_to(ROOT))
    for path in SRC.rglob("*")
    if path.is_file() and path.suffix.lower() in {".html", ".htm", ".js"}
]
if runtime_web:
    fail(f"runtime web files found under app/src/main: {runtime_web}")

gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
required_gradle = {
    'applicationId = "edu.gascnagercoil.kaalakolam"': "applicationId",
    "minSdk = 24": "minSdk",
    "targetSdk = 36": "targetSdk",
    "compileSdk = 36": "compileSdk",
    "versionCode = 3": "versionCode",
    'versionName = "2.0.0"': "versionName",
    'applicationIdSuffix = ".debug"': "debug suffix",
    "isMinifyEnabled = true": "R8 minification",
    "isShrinkResources = true": "resource shrinking",
}
for literal, label in required_gradle.items():
    if literal not in gradle:
        fail(f"{label} configuration missing")

gaps = json.loads((CONTENT / "gaps.json").read_text(encoding="utf-8"))
if gaps.get("count") != 0:
    fail(f"gaps count is {gaps.get('count')!r}, expected 0")
if gaps.get("uiLabel") != "English only":
    fail("gaps uiLabel must be exactly 'English only'")
expected_kinds = {
    "deepDive": 0,
    "councilDescription": 0,
    "eldersCrossCheck": 0,
    "predictExplanation": 0,
}
if gaps.get("countsByKind") != expected_kinds:
    fail(f"unexpected gap kind counts: {gaps.get('countsByKind')!r}")

app_ui = (JAVA / "edu/gascnagercoil/kaalakolam/ui/KaalaKolamApp.kt").read_text(encoding="utf-8")
screens = (JAVA / "edu/gascnagercoil/kaalakolam/ui/Screens.kt").read_text(encoding="utf-8")
if gaps.get("count", 0) > 0:
    for kind in expected_kinds:
        if expected_kinds[kind] and f'kind = "{kind}"' not in app_ui:
            fail(f"non-empty gap kind not wired to UI: {kind}")
else:
    if "GapListScreen(" in app_ui:
        fail("zero-gap build must not route production screens through GapListScreen")
if "items(gaps" not in screens or "text = manifest.uiLabel" not in screens:
    fail("gap renderer does not retain the manifest-label contract for future gaps")

def string_keys(path: Path) -> set[str]:
    root = ET.parse(path).getroot()
    return {element.get("name") for element in root.findall("string") if element.get("name")}

en_keys = string_keys(SRC / "res" / "values" / "strings.xml")
ta_keys = string_keys(SRC / "res" / "values-ta" / "strings.xml")
if en_keys != ta_keys:
    fail(f"native EN/TA key mismatch: only EN={sorted(en_keys-ta_keys)}, only TA={sorted(ta_keys-en_keys)}")

credit = "Department of Zoology, GASC, Nagercoil / Created by R. Ramesh"
for path in (SRC / "res" / "values" / "strings.xml", SRC / "res" / "values-ta" / "strings.xml"):
    if credit not in path.read_text(encoding="utf-8"):
        fail(f"exact credit line missing from {path}")

catalogue = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
allowed_groups = ("androidx.", "org.jetbrains.kotlinx", "junit")
for match in re.finditer(r'group\s*=\s*"([^"]+)"', catalogue):
    if not match.group(1).startswith(allowed_groups):
        fail(f"non-approved dependency group: {match.group(1)}")

if "\ufffd" in source_text:
    fail("U+FFFD found in Kotlin source")

print("PASS: source manifest requests no permissions; AndroidX compatibility permission is explicitly removed")
print("PASS: only launcher MainActivity is exported")
print("PASS: allowBackup=false and cleartext disabled")
print("PASS: no android.webkit/WebView/WebViewAssetLoader runtime code")
print("PASS: no HTML or JavaScript under app/src/main")
print("PASS: Android identifiers and release shrinker configuration")
print("PASS: gaps.json has zero untranslated long-form entries; future-gap chip contract retained")
print("PASS: zero-gap production routes use native workflows; future-gap renderer retains manifest.uiLabel")
content_tamil = subprocess.run([sys.executable, str(ROOT / "tools" / "test_tamil_content.py")], text=True, capture_output=True)
if content_tamil.returncode:
    fail(content_tamil.stdout + content_tamil.stderr)
print(content_tamil.stdout.strip())

accessibility = subprocess.run([sys.executable, str(ROOT / "tools" / "test_accessibility_policy.py")], text=True, capture_output=True)
if accessibility.returncode:
    fail(accessibility.stdout + accessibility.stderr)
print(accessibility.stdout.strip())

speech_policy = subprocess.run([sys.executable, str(ROOT / "tools" / "test_speech_policy.py")], text=True, capture_output=True)
if speech_policy.returncode:
    fail(speech_policy.stdout + speech_policy.stderr)
print(speech_policy.stdout.strip())

locale_contract = subprocess.run([sys.executable, str(ROOT / "tools" / "test_locale_contract.py")], text=True, capture_output=True)
if locale_contract.returncode:
    fail(locale_contract.stdout + locale_contract.stderr)
print(locale_contract.stdout.strip())

text_handling = subprocess.run([sys.executable, str(ROOT / "tools" / "test_text_handling.py")], text=True, capture_output=True)
if text_handling.returncode:
    fail(text_handling.stdout + text_handling.stderr)
print(text_handling.stdout.strip())

font_coverage = subprocess.run([sys.executable, str(ROOT / "tools" / "test_font_coverage.py")], text=True, capture_output=True)
if font_coverage.returncode:
    fail(font_coverage.stdout + font_coverage.stderr)
print(font_coverage.stdout.strip())

typography = subprocess.run([sys.executable, str(ROOT / "tools" / "test_tamil_typography.py")], text=True, capture_output=True)
if typography.returncode:
    fail(typography.stdout + typography.stderr)
print(typography.stdout.strip())

parity = subprocess.run([sys.executable, str(ROOT / "tools" / "test_string_parity.py")], text=True, capture_output=True)
if parity.returncode:
    fail(parity.stdout + parity.stderr)
print(parity.stdout.strip())
print("PASS: EN/TA native string parity, placeholders and translation policy")
print("PASS: dependency groups limited to AndroidX, kotlinx and JUnit")
print("NATIVE_STATIC_QA_PASS")
