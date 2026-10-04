#!/usr/bin/env python3
"""Assert that the built merged Android manifest remains permission-free."""
from __future__ import annotations

import argparse
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("manifest", type=Path)
args = parser.parse_args()

root = ET.parse(args.manifest).getroot()
permissions = [
    element.get("{http://schemas.android.com/apk/res/android}name", "<unnamed>")
    for element in root
    if element.tag in {"uses-permission", "uses-permission-sdk-23", "permission"}
]
if permissions:
    raise SystemExit("MERGED_MANIFEST_PERMISSION_FAIL: " + ", ".join(permissions))

print(f"MERGED_MANIFEST_PERMISSION_PASS: {args.manifest}")
