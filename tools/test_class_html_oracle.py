#!/usr/bin/env python3
import json
import math
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
FIXTURE = ROOT / "app" / "src" / "test" / "resources" / "class" / "class-parity-fixtures.json"
OUT = ROOT / "app" / "build" / "class-html-oracle.html"

html = SOURCE.read_text(encoding="utf-8")
expose = r"""
window.__CLASS_POOL_ORACLE__ = {
  mkCode,
  resetPool: pool => {
    const p = pool || {};
    S.pool = {
      imports: JSON.parse(JSON.stringify(p.imports || [])),
      plans: JSON.parse(JSON.stringify(p.plans || [])),
      sample: false,
      groupBy: 'all'
    };
    S.flags.pool = false;
  },
  importCodes,
  pool: () => JSON.parse(JSON.stringify(S.pool)),
  aggregateFor: (records, qi, groupBy) => {
    const keyOf = groupBy === 'place'
      ? (r => r.p)
      : groupBy === 'decade'
        ? (r => r.b)
        : (() => 0);
    const keys = groupBy === 'place'
      ? [0,1,2,3,4].map(v => ({v, label: 'p' + v}))
      : groupBy === 'decade'
        ? [0,1,2,3,4,5].map(v => ({v, label: 'd' + v}))
        : [{v: 0, label: 'all'}];
    return aggregate(records, qi, keyOf, keys).map(r => ({
      label: r.label,
      counts: r.c,
      n: r.n,
      mean: r.mean,
      agreement: r.agree
    }));
  },
  strongest: records => Q.map((q, i) => {
    const g = aggregate(records, i, () => 0, [{v: 0, label: ''}])[0];
    const score = g.n >= 5 && g.agree != null && g.mean != null
      ? Math.abs(g.mean) * g.agree
      : 0;
    return {
      questionIndex: i,
      n: g.n,
      mean: g.mean,
      agreement: g.agree,
      score
    };
  }).sort((a, b) => b.score - a.score).slice(0, 3).filter(x => x.score > 0.3),
  planSummary: plans => ({
    optionCounts: OPT.map((_, k) => plans.filter(p => p.o.includes(k)).length),
    plansLeavingSomeoneUnprotected: plans.filter(p => {
      const picks = p.o.map(k => OPT[k].id);
      return uncovered(picks).length > 0;
    }).length
  })
};
"""
needle = "boot();\n})();"
idx = html.rfind(needle)
if idx < 0:
    raise SystemExit("CLASS_HTML_ORACLE_FAIL authoritative boot/IIFE marker not found")
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(html[:idx] + expose + "\n" + html[idx:], encoding="utf-8")

fixture = json.loads(FIXTURE.read_text(encoding="utf-8"))


def assert_close(actual, expected, path="root"):
    if isinstance(expected, bool) or expected is None or isinstance(expected, str):
        assert actual == expected, (path, actual, expected)
        return
    if isinstance(expected, (int, float)):
        assert isinstance(actual, (int, float)), (path, actual, expected)
        assert math.isclose(float(actual), float(expected), rel_tol=0.0, abs_tol=1e-12), (
            path,
            actual,
            expected,
        )
        return
    if isinstance(expected, list):
        assert isinstance(actual, list), (path, actual, expected)
        assert len(actual) == len(expected), (path, len(actual), len(expected))
        for index, (left, right) in enumerate(zip(actual, expected)):
            assert_close(left, right, path + "[" + str(index) + "]")
        return
    if isinstance(expected, dict):
        assert isinstance(actual, dict), (path, actual, expected)
        assert set(actual) == set(expected), (path, set(actual), set(expected))
        for key in expected:
            assert_close(actual[key], expected[key], path + "." + key)
        return
    raise AssertionError((path, type(actual), type(expected)))


with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto(OUT.as_uri())
    page.wait_for_function("() => !!window.__CLASS_POOL_ORACLE__")

    for case in fixture["codeCases"]:
        result = page.evaluate(
            """caseData => {
              const o = window.__CLASS_POOL_ORACLE__;
              const webCode = o.mkCode(caseData.kind, caseData.record);
              o.resetPool({imports: [], plans: []});
              const imported = o.importCodes(caseData.code);
              const pool = o.pool();
              return {
                webCode,
                imported: {
                  found: imported.found,
                  added: imported.added,
                  duplicates: imported.dup,
                  rejected: imported.bad
                },
                importIds: pool.imports.map(r => r.i),
                planIds: pool.plans.map(r => r.i)
              };
            }""",
            case,
        )
        assert result["webCode"] == case["code"], (case["name"], result["webCode"], case["code"])
        assert result["imported"] == {
            "found": 1,
            "added": 1,
            "duplicates": 0,
            "rejected": 0,
        }, (case["name"], result)
        if case["kind"] == "K":
            assert result["importIds"] == [case["record"]["i"]], (case["name"], result)
            assert result["planIds"] == [], (case["name"], result)
        else:
            assert result["importIds"] == [], (case["name"], result)
            assert result["planIds"] == [case["record"]["i"]], (case["name"], result)

    for case in fixture["invalidCodes"]:
        result = page.evaluate(
            """caseData => {
              const o = window.__CLASS_POOL_ORACLE__;
              o.resetPool({imports: [], plans: []});
              const imported = o.importCodes(caseData.code);
              const pool = o.pool();
              return {
                found: imported.found,
                added: imported.added,
                duplicates: imported.dup,
                rejected: imported.bad,
                imports: pool.imports,
                plans: pool.plans
              };
            }""",
            case,
        )
        assert result["found"] == 1, (case["name"], result)
        assert result["added"] == 0, (case["name"], result)
        assert result["duplicates"] == 0, (case["name"], result)
        assert result["rejected"] == 1, (case["name"], result)
        assert result["imports"] == [], (case["name"], result)
        assert result["plans"] == [], (case["name"], result)

    for case in fixture["importCases"]:
        result = page.evaluate(
            """caseData => {
              const o = window.__CLASS_POOL_ORACLE__;
              o.resetPool(caseData.existing);
              const imported = o.importCodes(caseData.text);
              const pool = o.pool();
              return {
                found: imported.found,
                added: imported.added,
                duplicates: imported.dup,
                rejected: imported.bad,
                importIds: pool.imports.map(r => r.i),
                planIds: pool.plans.map(r => r.i)
              };
            }""",
            case,
        )
        assert result == case["expected"], (case["name"], result, case["expected"])

    records = fixture["aggregateRecords"]
    for fixture_key, group_by in [
        ("allQ0", "all"),
        ("placeQ0", "place"),
        ("decadeQ0", "decade"),
    ]:
        result = page.evaluate(
            """args => window.__CLASS_POOL_ORACLE__.aggregateFor(
              args.records, 0, args.groupBy
            )""",
            {"records": records, "groupBy": group_by},
        )
        assert_close(result, fixture["aggregates"][fixture_key], fixture_key)

    strongest = page.evaluate(
        "records => window.__CLASS_POOL_ORACLE__.strongest(records)",
        records,
    )
    assert_close(strongest, fixture["strongest"], "strongest")

    plan_summary = page.evaluate(
        "plans => window.__CLASS_POOL_ORACLE__.planSummary(plans)",
        fixture["planRecords"],
    )
    assert_close(plan_summary, fixture["planSummary"], "planSummary")

    browser.close()

print(
    "CLASS_HTML_ORACLE_PASS "
    "codes=" + str(len(fixture["codeCases"])) +
    " invalid=" + str(len(fixture["invalidCodes"])) +
    " imports=" + str(len(fixture["importCases"])) +
    " aggregate_modes=3 strongest=3 plans=" + str(len(fixture["planRecords"]))
)
