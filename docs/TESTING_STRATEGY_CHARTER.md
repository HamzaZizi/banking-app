# Testing Strategy Charter — `banking-app` SDLC

| | |
|---|---|
| **Status** | Draft v2.0 — scopes the compliance agent to genuinely judgment-based rules only; pending eng sign-off |
| **Owner** | Hamza Zizi (DevX) — reassign to QA/Eng lead once ratified |
| **Applies to** | `banking-app` repo and its `hamza_devx` Harness pipeline |
| **Last updated** | 2026-09-02 |
| **Review cadence** | Quarterly, or on any production incident traced to a test gap |

> This is the source of truth for "adequately tested" on `banking-app`. `Docs/QA_COMPLIANCE_AGENT.md` scores pipeline runs against it and produces a confidence number used as a promotion gate — it has no authority to invent rules, only to measure compliance with what's written here.
>
> **v2.0 change:** every rule is tagged **Deterministic** (a structural fact — exists/ran/passed/above-threshold — a tool can check with no interpretation) or **Judgment** (requires reading a test's intent against this charter; no tool can answer it). The agent's confidence score (§6) covers **only** Judgment rules. Deterministic rules are enforced by their own named policy/tool, so nothing is checked twice by mechanisms that could disagree.

---

## Table of Contents
1. [Purpose & Principles](#1-purpose--principles)
2. [Quality Bar — Anti-Pattern Register](#2-quality-bar--anti-pattern-register)
3. [Testing Layers L0–L9](#3-testing-layers-l0l9)
4. [Stage-to-Layer Map](#4-stage-to-layer-map)
5. [Promotion Gate Minimum Bars](#5-promotion-gate-minimum-bars)
6. [Confidence Scoring](#6-confidence-scoring)
7. [Engineer Definition of Done](#7-engineer-definition-of-done)
8. [Governance](#8-governance)
9. [Appendix — Semantic Bypass Glossary](#appendix--semantic-bypass-glossary)

---

## 1. Purpose & Principles

`banking-app` moves money; a suite that merely "passes" isn't evidence of safety. Three failure modes this charter exists to prevent:

1. **Coverage theater** — running code without asserting anything meaningful still counts as "coverage" in most tooling.
2. **Brittle assertions** — hardcoding a literal data value looks rigorous but gets deleted under deadline pressure the first time it breaks for an unrelated reason.
3. **Read-only blind spots** — `GET`-and-check-status tests are easier to write than write-and-verify tests, exactly backwards for a system whose job is mutating balances.

**Principles:**
- Shift left — cheaper layers gate earlier.
- Assert behavior and invariants, never a magic number tied to today's data.
- Every mutating operation needs a test that performs the mutation **and** verifies the side effect, not just a 200.
- A disabled control (`condition: "false"`) is scored **Missing**, not present — false confidence is worse than an absent control.
- **Codify what's mechanical, reserve judgment for what resists codification.** "Does it exist/pass/clear a threshold" belongs to tooling. "Does this assertion verify the right property, does a fixture quietly neutralize the control, does the expected value track spec or implementation" is where AI review earns its keep. §2 draws this line for smells; the layer tables in §3 apply it to every rule.

**Out of scope:** SAST, SCA, container scanning, DAST, rate-limiting, security headers — owned by a separate security policy gate. This charter covers test quality, functional correctness, and resilience only.

---

## 2. Quality Bar — Anti-Pattern Register

A test/check is non-compliant if it shows any pattern below, regardless of layer.

### 2.1 Mechanically Detectable — Tooling's Job
Answerable by a deterministic tool (AST rule, mutation testing, dup-detector, grep) with zero reading of intent. If one shows up in review it's a **tooling gap to close**, never a finding the agent should carry as a stopgap.

| Smell | Example | Detector |
|---|---|---|
| Tautological assertion | `assertEquals(x, x)` | Static rule; surfaces as surviving mutant |
| Assertion-free test | Runs, asserts nothing | AST: zero assert/expect calls |
| Swallowed exception | Empty `catch` block | AST: no-op catch |
| Skipped test counted as coverage | `@Disabled`, `.skip()` left indefinitely | Lint for stale skip annotations |
| Mock-count-only assertion | Only `verify(mock)` calls | AST + mutation testing on real collaborator |
| Unreviewed snapshot | `toMatchSnapshot()` auto-regenerated | Grep `.snap` for missing review gate |
| Disguised flakiness | `sleep`-based waits | Static rule + flaky-quarantine rerun |
| Copy-pasted duplicate tests | Same shape, renamed vars | Duplication detector (jscpd/PMD-CPD) |

### 2.2 Semantic Bypass — the Agent's Real Job
These execute fine and produce a plausible assertion — the defect is that it doesn't prove what it claims. This is the actual reason an AI reviewer belongs in the pipeline.

| Smell | Example | Why no tool catches it |
|---|---|---|
| Hardcoded golden value on mutable data | `.contains('"totalBalance":73936.09')` | Can't know if the literal is a real constant vs. a data snapshot without understanding the field |
| Status-only assertion on a mutating endpoint | `POST /transfer` asserts only `200` | Requires knowing the endpoint mutates state and no other test checks the effect — a cross-test judgment |
| Happy-path-only suite | No boundary/negative/authz case | "Is coverage adequate" is holistic, not countable |
| Wrong-property assertion | Asserts *an* exception was thrown, not the specific business rule | Assertion is real and passes — just checks a weaker property |
| Control-disabling fixture | `@BeforeEach` auto-approves, so a "fraud gets blocked" test passes with zero blocking logic | Defeat happens in setup, one level above the assertion |
| Implementation-pinned assertion | Expected value computed by calling the same prod code again | Identical structure to a correct test; only spec-vs-implementation intent tells them apart |

See [Appendix](#appendix--semantic-bypass-glossary) for repo-agnostic definitions. Any 2.1/2.2 smell scores non-compliant for that rule regardless of how healthy surrounding metrics look — the split only changes *who* is expected to catch it.

---

## 3. Testing Layers L0–L9

Each layer: Objective, where it runs in `hamza_devx`, and its rules with Classification (`Blocker`/`Required`/`Recommended`/`Optional`) and Enforcement (`Deterministic` = own policy/tool, excluded from agent score; `Judgment` = agent-scored). This charter defines rules only — point-in-time status is the agent's output, not this document's.

### L0 — Pre-Flight Change Review
*Catch unsafe/under-tested changes before compute is spent.* → `Harness_AI_PR_Review` → `Harness_PR_Reviewer`.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L0-01 | Recommended | Judgment (Agent) | Flag assertion-weakening changes for human attention, don't silently approve |

### L1 — Software Supply Chain Integrity
*Guarantee provenance of what's built/deployed (contents-scanning is out of scope, §1).* → `build_frontend`/`build_backend` → `Supply_Chain_*` stepGroups.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L1-01 | Required | Deterministic — `build_integrity` policy | SBOM generated + image signed before deploy |
| TSC-L1-02 | Recommended | Deterministic — tooling gap | SBOM diffed vs. previous build, not just archived unread |

*Zero Judgment rules — this layer doesn't contribute to the agent's score (§6.3).*

### L2 — Unit Tests
*Verify units in isolation with assertions strong enough to fail on real bugs.* → `Run_FrontEnd_Unit_Tests` (Jest), `Run_Backen_Unit_Tests` (Maven/JUnit).
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L2-01 | Required | Deterministic — CI status | Unit tests exist and pass every build |
| TSC-L2-02 | Required | Deterministic — artifact check | Coverage collected & reported (Jest/Jacoco) |
| TSC-L2-03 | Required | Deterministic — threshold gate | Minimum coverage % enforced |
| TSC-L2-04 | Blocker | Deterministic — **tooling gap**, no Stryker/PIT wired | Mutation testing with a kill-score gate — the real proxy for "assertions verify anything," since coverage % alone is gameable |
| TSC-L2-05 | Required | Deterministic — **tooling gap**, no AST/lint wired | No §2.1 mechanical smell in any test file |
| TSC-L2-06 | Required | **Judgment (Agent)** | Every money-handling function (interest/transfer/fees/rounding) has a boundary/edge-case test, not just happy path |

### L3 — Functional / API Integration Tests
*Verify the deployed service end-to-end, reads and writes, and that access control holds.* → `Deploy_Frontend_to_Dev` → `Integration_Testing` (`it_health`, `it_summary`, `it_accounts`, `it_transactions`, `it_negative_404`, `it_fraud_integration`).
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L3-01 | Required | Judgment | Read endpoints have status + body-content assertions |
| TSC-L3-02 | Blocker | Judgment | Every mutating endpoint has a write-then-reread test proving the side effect; status-only doesn't count |
| TSC-L3-03 | Blocker | Judgment | At least one negative-authz test (user B can't read/mutate user A's data) |
| TSC-L3-04 | Required | Judgment | Session/token lifecycle test (login → access → expiry/logout → denied) |
| TSC-L3-05 | Recommended | Judgment | Downstream dependency checks verify logic output, not just reachability |

*All five rules are Judgment — this is where the agent earns its keep, hence L3's 18% weight (§6.2).*

### L4 — Contract & Data Compatibility Tests
*Verify response shapes are stable without coupling to specific data values.* → `Deploy_to_SIT` → `Contract_and_Compatibility_Testing`.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L4-01 | Required | Judgment | Asserts on shape/keys present, not literal values |
| TSC-L4-02 | Blocker | Judgment | No hardcoded literal expected to change over the system's lifecycle (e.g. exact `totalBalance`) — must be a range/invariant/shape check |
| TSC-L4-03 | Recommended | Deterministic — tooling gap, no Pact-style tool | Consumer-driven contract test between directly-communicating services |

### L5 — Non-Functional: Performance & Load
*Verify latency/throughput under load without silent regression.* → `LoadTest` at SIT/Staging/Prod-canary.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L5-01 | Required | Deterministic — `performance_tests` policy | Load test runs at SIT, Staging, Prod-canary |
| TSC-L5-02 | Recommended | Deterministic — tooling gap, mocked today | Results compared to a stored baseline, not standalone pass/fail |

*Zero Judgment rules — doesn't contribute to the agent's score.*

### L6 — Resilience & Chaos
*Verify graceful degradation/recovery under infra failure.* → `Deploy_to_staging` → `Resilience_Gate`.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L6-01 | Required | Deterministic | At least one chaos experiment before prod promotion |
| TSC-L6-02 | Recommended | **Judgment** | Resilience score bar is deliberately justified & periodically reviewed, not arbitrary |
| TSC-L6-03 | Recommended | Deterministic | Fault types beyond pod-deletion (latency injection, dependency timeout, esp. fraud-check) |

### L7 — Progressive Delivery & Production Verification
*Verify a canary is operationally healthy AND functionally correct before full traffic.* → `Deploy_to_Prod` → `canaryDeployment` / `CV` / `LoadTest`.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L7-01 | Required | Deterministic — CV status | Statistical canary verification (error rate, latency anomaly) runs |
| TSC-L7-02 | Blocker | **Judgment** | A functional smoke check runs against the canary — metrics-only can pass while functionally broken |
| TSC-L7-03 | Recommended | **Judgment** | Business KPI comparison (txn success, fraud false-positive rate) vs. baseline |

### L8 — Data & Compliance Integrity
*Verify data/audit trail at rest — the layer HTTP-shaped testing structurally can't cover; highest-consequence for banking.* → post-deploy, independent of API layer.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L8-01 | Blocker | Deterministic — tooling gap, no invariant query wired | Ledger/DB conservation check (`SUM(debits)==SUM(credits)`, no orphans, no disallowed negative balances) via direct query |
| TSC-L8-02 | Blocker | **Judgment** | Audit-trail entry for a real transaction is correct & complete (who/when/amount/before-after) |

### L9 — End-to-End & Visual Verification
*Verify the real user journey through the real UI (dynamic security probing is out of scope, §1).* → browser automation vs. Dev/SIT.
| Rule | Class | Enforcement | Description |
|---|---|---|---|
| TSC-L9-01 | Recommended | **Judgment** | E2E journey (login → accounts → transfer → updated balance/history → logout) — adequacy of coverage is the real question |
| TSC-L9-02 | Optional | Deterministic | Visual regression (screenshot diffing) wired or not |
| TSC-L9-03 | Optional | Deterministic | Cross-browser/responsive checks wired or not |

**Tally:** 14 rules Judgment (agent-scored) / 15 Deterministic (policy/tool-owned, several currently tooling gaps). L1 and L5 have zero Judgment rules and don't contribute to the score at all; L3 is all-Judgment, which is why it's weighted heaviest among Judgment-bearing layers.

---

## 4. Stage-to-Layer Map

| Pipeline stage (`hamza_devx`) | Layers | Rule IDs |
|---|---|---|
| `Harness_AI_PR_Review` | L0 | TSC-L0-01 |
| `build_frontend` / `build_backend` | L1, L2 | TSC-L1-01..02, TSC-L2-01..06 |
| `Deploy_Frontend_to_Dev` (`Integration_Testing`) | L3 | TSC-L3-01..05 |
| `Deploy_to_SIT` (`Contract_and_Compatibility_Testing`) | L4, L5 | TSC-L4-01..03, TSC-L5-01..02 |
| `Deploy_to_staging` (`Resilience_Gate`) | L5, L6 | TSC-L5-01..02, TSC-L6-01..03 |
| `Deploy_to_Prod` (`canaryDeployment`/`CV`) | L5, L7 | TSC-L5-01, TSC-L7-01..03 |
| *(not yet present anywhere)* | L8, L9 | TSC-L8-01..02, TSC-L9-01..03 |

---

## 5. Promotion Gate Minimum Bars

Applies regardless of whether a rule is Deterministic or Judgment — §2/§3's split changes *who computes* status, not whether it's required.

| Promotion | Minimum bar |
|---|---|
| Merge to main | L0 review complete; L2 unit tests pass; no TSC-L2-05 smells in changed files |
| Dev → SIT | L2 Blocker/Required met; L3 read-path passes; TSC-L3-02 present & passing |
| SIT → Staging | L4 met incl. TSC-L4-02; L5 load test passes within tolerance |
| Staging → Prod | L6 chaos gate passes; TSC-L3-03 present & passing |
| Canary → 100% | L7 CV passes **and** TSC-L7-02 passes — CV alone insufficient |

---

## 6. Confidence Scoring

The agent must implement this literally, not invent its own weights/bands.

**Classification effect:** Blocker (Judgment) Missing/Disabled/Violated → score capped at **30** regardless of other layers. Required → 0 for that rule if missing. Recommended → deduction at reduced weight. Optional → presence is a small bonus, absence neutral. (Deterministic Blockers — TSC-L2-04, TSC-L8-01 — cap promotion through their own tool/policy, not through this formula.)

**Layer weights** (active layers only; rebalanced after removing security-scanning rules, shifted toward L2/L3/L6):
L1 5% · L2 18% · L3 18% · L4 8% · L5 7% · L6 12% · L7 14% · L8 15% · L9 3%. (L0 is pass/fail, not scored.)

```
for each layer L:
    J = Judgment-tagged rules in L
    layer_score(L) = N/A if J empty, else Σ(rule_met ? weight : 0)/Σweight over J   # 0-100

active_layers = layers where layer_score != N/A
renormalized_weight(L) = layer_weight(L) / Σ layer_weight(active_layers)
weighted_score = Σ layer_score(L) * renormalized_weight(L)

confidence_score = min(weighted_score, 30) if any Judgment Blocker Missing/Disabled/Violated else weighted_score
```

Deterministic rules (incl. Deterministic Blockers) are never inputs here — they gate promotion independently via their own tool/policy (§5). An unwired Deterministic rule is a **tooling gap**, tracked as such, never folded into the agent's score as a substitute.

**Bands:** 0–39 Non-Compliant (don't promote) · 40–69 Partially Compliant (needs sign-off + remediation plan) · 70–89 Substantially Compliant · 90–100 Fully Compliant (for the Judgment rules this score covers).

Bands/weights are a starting proposal pending ratification (§8). This score is deliberately not the full compliance picture — Deterministic rules matter equally and are gated separately.

---

## 7. Engineer Definition of Done

- [ ] New/modified money logic has a unit test satisfying TSC-L2-05 (and TSC-L2-06 where applicable).
- [ ] New/modified write endpoint has a write-then-verify integration test (TSC-L3-02), not a status-code check.
- [ ] New/modified endpoint touching another user's data has an authz test (TSC-L3-03).
- [ ] No test weakened/skipped/deleted without an explicit, reviewed justification in the PR — this is a substantive change, not a buried diff line.
- [ ] Any assertion on live/mutable data is a shape/range check, never a hardcoded literal (TSC-L4-02).

## 8. Governance

- **Ratification:** binding only after eng leadership sign-off; treat as a working proposal until then.
- **Ownership:** named QA/Eng lead keeps this in sync with the pipeline, including flipping a rule's Enforcement tag the moment tooling closes a gap — never leaving it parked on the agent.
- **Exceptions:** bypassing a Blocker/Required rule for a release requires a documented exception (what/why/expiry/compensating control) in the PR — silent non-compliance isn't acceptable.
- **Change process:** changes to classifications, weights, bands, or Enforcement tags need review by whoever owns the promotion gate consuming the score — moving a rule between Judgment and Deterministic is exactly this kind of change.
- **Next step:** `Docs/QA_COMPLIANCE_AGENT.md` reads this charter and scores actual pipeline evidence against it, Judgment rules only (v2.0). This document defines rules; it carries no point-in-time status itself.

---

## Appendix — Semantic Bypass Glossary

Repo-agnostic reference for §2.2, independent of any specific example:

1. **Wrong-property assertion** — asserts *that something happened* (exception thrown, value returned) but not the *specific* thing that matters. Real, passing, just weaker than required.
2. **Control-disabling fixture** — shared setup neutralizes the exact mechanism under test (auto-approving mock, always-granted flag) before the test body runs, so it passes whether or not the real control works.
3. **Implementation-pinned assertion** — expected value is derived from the same code path being tested rather than the spec, so a regression that changes behavior is invisible to the test.

Plus the six deterministic patterns in §2.1 — the full current smell register. A genuinely new smell defaults to Judgment until someone proves a deterministic tool can catch it; the burden is on moving something *out* of the agent's scope.
