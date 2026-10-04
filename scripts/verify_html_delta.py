#!/usr/bin/env python3
from pathlib import Path
import hashlib, re
EXPECTED_SHA256 = "ae7b87a4f90fd990806f68cf6db1d3a0903d9cf6b5ef75c9be2c5392e66508a9"
EXPECTED_CSP = """<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'">"""
path = Path(__file__).resolve().parents[1] / "app/src/main/assets/www/index.html"
text = path.read_text(encoding="utf-8")
if EXPECTED_CSP not in text:
    raise SystemExit("FAIL: CSP differs from source-of-truth value")
pattern = re.compile(r"/\* ===== Android shell hooks \(used only by the native wrapper\) ===== \*/\nwindow\.__appIsHome=.*?\n};\n\n(?=boot\(\);)", re.S)
m = pattern.search(text)
if not m:
    raise SystemExit("FAIL: permitted Android hook block not found")
restored = (text[:m.start()] + text[m.end():]).encode("utf-8")
sha = hashlib.sha256(restored).hexdigest()
print(f"restored_sha256={sha}")
if sha != EXPECTED_SHA256:
    raise SystemExit("FAIL: removing hook block does not reproduce original HTML bytes")
print("PASS: HTML equals original plus only permitted hook block")
