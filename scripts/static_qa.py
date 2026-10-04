#!/usr/bin/env python3
"""Static release-safety checks for Kaala Kolam.

Uses only the Python standard library so CI does not need extra packages.
"""
from __future__ import annotations

from pathlib import Path
import hashlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
SRC = APP / "src" / "main"
ORIGINAL = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
HTML = SRC / "assets" / "www" / "index.html"

ORIGINAL_SIZE = 361_881
ORIGINAL_SHA256 = "ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9"
CSP = (
    '<meta http-equiv="Content-Security-Policy" '
    'content="default-src \'none\'; script-src \'unsafe-inline\'; '
    'style-src \'unsafe-inline\'; img-src data:; base-uri \'none\'; '
    'form-action \'none\'">'
)
HOOK_MARKER = b"/* ===== Android shell hooks (used only by the native wrapper) ===== */\n"
TOOLS_NODE = "{http://schemas.android.com/tools}node"
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def ok(message: str) -> None:
    print(f"PASS: {message}")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def strip_comments_and_strings(src: str) -> str:
    """Blank Kotlin/Java comments and quoted strings while preserving line numbers."""
    out: list[str] = []
    i = 0
    state = "code"
    quote = ""
    while i < len(src):
        c = src[i]
        d = src[i + 1] if i + 1 < len(src) else ""
        if state == "code":
            if c == "/" and d == "/":
                state = "line"
                out.extend((" ", " "))
                i += 2
                continue
            if c == "/" and d == "*":
                state = "block"
                out.extend((" ", " "))
                i += 2
                continue
            if c in ('"', "'"):
                quote = c
                state = "string"
                out.append(" ")
                i += 1
                continue
            out.append(c)
            i += 1
            continue
        if state == "line":
            out.append("\n" if c == "\n" else " ")
            if c == "\n":
                state = "code"
            i += 1
            continue
        if state == "block":
            if c == "*" and d == "/":
                state = "code"
                out.extend((" ", " "))
                i += 2
            else:
                out.append("\n" if c == "\n" else " ")
                i += 1
            continue
        if state == "string":
            if c == "\\":
                out.extend((" ", " "))
                i += 2
                continue
            if c == quote:
                state = "code"
            out.append("\n" if c == "\n" else " ")
            i += 1
    return "".join(out)


def xml_string_keys(path: Path) -> set[str]:
    root = ET.parse(path).getroot()
    return {
        item.attrib["name"]
        for item in root
        if item.tag == "string" and "name" in item.attrib
    }


original = ORIGINAL.read_bytes()
if len(original) != ORIGINAL_SIZE:
    fail(f"original HTML size is {len(original)}, expected {ORIGINAL_SIZE}")
if sha256(original) != ORIGINAL_SHA256:
    fail("original HTML SHA-256 mismatch")
ok("original HTML fingerprint")

app_html = HTML.read_bytes()
if app_html.count(HOOK_MARKER) != 1:
    fail("Android hook marker must occur exactly once")
hook_start = app_html.index(HOOK_MARKER)
boot_start = app_html.find(b"boot();", hook_start)
if boot_start < 0:
    fail("boot(); not found after Android hook block")
hook = app_html[hook_start:boot_start]
for symbol in (b"window.__appIsHome", b"window.__appGoHome", b"window.__appBack"):
    if hook.count(symbol) != 1:
        fail(f"{symbol.decode()} must occur exactly once in the permitted hook block")
restored = app_html[:hook_start] + app_html[boot_start:]
if sha256(restored) != ORIGINAL_SHA256 or restored != original:
    fail("removing the hook block does not restore the original HTML byte-for-byte")
ok("HTML equals original plus only the permitted hook block")

html_text = app_html.decode("utf-8")
if html_text.count(CSP) != 1:
    fail("required CSP is absent or duplicated/changed")
if "const KEY='kaala_kolam_v1';" not in html_text:
    fail("localStorage key changed")
ok("CSP and localStorage key unchanged")

# Scan raw app/src bytes for forbidden network/file markers.
for path in SRC.rglob("*"):
    if not path.is_file():
        continue
    data = path.read_bytes()
    if b"file://" in data:
        fail(f"file:// found in {path.relative_to(ROOT)}")
    if b"INTERNET" in data:
        fail(f"INTERNET marker found in {path.relative_to(ROOT)}")
ok("no file:// or INTERNET marker in app/src")

# The previous scanner incorrectly matched the API name in a comment.
# Inspect executable Kotlin/Java text instead.
call_re = re.compile(r"\baddJavascriptInterface\s*\(")
for path in list((SRC / "java").rglob("*.kt")) + list((SRC / "java").rglob("*.java")):
    cleaned = strip_comments_and_strings(path.read_text(encoding="utf-8"))
    match = call_re.search(cleaned)
    if match:
        line = cleaned.count("\n", 0, match.start()) + 1
        fail(f"executable addJavascriptInterface(...) call at {path.relative_to(ROOT)}:{line}")
ok("no executable addJavascriptInterface(...) call")

constants = (SRC / "java/edu/gascnagercoil/kaalakolam/Constants.kt").read_text(encoding="utf-8")
if 'const val HOST = "appassets.androidplatform.net"' not in constants:
    fail("WebView host constant changed")
if 'const val ORIGIN = "https://$HOST"' not in constants:
    fail("WebView origin constant changed")
if 'const val START_URL = "$ORIGIN/assets/www/index.html"' not in constants:
    fail("WebView start URL changed")

main = (SRC / "java/edu/gascnagercoil/kaalakolam/MainActivity.kt").read_text(encoding="utf-8")
tts = (SRC / "java/edu/gascnagercoil/kaalakolam/TtsBridge.kt").read_text(encoding="utf-8")
if "val origins = setOf(Constants.ORIGIN)" not in main:
    fail("MainActivity does not build its bridge allow-list from Constants.ORIGIN")
if "val origins = setOf(Constants.ORIGIN)" not in tts:
    fail("TtsBridge does not build its bridge allow-list from Constants.ORIGIN")
for source, listener in (
    (main, "AndroidTheme"),
    (main, "AndroidLocale"),
    (tts, "AndroidTTS"),
):
    pattern = re.compile(
        r"addWebMessageListener\s*\(\s*(?:wv|webView)\s*,\s*"
        + re.escape(f'"{listener}"')
        + r"\s*,\s*origins\b",
        re.S,
    )
    if not pattern.search(source):
        fail(f"{listener} is not registered with the restricted origins set")
ok("AndroidTTS, AndroidTheme and AndroidLocale bridges are origin-restricted")

# XML parse and source-manifest permission policy.
xml_paths = [SRC / "AndroidManifest.xml", *list((SRC / "res").rglob("*.xml"))]
for path in xml_paths:
    ET.parse(path)
ok(f"{len(xml_paths)} Android XML files are well-formed")

manifest = ET.parse(SRC / "AndroidManifest.xml").getroot()
for node in list(manifest):
    local = node.tag.rsplit("}", 1)[-1]
    if local in {"permission", "uses-permission"}:
        name = node.attrib.get(ANDROID_NAME, "")
        if (
            node.attrib.get(TOOLS_NODE) != "remove"
            or name != "${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        ):
            fail(f"unexpected manifest permission node: {local} {name}")
ok("source manifest contains only removal markers, no requested permission")

en = SRC / "res/values/strings.xml"
ta = SRC / "res/values-ta/strings.xml"
en_keys = xml_string_keys(en)
ta_keys = xml_string_keys(ta)
if en_keys != ta_keys:
    fail(
        "EN/TA string keys differ: "
        f"missing in TA={sorted(en_keys - ta_keys)}, missing in EN={sorted(ta_keys - en_keys)}"
    )
if "privacy_url" not in en_keys:
    fail("privacy_url missing from resources")
ok(f"EN/TA string keys match exactly ({len(en_keys)} keys)")

build = (APP / "build.gradle.kts").read_text(encoding="utf-8")
if 'buildConfigField("boolean", "SHOW_NATIVE_BAR", "true")' not in build:
    fail("SHOW_NATIVE_BAR is not defined true by default")
for key in ("KK_STORE_FILE", "KK_STORE_PASSWORD", "KK_KEY_ALIAS", "KK_KEY_PASSWORD"):
    if f'secret("{key}")' not in build:
        fail(f"optional signing source {key} missing")
ok("native toolbar flag and optional signing inputs are configured")

print("STATIC_QA_PASS")
