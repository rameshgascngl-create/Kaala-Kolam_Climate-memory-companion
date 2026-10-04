#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

EXPECTED_ORIGINAL_SHA256 = "ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9"
EXPECTED_ORIGINAL_BYTES = 361_881
EXPECTED_CSP = b"""<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'">"""
HOOK_MARKER = b"/* ===== Android shell hooks (used only by the native wrapper) ===== */"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
TOOLS_NS = "{http://schemas.android.com/tools}"

failures: list[str] = []
passes: list[str] = []


def check(condition: bool, message: str) -> None:
    (passes if condition else failures).append(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def strip_kotlin_non_code(text: str) -> str:
    """Replace comments and string/char literal contents while preserving code layout."""
    out: list[str] = []
    i = 0
    n = len(text)
    state = "code"
    while i < n:
        if state == "code":
            if text.startswith("//", i):
                out.extend("  ")
                i += 2
                state = "line_comment"
                continue
            if text.startswith("/*", i):
                out.extend("  ")
                i += 2
                state = "block_comment"
                continue
            if text.startswith('"""', i):
                out.extend("   ")
                i += 3
                state = "triple_string"
                continue
            c = text[i]
            if c == '"':
                out.append(" ")
                i += 1
                state = "string"
                continue
            if c == "'":
                out.append(" ")
                i += 1
                state = "char"
                continue
            out.append(c)
            i += 1
            continue
        if state == "line_comment":
            c = text[i]
            out.append("\n" if c == "\n" else " ")
            i += 1
            if c == "\n":
                state = "code"
            continue
        if state == "block_comment":
            if text.startswith("*/", i):
                out.extend("  ")
                i += 2
                state = "code"
                continue
            c = text[i]
            out.append("\n" if c == "\n" else " ")
            i += 1
            continue
        if state == "triple_string":
            if text.startswith('"""', i):
                out.extend("   ")
                i += 3
                state = "code"
                continue
            c = text[i]
            out.append("\n" if c == "\n" else " ")
            i += 1
            continue
        if state in ("string", "char"):
            quote = '"' if state == "string" else "'"
            c = text[i]
            if c == "\\" and i + 1 < n:
                out.extend("  ")
                i += 2
                continue
            out.append("\n" if c == "\n" else " ")
            i += 1
            if c == quote:
                state = "code"
            continue
    return "".join(out)


original_path = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
asset_path = ROOT / "app/src/main/assets/www/index.html"
if not original_path.is_file() or not asset_path.is_file():
    failures.append("original/asset HTML present")
else:
    original = original_path.read_bytes()
    asset = asset_path.read_bytes()
    check(len(original) == EXPECTED_ORIGINAL_BYTES, f"original HTML size = {EXPECTED_ORIGINAL_BYTES}")
    check(sha256(original) == EXPECTED_ORIGINAL_SHA256, "original HTML SHA-256 matches source of truth")
    check(
        original.count(EXPECTED_CSP) == 1 and asset.count(EXPECTED_CSP) == 1,
        "CSP is exact and occurs once in original and asset",
    )
    check(
        original.count(b"kaala_kolam_v1") >= 1 and asset.count(b"kaala_kolam_v1") >= 1,
        "localStorage key kaala_kolam_v1 is preserved",
    )
    try:
        start = asset.index(HOOK_MARKER)
        boot = asset.index(b"\nboot();", start)
        restored = asset[:start] + asset[boot + 1 :]
        check(asset.count(HOOK_MARKER) == 1, "exactly one Android hook block exists")
        check(
            b"window.__appIsHome=" in asset[start:boot]
            and b"window.__appGoHome=" in asset[start:boot]
            and b"window.__appBack=" in asset[start:boot],
            "hook block defines __appIsHome, __appGoHome and __appBack",
        )
        check(restored == original, "removing hook block restores original HTML byte-for-byte")
    except ValueError:
        failures.append("Android hook block can be located and removed")


resource_defs: set[tuple[str, str]] = set()
res_root = ROOT / "app/src/main/res"
xml_files = sorted(res_root.rglob("*.xml")) if res_root.is_dir() else []
for path in xml_files:
    try:
        tree = ET.parse(path)
    except ET.ParseError as exc:
        failures.append(f"XML well-formed: {path.relative_to(ROOT)} ({exc})")
        continue
    parent = path.parent.name
    if parent.startswith("values"):
        for child in tree.getroot():
            name = child.attrib.get("name")
            if not name:
                continue
            typ = child.attrib.get("type") if child.tag == "item" else child.tag
            if typ:
                resource_defs.add((typ, name))
    else:
        typ = parent.split("-", 1)[0]
        resource_defs.add((typ, path.stem))
    text = path.read_text(encoding="utf-8")
    for rid in re.findall(r"@\+id/([A-Za-z0-9_]+)", text):
        resource_defs.add(("id", rid))
if xml_files:
    passes.append(f"XML well-formed ({len(xml_files)} files)")


def string_keys(path: Path) -> set[str]:
    root = ET.parse(path).getroot()
    return {e.attrib["name"] for e in root.findall("string")}


try:
    en_keys = string_keys(res_root / "values/strings.xml")
    ta_keys = string_keys(res_root / "values-ta/strings.xml")
    check(en_keys == ta_keys, f"EN/TA string keys match ({len(en_keys)} keys)")
    check("privacy_url" in en_keys and "privacy_url" in ta_keys, "privacy_url exists in both EN and TA resources")
except Exception as exc:
    failures.append(f"EN/TA string key check ({exc})")


manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
try:
    manifest = ET.parse(manifest_path).getroot()
    requested = []
    for node in manifest.findall("uses-permission"):
        if node.attrib.get(TOOLS_NS + "node") != "remove":
            requested.append(node.attrib.get(ANDROID_NS + "name", "<unnamed>"))
    check(not requested, "source manifest requests no Android permission")
    app = manifest.find("application")
    activity_names = {
        a.attrib.get(ANDROID_NS + "name") for a in app.findall("activity")
    } if app is not None else set()
    for cls in (".MainActivity", ".AboutActivity"):
        src = ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam" / (cls[1:] + ".kt")
        check(cls in activity_names and src.is_file(), f"manifest class {cls} has matching Kotlin source")
except Exception as exc:
    failures.append(f"manifest static check ({exc})")


missing_refs: set[str] = set()
ref_re = re.compile(r"@([A-Za-z0-9_]+)/([A-Za-z0-9_.]+)")
for path in xml_files:
    text = path.read_text(encoding="utf-8")
    for typ, name in ref_re.findall(text):
        if (typ, name) not in resource_defs:
            missing_refs.add(f"{path.relative_to(ROOT)} -> @{typ}/{name}")
kotlin_files = sorted((ROOT / "app/src/main/java").rglob("*.kt"))
for path in kotlin_files:
    text = path.read_text(encoding="utf-8")
    for typ, name in re.findall(r"\bR\.([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)", text):
        if (typ, name) not in resource_defs:
            missing_refs.add(f"{path.relative_to(ROOT)} -> R.{typ}.{name}")
check(not missing_refs, "all local @resource and R.* references resolve statically")
if missing_refs:
    failures.extend(sorted(missing_refs))


forbidden_hits: list[str] = []
for path in kotlin_files:
    code = strip_kotlin_non_code(path.read_text(encoding="utf-8"))
    patterns = {
        "addJavascriptInterface": r"\baddJavascriptInterface\s*\(",
        "Runtime.exec": r"\bRuntime\s*\.\s*getRuntime\s*\(\s*\)\s*\.\s*exec\s*\(",
        "ProcessBuilder": r"\bProcessBuilder\s*\(",
    }
    for label, pattern in patterns.items():
        if re.search(pattern, code):
            forbidden_hits.append(f"{path.relative_to(ROOT)}: {label}")
check(not forbidden_hits, "no executable addJavascriptInterface/exec/ProcessBuilder use")
if forbidden_hits:
    failures.extend(forbidden_hits)


app_src = ROOT / "app/src"
text_sources = []
for path in sorted(app_src.rglob("*")) if app_src.is_dir() else []:
    if path.is_file() and path.suffix.lower() in {".kt", ".xml", ".js", ".html", ".txt", ".pro"}:
        try:
            text_sources.append((path, path.read_text(encoding="utf-8")))
        except UnicodeDecodeError:
            pass
check(not any("file://" in text for _, text in text_sources), "app/src contains no file:// URL")
check(
    not any(
        "android.permission.INTERNET" in text
        or '<uses-permission android:name="android.permission.INTERNET"' in text
        for _, text in text_sources
    ),
    "app/src contains no INTERNET permission",
)

constants = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/Constants.kt").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/MainActivity.kt").read_text(encoding="utf-8")
tts_bridge = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/TtsBridge.kt").read_text(encoding="utf-8")
check(
    'const val HOST = "appassets.androidplatform.net"' in constants
    and 'const val ORIGIN = "https://$HOST"' in constants,
    "Constants fixes the appassets HTTPS origin",
)
for listener, source in [
    ("AndroidTheme", main_activity),
    ("AndroidLocale", main_activity),
    ("AndroidTTS", tts_bridge),
]:
    listener_ok = re.search(
        r'addWebMessageListener\s*\([^;]*?"' + re.escape(listener) + r'"\s*,\s*origins',
        source,
        re.S,
    ) is not None
    origin_ok = "val origins = setOf(Constants.ORIGIN)" in source
    check(listener_ok and origin_ok, f"{listener} WebMessageListener is restricted to Constants.ORIGIN")


secret_files = []
for pattern in ("*.jks", "*.keystore", "keystore.properties"):
    secret_files.extend(
        p.relative_to(ROOT).as_posix() for p in ROOT.rglob(pattern) if p.is_file()
    )
check(not secret_files, "no keystore or keystore.properties file is present")
if secret_files:
    failures.extend("secret file: " + p for p in secret_files)


for item in passes:
    print("PASS:", item)
if failures:
    for item in failures:
        print("FAIL:", item)
    sys.exit(1)
print(f"PASS: static verification complete ({len(passes)} checks)")
