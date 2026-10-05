#!/usr/bin/env python3
"""Generate M2 domain golden fixtures from the shipped web prototype itself.

The HTML file is never edited. A temporary in-memory copy exports only the
prototype's deterministic domain functions/data, then Playwright evaluates
fixed fixtures. The committed JSON is compared for drift in CI.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "Kaala_Kolam_Climate_Memory_Companion.html"
OUT = ROOT / "tests" / "m2" / "web-golden.json"


def instrumented_html() -> str:
    source = HTML.read_text(encoding="utf-8")
    needle = "boot();\n})();"
    if source.count(needle) != 1:
        raise SystemExit("M2_GOLDEN_FAIL expected one boot closure marker")
    export = """window.__m2 = {
      mkCode,
      sampleRecords,
      aggregate,
      riskFor,
      uncovered,
      spent,
      PRED
    };
})();"""
    return source.replace(needle, export, 1)


def canonical_sha(value: object) -> str:
    compact = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return hashlib.sha256(compact.encode("utf-8")).hexdigest()


def main() -> None:
    source_bytes = HTML.read_bytes()
    source_sha = hashlib.sha256(source_bytes).hexdigest()
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        context = browser.new_context(bypass_csp=True)
        page = context.new_page()
        try:
            page.set_content(instrumented_html(), wait_until="load")
            data = page.evaluate(
                """() => {
                  const m = window.__m2;
                  if (!m) throw new Error('M2 export missing');
                  const interview = {t:'I',i:'iv000001',p:2,b:4,a:'1234501234'};
                  const plan = {t:'P',i:'pl000001',s:2,o:[0,1,4]};
                  const records = m.sampleRecords();
                  const aggregateRows = (questionIndex, groupBy) => {
                    if (groupBy === 'all') {
                      return m.aggregate(
                        records,
                        questionIndex,
                        () => 0,
                        [{v:0,label:'all'}]
                      );
                    }
                    return m.aggregate(
                      records,
                      questionIndex,
                      r => r.p,
                      [0,1,2,3,4].map(v => ({v,label:'p'+v}))
                    );
                  };
                  const council = (name, picks, scenario) => {
                    const result = m.riskFor(picks, scenario);
                    return {
                      name,
                      picks,
                      scenario,
                      spent: m.spent(picks),
                      hazard: result.H,
                      risks: result.r,
                      mean: result.mean,
                      uncovered: m.uncovered(picks).map(x => x.id)
                    };
                  };
                  return {
                    shareCodes: {
                      interview: {record: interview, code: m.mkCode('K', interview)},
                      plan: {record: plan, code: m.mkCode('P', plan)}
                    },
                    sampleRecords: records,
                    aggregates: [
                      {name:'q0-all',questionIndex:0,groupBy:'all',rows:aggregateRows(0,'all')},
                      {name:'q7-all',questionIndex:7,groupBy:'all',rows:aggregateRows(7,'all')},
                      {name:'q0-place',questionIndex:0,groupBy:'place',rows:aggregateRows(0,'place')}
                    ],
                    council: [
                      council('none_today', [], 0),
                      council('warn_shelter_today', ['warn','shelter'], 0),
                      council('mangrove_clean_high', ['mangrove','clean'], 2),
                      council('balanced_strong', ['warn','shelter','drains','income'], 1)
                    ],
                    predictionSpecs: m.PRED.map(p => ({
                      id:p.id,min:p.min,max:p.max,step:p.step,initial:p.init,
                      low:p.lo,high:p.hi,unit:p.unit
                    }))
                  };
                }"""
            )
        finally:
            context.close()
            browser.close()

    records = data.pop("sampleRecords")
    payload = {
        "schemaVersion": 1,
        "sourceHtml": HTML.name,
        "sourceHtmlSha256": source_sha,
        "shareCodes": data["shareCodes"],
        "sampleRecords": {
            "count": len(records),
            "canonicalSha256": canonical_sha(records),
            "first": records[:5],
            "last": records[-2:],
        },
        "aggregates": data["aggregates"],
        "council": data["council"],
        "predictionSpecs": data["predictionSpecs"],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        "M2_WEB_GOLDEN_PASS "
        f"records={payload['sampleRecords']['count']} "
        f"aggregates={len(payload['aggregates'])} "
        f"council={len(payload['council'])} "
        f"predictions={len(payload['predictionSpecs'])} "
        f"sha256={source_sha}"
    )


if __name__ == "__main__":
    main()
