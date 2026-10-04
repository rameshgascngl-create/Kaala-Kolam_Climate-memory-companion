#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/speech/SpeechController.kt").read_text(encoding="utf-8")
ui = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/speech/SpeechUi.kt").read_text(encoding="utf-8")
required = ['Locale("ta", "IN")', "isLanguageAvailable", "ACTION_INSTALL_TTS_DATA", "missingShown"]
for token in required:
    if token not in source:
        raise SystemExit("SPEECH_POLICY_FAIL missing " + token)
if "AlertDialog" not in ui or "tts_missing_title" not in ui:
    raise SystemExit("SPEECH_POLICY_FAIL missing once-per-session UI")
for token in ("IPCC","IMD","NOAA","WMO","UNEP","ENSO","CO₂","ppm","°C","km","µm","W/m²"):
    if f'"{token}" to ' not in source:
        raise SystemExit("SPEECH_POLICY_FAIL pronunciation map missing " + token)
print("SPEECH_POLICY_PASS ta-IN availability-check install-dialog pronunciation-map")
