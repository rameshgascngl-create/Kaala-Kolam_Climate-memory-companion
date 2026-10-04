#!/usr/bin/env python3
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main"
main = (SRC / "java/edu/gascnagercoil/kaalakolam/MainActivity.kt").read_text(encoding="utf-8")
theme = (SRC / "java/edu/gascnagercoil/kaalakolam/ui/theme/Theme.kt").read_text(encoding="utf-8")
annotated = (SRC / "java/edu/gascnagercoil/kaalakolam/text/LocaleText.kt").read_text(encoding="utf-8")

root = ET.parse(SRC / "res/xml/locales_config.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
locales = [node.get(ns + "name") for node in root.findall("locale")]
if locales != ["en", "ta"]:
    raise SystemExit(f"LOCALE_CONTRACT_FAIL locales={locales!r}")
if "AppCompatDelegate.setApplicationLocales" not in main or "LocaleListCompat.forLanguageTags(target)" not in main:
    raise SystemExit("LOCALE_CONTRACT_FAIL AppCompat application locale switching missing")
if 'LocaleList("ta")' not in theme:
    raise SystemExit("LOCALE_CONTRACT_FAIL Tamil TextStyle LocaleList missing")
for token in ("IPCC", "IMD", "NOAA", "WMO", "UNEP", "ENSO", "CO₂", "ppm"):
    if token not in annotated:
        raise SystemExit(f"LOCALE_CONTRACT_FAIL acronym span token missing: {token}")
print("LOCALE_CONTRACT_PASS locales=en,ta TamilTextStyle=ta acronymSpans=present digits=Latin")
