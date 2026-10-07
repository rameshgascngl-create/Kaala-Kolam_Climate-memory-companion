#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
screens = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/PrototypeWorkflowScreens.kt").read_text(encoding="utf-8")
fixture = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/KaalaKolamApp.kt").read_text(encoding="utf-8")
fidelity = (ROOT / "app/src/main/java/edu/gascnagercoil/kaalakolam/ui/FidelityProbe.kt").read_text(encoding="utf-8")
visual = (ROOT / "app/src/test/java/edu/gascnagercoil/kaalakolam/ui/ClassVisualStateMatrixTest.kt").read_text(encoding="utf-8")
storage = (ROOT / "app/src/test/java/edu/gascnagercoil/kaalakolam/data/AppStateRealStorageTest.kt").read_text(encoding="utf-8")

required_screen = (
    'var code by rememberSaveable { mutableStateOf("") }',
    'accessibilityLabel = copy.pastePlaceholder.text(lang)',
    'tag = "class-import-input"',
    'tag = "class-sample-toggle"',
    'tagPrefix = "class-group"',
)
for snippet in required_screen:
    if snippet not in screens:
        raise SystemExit("CLASS_CLOSURE_FAIL missing screen contract: " + snippet)

for snippet in (
    'visualState.startsWith("class-") -> Destination.CLASS.route',
    '"class-empty"',
    '"class-sample"',
    '"class-imported"',
    '"class-place"',
    '"class-decade"',
):
    if snippet not in fixture:
        raise SystemExit("CLASS_CLOSURE_FAIL missing visual fixture state: " + snippet)

for snippet in (
    'private val states = listOf(',
    '"class-empty"',
    '"class-sample"',
    '"class-imported"',
    '"class-place"',
    '"class-decade"',
    'listOf(390 to 844, 320 to 568)',
    'listOf("en", "ta")',
    'ComposeDensity(1f, 2f)',
):
    if snippet not in visual:
        raise SystemExit("CLASS_CLOSURE_FAIL missing 200% visual matrix contract: " + snippet)

if "48.dp.roundToPx()" not in fidelity:
    raise SystemExit("CLASS_CLOSURE_FAIL 48dp touch-target enforcement missing")

for snippet in (
    "classPool = ClassPoolState(",
    "sample = true",
    "groupBy = ClassGroupBy.DECADE",
    "assertEquals(expected, reloaded)",
):
    if snippet not in storage:
        raise SystemExit("CLASS_CLOSURE_FAIL real-storage Class persistence coverage missing: " + snippet)

print(
    "CLASS_CLOSURE_STATIC_PASS "
    "stable-tags=3 talkback=1 saveable-draft=1 touch-target=48dp "
    "visual-states=5 viewports=2 languages=2 fontScale=200 storage-reopen=1"
)
