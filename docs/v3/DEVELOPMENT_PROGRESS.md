# Progress
2026-11-24: baseline e172d954ccc782e438c4f5a90096cb6346eed997; all eight jobs passed at run 37650650717. Separate V3 branch created. Initial registers added; full scientific audit remains open. No V3 runtime changes or CI executed yet. Next: exact-head branch CI and offline warning contract.

2026-11-24: V3 first CI run 45369440424 at 3b6aca8: Compile PASS; reference capture failed before tests because Playwright 1.55.0 does not support ubuntu26.04-x64, downstream gates blocked. Pin Android workflow runners to ubuntu-24.04; no assertion or browser version changes. Offline citizen Today/Safety screens committed 0135dfe; subsequent navigation/test scroll hardening included with CI environment correction. Exact-head QA pending. Existing native-v2/main unchanged.
