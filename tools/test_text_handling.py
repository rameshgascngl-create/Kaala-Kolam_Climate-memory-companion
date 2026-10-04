#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java"
EXCEPTIONS = ROOT / "tools/text_handling_exceptions.txt"
TOKENS = (".take(", ".substring(", ".dropLast(", ".uppercase(", ".lowercase(", "String.format(")

allowed = {}
for raw in EXCEPTIONS.read_text(encoding="utf-8").splitlines():
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    path, literal, reason = [part.strip() for part in line.split("|", 2)]
    allowed.setdefault((path, literal), []).append(reason)

violations = []
exercised = set()
for path in sorted(JAVA.rglob("*.kt")):
    rel = str(path.relative_to(ROOT))
    text = path.read_text(encoding="utf-8")
    for number, line in enumerate(text.splitlines(), 1):
        for token in TOKENS:
            if token not in line:
                continue
            key = (rel, token)
            if key in allowed:
                exercised.add(key)
            else:
                violations.append(f"{rel}:{number}: {token} :: {line.strip()}")

unused = sorted(set(allowed) - exercised)
if violations:
    raise SystemExit("TEXT_HANDLING_FAIL\n" + "\n".join(violations))
if unused:
    raise SystemExit("TEXT_HANDLING_FAIL unused exceptions=" + repr(unused))
print(f"TEXT_HANDLING_PASS exceptions={len(exercised)}")
