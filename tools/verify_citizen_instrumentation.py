#!/usr/bin/env python3
"""Fail if expected device tests were not executed, or if any reported test failed/skipped."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path("app/build/outputs/androidTest-results/connected")
expected = {
    ("CitizenNavigationTest", "allFiveTamilSectionsSelectActualRoutes"),
    ("CitizenNavigationTest", "realNavigationHostOpensCitizenAndLegacyModules"),
    ("CitizenNavigationTest", "tamilNavigationAtDoubleFontScaleRetainsFiveTargets"),
    ("CitizenNavigationTest", "climateShowsUnavailableRatherThanInventedTrend"),
    ("CitizenOfflineScreenTest", "todayShowsNoDataAndManualSelectionInEnglish"),
    ("CitizenOfflineScreenTest", "safetyIsUnknownInTamil"),
    ("TamilNavigationSemanticsTest", "allSixTamilTabsExposeTamilContentDescriptions"),
}
seen = set()
for path in root.rglob("TEST-*.xml"):
    for case in ET.parse(path).getroot().iter("testcase"):
        key = (case.attrib.get("classname", "").split(".")[-1], case.attrib.get("name", ""))
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            raise SystemExit(f"CITIZEN_INSTRUMENTATION_FAIL {key}")
        seen.add(key)
missing = expected - seen
if missing:
    raise SystemExit(f"CITIZEN_INSTRUMENTATION_MISSING {sorted(missing)}")
print(f"CITIZEN_INSTRUMENTATION_PASS expected={len(expected)} actual={len(seen)}")
