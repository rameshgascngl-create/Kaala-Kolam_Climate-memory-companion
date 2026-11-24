# Architecture and risk
Preserve Kotlin/Compose, DataStore schema 3, legacy routes, historical visual baselines and tests. New V3 baselines only for intentional reviewed redesigns.
First implement pure Kotlin warning/freshness contract. Distinguish unavailable, invalid, future, stale, expired, active warning and explicit authoritative no-warning. Missing data, wrong geography, failed retrieval and expired bulletins never imply safe. Require authority, source, hazard, geography, issue and validity time; no warning is not safety guarantee. No fabricated forecasts or locally invented severity.
P0 risks: false reassurance, wrong district/time, unsafe translations. P1: loss of old state, stale science, synthetic data mixed with real, licensing, QA mistaken for release. Negative tests and review required.
Offline guidance independent of network. No GPS, accounts, secrets or transmission by default; Internet permission change requires owner approval.
