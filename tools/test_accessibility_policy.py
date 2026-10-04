#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
app = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/KaalaKolamApp.kt").read_text(encoding="utf-8")
screens = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/Screens.kt").read_text(encoding="utf-8")
test = (ROOT / "app/src/androidTest/java/edu/gascnagercoil/kaalakolam/ui/TamilNavigationSemanticsTest.kt").read_text(encoding="utf-8")
if "contentDescription = localized(lang, destination.en, destination.ta)" not in app:
    raise SystemExit("ACCESSIBILITY_FAIL tab icon descriptions missing")
for phrase in ("Memory stripes showing climate change over time", "Kolam learning progress:", "Chart showing three generations"):
    if phrase not in screens:
        raise SystemExit("ACCESSIBILITY_FAIL chart/graphic description missing: " + phrase)
for label in ("முகப்பு","கற்க","மூத்தோர்","வகுப்பு","ஊர்சபை","கணிப்பு"):
    if label not in test:
        raise SystemExit("ACCESSIBILITY_FAIL semantics test label missing: " + label)
print("ACCESSIBILITY_STATIC_PASS six-tab semantics test present icons-and-charts-described")
