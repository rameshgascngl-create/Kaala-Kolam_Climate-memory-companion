#!/usr/bin/env python3
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
fixture_path = ROOT / "tests" / "elders" / "kk1-native-fixtures.json"
out = ROOT / "app" / "build" / "elders-html-oracle.html"

html = source.read_text(encoding="utf-8")
expose = """
window.__ELDERS_KK1_ORACLE__ = {
  ivRecord,
  mkCode,
  importCodes,
  resetPool: () => {
    S.pool = {imports: [], plans: [], sample: false};
    S.flags.pool = false;
  },
  poolImports: () => JSON.parse(JSON.stringify(S.pool.imports))
};
"""
idx = html.rfind("</script>")
if idx < 0:
    raise SystemExit("Authoritative HTML has no closing script tag")
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(html[:idx] + expose + html[idx:], encoding="utf-8")

fixture = json.loads(fixture_path.read_text(encoding="utf-8"))
with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto(out.as_uri())
    page.wait_for_function("() => !!window.__ELDERS_KK1_ORACLE__")
    for case in fixture["cases"]:
        result = page.evaluate(
            """({record, code}) => {
              const o = window.__ELDERS_KK1_ORACLE__;
              o.resetPool();
              const imported = o.importCodes(code);
              return {imported, imports: o.poolImports(), webCode: o.mkCode('K', record)};
            }""",
            {"record": case["record"], "code": case["nativeCode"]},
        )
        assert result["imported"]["added"] == 1, (case["name"], result)
        assert result["imported"]["bad"] == 0, (case["name"], result)
        assert result["imports"] == [case["record"]], (case["name"], result)
        assert result["webCode"] == case["nativeCode"], (case["name"], result)

    good = fixture["cases"][0]["nativeCode"]
    corrupt = good[:-1] + ("1" if good[-1] == "0" else "0")
    rejected = page.evaluate(
        """code => {
          const o = window.__ELDERS_KK1_ORACLE__;
          o.resetPool();
          const result = o.importCodes(code);
          return {result, imports: o.poolImports()};
        }""",
        corrupt,
    )
    assert rejected["result"]["added"] == 0, rejected
    assert rejected["result"]["bad"] == 1, rejected
    assert rejected["imports"] == [], rejected
    browser.close()

print("ELDERS_HTML_KK1_ORACLE_PASS cases=4 corrupt=1")
