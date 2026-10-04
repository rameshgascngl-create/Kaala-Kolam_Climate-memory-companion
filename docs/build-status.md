# Native visual-fidelity build status

Branch: **native-v2**  
Milestone in progress: **M1 visual-fidelity reskin**  
Starting HEAD: **fc492c856b96a72b5784a3d4ed1b6f42146c87cd**

## Gate

No APK, AAB, tag or build from this branch is described as **release**, **final** or **authoritative** while any row below is **PARTIAL** or **PLACEHOLDER**.

Status meanings:

- **DONE** — implementation exists and its required golden screenshot comparison has passed.
- **PARTIAL** — implementation exists but visual parity is incomplete or the golden gate has not passed.
- **PLACEHOLDER** — the native workflow is not yet implemented.

| Screen / state | Status | Current evidence / next gate |
|---|---|---|
| Home | **PARTIAL** | Prototype palette/chrome, hero stripe, entry cards, kolam and baseline drawing ported. Golden comparison still required. |
| About | **PARTIAL** | Existing M1 behaviour re-skinned with prototype tokens. Golden comparison still required. |
| Learn home | **PARTIAL** | Existing native content scaffold remains; full prototype layout, controls and golden parity are M2 work. |
| Learn topic | **PLACEHOLDER** | Native topic-detail parity not implemented. |
| Learn game | **PLACEHOLDER** | Native game parity not implemented. |
| Learn word list | **PLACEHOLDER** | Native word-list parity not implemented. |
| Elders | **PARTIAL** | Existing content-gap scaffold only; interview workflow and prototype layout are not yet ported. |
| Class | **PLACEHOLDER** | Existing M1 placeholder remains. |
| Council | **PARTIAL** | Existing content-gap scaffold only; council workflow and prototype layout are not yet ported. |
| Predict | **PARTIAL** | Existing content-gap scaffold only; prediction/calibration workflow and prototype layout are not yet ported. |

## Visual-fidelity contract

- tools/extract_tokens.py is the sole generator for native visual tokens in Theme.kt.
- Material 3 is permitted for behaviour/accessibility primitives, not as the visual design source.
- Dark-theme prototype brand colours are fixed at:
  - turmeric **#F0B429**
  - vermilion **#E2573A**
  - sea **#34B3AB**
- The HTML prototype is the visual source of truth.
- Reference web captures must cover all six primary views plus Learn topic, game and word list at:
  - 390x844
  - 320x568
  - English and Tamil
  - dark and light
- JVM screenshot comparison must report a measured difference per state. A milestone is not complete until its scoped screens pass.

## Milestone order

1. **M1** — token extraction + prototype chrome + Home/About reskin.
2. **Golden-web foundation** — deterministic 72-image web reference matrix.
3. **M2+** — implement each remaining native workflow, adding its JVM golden state before marking it DONE.
4. Only when every row is DONE may release-signing/tagging be considered.
