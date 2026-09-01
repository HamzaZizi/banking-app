# Testing Strategy Charter — `banking-app` SDLC

| | |
|---|---|
| **Status** | Draft v1.0 — pending engineering sign-off |
| **Owner** | Hamza Zizi (DevX) — reassign to QA/Eng lead once ratified |
| **Applies to** | `banking-app` repository and its `hamza_devx` Harness pipeline |
| **Last updated** | 2026-09-01 |
| **Review cadence** | Quarterly, or on any new production incident traced to a test gap |

> **This document is the single source of truth for what "adequately tested" means for `banking-app`.**
> Every engineer — backend, frontend, and whoever writes pipeline YAML — is expected to satisfy the rules in this charter before code is considered mergeable and before a build is considered promotable. A separate automated agent (`Docs/QA_COMPLIANCE_AGENT.md`) scores actual pipeline executions against this charter and produces a confidence number used as a promotion gate. That agent has no authority to invent new rules — it only measures compliance with what's written here.

---

## Table of Contents

1. [Purpose & Guiding Principles](#1-purpose--guiding-principles)
2. [What Counts as a Real Test — the Quality Bar](#2-what-counts-as-a-real-test--the-quality-bar)
3. [Testing Layers (L0–L9)](#3-testing-layers-l0l9)
4. [Stage-to-Layer Compliance Map](#4-stage-to-layer-compliance-map)
5. [Promotion Gate Minimum Bars](#5-promotion-gate-minimum-bars)
6. [Confidence Scoring Framework](#6-confidence-scoring-framework)
7. [Engineer Responsibilities & Definition of Done](#7-engineer-responsibilities--definition-of-done)
8. [Governance](#8-governance)
9. [Appendix A — Rule ID Index](#appendix-a--rule-id-index)

---

## 1. Purpose & Guiding Principles

`banking-app` moves money. A test suite that merely "passes" is not sufficient evidence of safety — it must demonstrably exercise real behavior, real failure modes, and real data invariants. This charter exists to prevent three failure modes that this kind of system is especially prone to:

1. **Coverage theater** — a test that runs code without asserting anything meaningful about its output still counts as "coverage" in most tooling. Coverage % is a necessary signal, never a sufficient one.
2. **Brittle-instead-of-robust assertions** — hardcoding a literal data value (e.g. an exact dollar figure) into an assertion looks like a strong check and is actually a maintenance trap that gets deleted under deadline pressure the first time it breaks for an unrelated, legitimate reason.
3. **Read-only blind spots** — it is far easier to write a `GET`-and-check-status test than to write a test that performs a write and verifies the side effect. A pipeline that is stronger at reading than at writing has this precisely backwards for a system whose core function is mutating account balances.

**Guiding principles that every rule below derives from:**

- **Shift left, fail cheap.** A defect caught in a unit test costs less than the same defect caught in SIT, and immeasurably less than one caught in production. Layers are ordered so cheaper/faster checks gate earlier.
- **Assert behavior, not implementation, and never a magic number tied to a snapshot of today's data.**
- **Every mutating operation must have at least one test that performs the mutation and verifies its side effect** — not just that the endpoint returned 200.
- **A security or compliance gate that is wired but disabled is worse than one that doesn't exist**, because it creates false confidence. `condition: "false"` steps are treated as **Missing**, not as "present."
- **Deterministic checks are preferred over AI judgment wherever a deterministic check can do the job** (mutation testing over vibes, schema validation over an LLM guessing if a JSON shape looks right). AI-assisted review (Section 6, and the agent that will consume this charter) exists to catch what deterministic tooling structurally cannot — assertion *intent*, test *design* quality, and gaps that only make sense in context.

**Scope note — security scanning is intentionally out of this charter.** SAST, SCA/dependency scanning, container-image vulnerability scanning, DAST, rate-limiting/abuse protection, and security-header checks are governed (in theory) by a separate security policy gate outside this document. This charter is scoped to **test quality, functional correctness, and resilience** — whether the things we build actually work and keep working, not whether they're free of known vulnerabilities. The two concerns are complementary but are deliberately not conflated here.

---

## 2. What Counts as a Real Test — the Quality Bar

A test file, test case, or pipeline check is **non-compliant** with this charter if it exhibits any of the following, regardless of which layer it sits in. This is the shared "smell register" every rule in Section 3 refers back to.

| Smell | Example | Why it's banned |
|---|---|---|
| **Tautological assertion** | `assert True`, `assertEquals(x, x)` | Asserts nothing about behavior |
| **Assertion-free test** | Calls the function, checks nothing | Passes regardless of correctness; inflates coverage without adding safety |
| **Swallowed exception** | `try { ... } catch (Exception e) {}` | Hides real failures; the test can never fail even when the code is broken |
| **Silently skipped/disabled test counted as coverage** | `@Disabled`, `xit(...)`, `.skip()` left in the suite indefinitely | Creates false confidence that behavior is verified when it is not |
| **Hardcoded golden-value assertion on live/mutable data** | `.contains('"totalBalance":73936.09')` | Breaks on any legitimate data change; encourages deletion instead of fixing |
| **Status-code-only assertion on a mutating endpoint** | `POST /transfer` asserted only on `200`, balance never re-checked | Verifies the server responded, not that the operation happened correctly |
| **Single happy-path-only suite** | Only the success case is tested; no boundary, no negative, no auth-denial case | Real defects live at the edges, not in the middle of the happy path |
| **Over-mocked test** | Every collaborator mocked, assertion only on mock call count | Tests that the code called a method, not that the method's effect was correct |
| **Snapshot test with no reviewed diff policy** | `toMatchSnapshot()` regenerated on CI without human review | A snapshot that's always "updated to match" can never fail |
| **Flaky/non-deterministic test disguised as passing** | Sleep-based waits, time-dependent assertions | Erodes trust in the pass rate itself; teams learn to ignore "known flaky" failures |
| **Copy-pasted near-duplicate tests** | Ten tests that differ only in variable names, same assertion shape | Inflates test count without adding real coverage |

Any pipeline step or test file exhibiting one of these is scored as **non-compliant for that rule**, even if the surrounding metric (pass rate, coverage %, file count) looks healthy.

---

## 3. Testing Layers (L0–L9)

Each layer states: **Objective**, **Where this applies** (mapped to the relevant `hamza_devx` stage/step identifiers), **Rules**, and **Classification** (`Blocker` / `Required` / `Recommended` / `Optional` — defined in Section 6). This charter defines what must be true. It intentionally carries no compliance status — whether a rule is currently Met, Missing, Disabled, or Violated is a fact about a specific pipeline execution at a point in time, not about the charter. Producing and reporting that status is the job of the compliance agent (`Docs/QA_COMPLIANCE_AGENT.md`), which reads this charter and the actual, current pipeline/repository state each time it runs.

### L0 — Pre-Flight Change Review
**Objective:** Catch structurally unsafe or under-tested changes before any compute is spent building or deploying.
**Where this applies:** `Harness_AI_PR_Review` stage → `AI_PR_Review` stepGroup → `Harness_PR_Reviewer` (Agent step).
**Rules:**
- `TSC-L0-01` (Recommended): A change that modifies or removes test assertions must be flagged for explicit human attention before merge, not silently approved.
**Classification:** Recommended.

### L1 — Software Supply Chain Integrity
**Objective:** Guarantee provenance and integrity of what gets built and deployed — that the artifact promoted through the pipeline is the exact, traceable thing it claims to be. (Security scanning of that artifact's contents — SAST, SCA, container scanning — is out of scope for this charter; see the Section 1 scope note.)
**Where this applies:** `build_frontend` and `build_backend` stages → `Supply_Chain_Frontend`/`Supply_Chain_Backend` stepGroups (SBOM generation + keyless signing). *(Note: any static/security-scanning steps present in these stages — e.g. SAST, SCA, container scanning — are out of scope for this charter regardless of whether they are enabled; they belong to a separate security policy gate, not to this testing strategy.)*
**Rules:**
- `TSC-L1-01` (Required): Every built image must have an SBOM generated and be signed (keyless or otherwise) before deployment.
- `TSC-L1-02` (Recommended): SBOM contents should be diffed against the previous build so newly introduced or changed dependencies are surfaced for review, not just generated and archived unread.
**Classification:** Required for `TSC-L1-01`; Recommended for `TSC-L1-02`.

### L2 — Unit Tests
**Objective:** Verify individual units of business logic in isolation, fast and cheap, with assertions strong enough to fail when logic is actually wrong.
**Where this applies:** `build_frontend` → `Run_FrontEnd_Unit_Tests` (Jest, JUnit-format report); `build_backend` → `Run_Backen_Unit_Tests` (Maven/JUnit, Surefire reports).
**Rules:**
- `TSC-L2-01` (Required): Unit tests must exist and execute successfully on every build for both frontend and backend.
- `TSC-L2-02` (Required): Code coverage must be collected (Jest `--coverage`, Jacoco for Maven) and reported as a build artifact, even before a threshold is enforced.
- `TSC-L2-03` (Required): A minimum coverage threshold must be enforced as a build gate once `TSC-L2-02` is in place.
- `TSC-L2-04` (Blocker): Mutation testing must run (Stryker for frontend, PIT for backend) with a minimum mutation-kill-score gate. This is the primary deterministic proxy for "do these assertions actually verify anything," and is treated as a Blocker because coverage % alone is demonstrably gameable (Section 2).
- `TSC-L2-05` (Required): No test file may contain a Section-2 smell (tautological assert, assertion-free test, swallowed exception, silently-skipped test).
- `TSC-L2-06` (Required): Every business-logic function handling money (interest, transfers, fees, rounding) must have at least one boundary/edge-case test (zero, negative, max-value, currency mismatch) in addition to the happy path.
**Classification:** Blocker for `TSC-L2-04`; Required for the rest.

### L3 — Functional / API Integration Tests
**Objective:** Verify the deployed service behaves correctly end-to-end at the API layer, covering both reads and writes, and that access control actually holds.
**Where this applies:** `Deploy_Frontend_to_Dev` stage → `Integration_Testing` stepGroup (`it_health`, `it_summary`, `it_accounts`, `it_transactions`, `it_negative_404`, `it_fraud_integration`).
**Rules:**
- `TSC-L3-01` (Required): Read endpoints (health, summary, accounts, transactions) must have status + body-content assertions.
- `TSC-L3-02` (Blocker): Every mutating endpoint (e.g. transfer, account creation) must have an integration test that performs the write **and** re-reads state to confirm the side effect occurred (balances moved by the correct amount, a new record exists, etc.). Status-code-only assertions on a mutating endpoint do not satisfy this rule.
- `TSC-L3-03` (Blocker): At least one negative authorization test must exist proving a user cannot read or mutate another user's account (e.g. user B requesting user A's account data receives a `403`/`401`, not the data).
- `TSC-L3-04` (Required): An authenticated session/token lifecycle test must exist (login → access protected resource → expiry or logout → subsequent access denied).
- `TSC-L3-05` (Recommended): Downstream dependency integration checks (e.g. `it_fraud_integration`) must verify the dependency's *logic output* is sane for at least one non-trivial input, not merely that it is reachable (`"integration":"ok"` alone is insufficient).
**Classification:** Blocker for `TSC-L3-02/03`; Required for `TSC-L3-01/04`; Recommended for `TSC-L3-05`.

### L4 — Contract & Data Compatibility Tests
**Objective:** Verify API response shapes are stable across services/versions without coupling tests to specific data values.
**Where this applies:** `Deploy_to_SIT` stage → `Contract_and_Compatibility_Testing` stepGroup (`sit_data_integrity_balance`, `sit_data_integrity_mortgage`, `sit_contract_summary`, `sit_contract_accounts`, `sit_negative_404`).
**Rules:**
- `TSC-L4-01` (Required): Contract tests must assert on response **shape/keys present** (e.g. checking for `accountCount`, `totalBalance`, `mortgageCount`, `totalMortgageOutstanding` keys) rather than on literal values.
- `TSC-L4-02` (Blocker): No contract or "data integrity" test may hardcode a literal data value that is expected to change over the system's normal lifecycle (e.g. asserting an exact `"totalBalance": 73936.09`). Such assertions must be rewritten as a range/tolerance check, a computed-invariant check (see `TSC-L8-01`), or a shape check.
- `TSC-L4-03` (Recommended): Where two services communicate directly (frontend↔backend, backend↔fraud-check), a consumer-driven contract test (e.g. Pact) should verify the interaction contract independent of the running environment's data state.
**Classification:** Blocker for `TSC-L4-02`; Required for `TSC-L4-01`; Recommended for `TSC-L4-03`.

### L5 — Non-Functional: Performance & Load
**Objective:** Verify the system meets latency/throughput expectations under realistic load, and that performance doesn't silently regress release over release.
**Where this applies:** `Deploy_to_SIT` (`LoadTest`), `Deploy_to_staging` → `Resilience_Gate` (`Staging_LoadTest`), `Deploy_to_Prod` → `CV` stepGroup (`LoadTest`).
**Rules:**
- `TSC-L5-01` (Required): A load test must run at SIT, Staging, and Prod-canary stages.
- `TSC-L5-02` (Recommended): Load test results should be compared against a stored baseline/trend, not evaluated as a standalone pass/fail with no regression context.
**Classification:** Required for `TSC-L5-01`; Recommended for `TSC-L5-02`.

### L6 — Resilience & Chaos
**Objective:** Verify the system degrades gracefully and recovers when infrastructure fails, not just when the application logic is exercised normally.
**Where this applies:** `Deploy_to_staging` → `Resilience_Gate` stepGroup (Chaos experiment(s) against the running deployment).
**Rules:**
- `TSC-L6-01` (Required): At least one chaos experiment must run before production promotion.
- `TSC-L6-02` (Recommended): The resilience score bar used to gate a chaos experiment should be deliberately justified and periodically reviewed, not left at an arbitrary or default value.
- `TSC-L6-03` (Recommended): Fault types beyond pod deletion (network latency injection, dependency timeout/unavailability — especially to downstream services like fraud-check) should be added to the resilience suite.
**Classification:** Required for `TSC-L6-01`; Recommended for `TSC-L6-02/03`.

### L7 — Progressive Delivery & Production Verification
**Objective:** Verify a production canary is both operationally healthy and functionally correct before it receives full traffic.
**Where this applies:** `Deploy_to_Prod` stage → `canaryDeployment` stepGroup → `CV` stepGroup (Continuous Verification against metrics) + `LoadTest`.
**Rules:**
- `TSC-L7-01` (Required): Statistical/metrics-based canary verification (error rate, latency anomaly detection) must run during every production canary.
- `TSC-L7-02` (Blocker): A functional smoke check (equivalent to the Dev/SIT read-path pattern) must run against the canary and/or immediately post-full-rollout in Production. Metrics-only verification is not sufficient — a functionally broken build can clear every metrics gate as long as latency and error-rate stay within bounds.
- `TSC-L7-03` (Recommended): Business KPI comparison (transaction success rate, fraud false-positive rate) between the canary slice and baseline should run alongside the infrastructure-metric CV.
**Classification:** Blocker for `TSC-L7-02`; Required for `TSC-L7-01`; Recommended for `TSC-L7-03`.

### L8 — Data & Compliance Integrity
**Objective:** Verify the system's data and audit trail are correct at rest, independent of what any API response claims — this is the layer HTTP-shaped testing structurally cannot cover, and it is the highest-consequence layer for a banking domain.
**Where this applies:** Post-deployment verification, independent of any API/HTTP-layer test.
**Rules:**
- `TSC-L8-01` (Blocker): A ledger/database invariant check must run post-deploy verifying fundamental conservation properties (e.g. `SUM(debits) == SUM(credits)`, no orphaned transaction records, no negative balance where the product does not permit overdraft) via direct query, independent of the API layer.
- `TSC-L8-02` (Blocker): An audit-trail verification test must run confirming that a real transaction (e.g. the `TSC-L3-02` mutating test) produces a correct, complete audit/compliance log entry (who, when, amount, before/after balance).
**Classification:** Blocker for both rules.

### L9 — End-to-End & Visual Verification
**Objective:** Verify the system as a real user actually experiences it — through the real UI, across a full journey — which is a class of defect API-level and unit tests structurally cannot see. (Dynamic security probing, rate-limiting, security headers, and TLS/certificate checks are out of scope for this charter; see the Section 1 scope note.)
**Where this applies:** Frontend, via browser automation against Dev or SIT.
**Rules:**
- `TSC-L9-01` (Recommended): At least one end-to-end browser test (Playwright/Cypress) covering a full user journey (login → view accounts → transfer → view updated balance/history → logout) should run against Dev or SIT.
- `TSC-L9-02` (Optional): Visual regression testing (screenshot diffing) for the frontend, to catch unintended UI/layout breakage that functional assertions won't detect.
- `TSC-L9-03` (Optional): Cross-browser/responsive-layout checks for the critical user journeys, given this is a customer-facing banking UI.
**Classification:** Recommended for `TSC-L9-01`; Optional for `TSC-L9-02/03`.

---

## 4. Stage-to-Layer Compliance Map

This is the literal checklist an automated agent (or a human auditor) should walk through, stage by stage, against the actual pipeline.

| Pipeline stage (`hamza_devx`) | Layers in scope | Rule IDs to evaluate |
|---|---|---|
| `Harness_AI_PR_Review` | L0 | `TSC-L0-01` |
| `build_frontend` | L1, L2 | `TSC-L1-01..02`, `TSC-L2-01..06` (frontend subset) |
| `build_backend` | L1, L2 | `TSC-L1-01..02`, `TSC-L2-01..06` (backend subset) |
| `Deploy_Frontend_to_Dev` (`Integration_Testing`) | L3 | `TSC-L3-01..05` |
| `Deploy_to_SIT` (`Contract_and_Compatibility_Testing`) | L4, L5 | `TSC-L4-01..03`, `TSC-L5-01..02` |
| `Deploy_to_staging` (`Resilience_Gate`) | L5, L6 | `TSC-L5-01..02`, `TSC-L6-01..03` |
| `Deploy_to_Prod` (`canaryDeployment` / `CV`) | L5, L7 | `TSC-L5-01`, `TSC-L7-01..03` |
| *(not yet present anywhere)* | L8, L9 | `TSC-L8-01..02`, `TSC-L9-01..03` |

---

## 5. Promotion Gate Minimum Bars

A build must not be promoted past the named environment unless the stated bar is met. These are the enforceable thresholds this charter expects the pipeline (or the compliance agent, acting as a gate) to check.

| Promotion | Minimum bar |
|---|---|
| **Merge to main** | L0 review complete; L2 unit tests pass; no `TSC-L2-05` smell violations in changed test files |
| **Dev → SIT** | All L2 Blocker/Required rules met; L3 read-path tests pass; `TSC-L3-02` (mutating-path test) present and passing |
| **SIT → Staging** | All L4 rules met including `TSC-L4-02` (no hardcoded golden values); L5 load test passes without regression beyond agreed tolerance |
| **Staging → Prod** | L6 chaos gate passes; `TSC-L3-03` (authz negative test) present and passing |
| **Canary → 100% Prod traffic** | L7 statistical CV passes **and** `TSC-L7-02` (functional smoke check) passes — CV alone is not sufficient |

---

## 6. Confidence Scoring Framework

This section is the contract the compliance agent must implement literally — it should not invent its own weights or bands.

### 6.1 Rule classification and weight

| Classification | Meaning | Effect on score |
|---|---|---|
| **Blocker** | Absence/violation makes the release unsafe regardless of anything else | If **any** Blocker rule is Missing/Disabled/Violated, the overall confidence score is capped at **30**, regardless of how well every other layer scores |
| **Required** | Expected to be met for a compliant release; absence is a significant deduction but not an automatic cap | Contributes to the weighted layer score; a Missing Required rule scores 0 for that rule |
| **Recommended** | Strengthens confidence; absence is a deduction, not disqualifying | Contributes at reduced weight |
| **Optional** | Nice to have; absence never penalizes | Bonus only — presence adds a small positive adjustment, absence is neutral |

### 6.2 Layer weights (used only when no Blocker cap is in effect)

| Layer | Weight |
|---|---|
| L1 — Software Supply Chain Integrity | 5% |
| L2 — Unit Tests | 18% |
| L3 — Functional/API Integration | 18% |
| L4 — Contract & Compatibility | 8% |
| L5 — Performance/Load | 7% |
| L6 — Resilience/Chaos | 12% |
| L7 — Production Verification | 14% |
| L8 — Data & Compliance | 15% |
| L9 — E2E & Visual Verification | 3% |

*(Weights were rebalanced when security-scanning rules were removed from L1 and L9 — see Section 1 scope note. Weight shifted toward the layers that are directly about test/QA quality and resilience: L2, L3, and L6.)*

*(L0 is process, not scored numerically; it is reported as a pass/fail note.)*

### 6.3 Formula (for the compliance agent to implement exactly)

```
for each layer L:
    layer_score(L) = Σ (rule_met ? rule_weight_within_layer : 0) / Σ rule_weight_within_layer   # 0–100

weighted_score = Σ layer_score(L) * layer_weight(L)   # 0–100

if any Blocker-classified rule is Missing / Disabled / Violated:
    confidence_score = min(weighted_score, 30)
else:
    confidence_score = weighted_score
```

### 6.4 Confidence bands

| Score | Band | Meaning |
|---|---|---|
| 0–39 | **Non-Compliant** | Do not promote. One or more Blocker gaps, or systemic Required-rule absence |
| 40–69 | **Partially Compliant** | Promotable only with explicit sign-off and a tracked remediation plan |
| 70–89 | **Substantially Compliant** | Promotable; remaining gaps are Recommended/Optional |
| 90–100 | **Fully Compliant** | Meets the charter in full |

These bands and weights are a starting proposal — ratify or adjust them with engineering leadership before the scoring agent is wired into an actual promotion gate (Section 8).

---

## 7. Engineer Responsibilities & Definition of Done

Every engineer touching `banking-app` is responsible for the following before requesting review or merge:

- [ ] Any new or modified business logic (especially anything touching money — interest, transfers, fees) has a unit test satisfying `TSC-L2-05` and, where applicable, `TSC-L2-06`.
- [ ] Any new or modified API endpoint that **writes** data has an integration test satisfying `TSC-L3-02` — a write-then-verify test, not a status-code check.
- [ ] Any new or modified endpoint touching another user's data has an authorization test satisfying `TSC-L3-03`.
- [ ] No test was weakened, skipped, or deleted without an explicit, reviewed justification in the PR description — deleting or loosening an assertion is a **substantive change** and must be called out, not buried in an unrelated diff.
- [ ] Any assertion added against a live/mutable data value is a shape/range check, not a hardcoded literal (`TSC-L4-02`).

## 8. Governance

- **Ratification:** This draft requires sign-off from engineering leadership before it is treated as binding. Until ratified, it should be socialized as a working proposal.
- **Ownership:** A named owner (QA/Eng lead) is responsible for keeping this document in sync with the pipeline as stages are added/changed.
- **Exceptions:** Any team wishing to bypass a Blocker or Required rule for a specific release must document the exception (what, why, expiry date, compensating control) in the PR/change record — silent non-compliance is not an acceptable path.
- **Change process:** Changes to rule classifications, weights, or bands require review by whoever owns the promotion gate that consumes the resulting confidence score, since those numbers have operational teeth once the scoring agent is live.
- **Next step:** The compliance agent that reads this charter and assesses actual pipeline execution evidence against it lives separately in `Docs/QA_COMPLIANCE_AGENT.md`. This charter defines the rules only; it deliberately carries no point-in-time compliance status — that is the agent's output, produced fresh against whatever pipeline execution it is pointed at.

---

## Appendix A — Rule ID Index

| Rule ID | Layer | Classification | Short description |
|---|---|---|---|
| TSC-L0-01 | L0 | Recommended | Flag assertion-weakening changes for human review |
| TSC-L1-01 | L1 | Required | SBOM + signing present |
| TSC-L1-02 | L1 | Recommended | SBOM diffed against previous build for review |
| TSC-L2-01 | L2 | Required | Unit tests exist and run |
| TSC-L2-02 | L2 | Required | Coverage collected |
| TSC-L2-03 | L2 | Required | Coverage threshold enforced |
| TSC-L2-04 | L2 | Blocker | Mutation testing enforced |
| TSC-L2-05 | L2 | Required | No Section-2 smells in test files |
| TSC-L2-06 | L2 | Required | Boundary/edge-case tests on money logic |
| TSC-L3-01 | L3 | Required | Read-endpoint integration coverage |
| TSC-L3-02 | L3 | Blocker | Mutating-endpoint write+verify test |
| TSC-L3-03 | L3 | Blocker | Negative authorization test |
| TSC-L3-04 | L3 | Required | Session/token lifecycle test |
| TSC-L3-05 | L3 | Recommended | Downstream dependency logic-output check |
| TSC-L4-01 | L4 | Required | Shape/schema contract checks |
| TSC-L4-02 | L4 | Blocker | No hardcoded golden-value assertions |
| TSC-L4-03 | L4 | Recommended | Consumer-driven contract tests |
| TSC-L5-01 | L5 | Required | Load test present per stage |
| TSC-L5-02 | L5 | Recommended | Baseline/regression comparison |
| TSC-L6-01 | L6 | Required | Chaos experiment present |
| TSC-L6-02 | L6 | Recommended | Resilience bar justified |
| TSC-L6-03 | L6 | Recommended | Fault types beyond pod-delete |
| TSC-L7-01 | L7 | Required | Statistical canary verification |
| TSC-L7-02 | L7 | Blocker | Functional smoke check in Prod |
| TSC-L7-03 | L7 | Recommended | Business KPI canary comparison |
| TSC-L8-01 | L8 | Blocker | Ledger/DB invariant check |
| TSC-L8-02 | L8 | Blocker | Audit-trail verification |
| TSC-L9-01 | L9 | Recommended | E2E browser journey test |
| TSC-L9-02 | L9 | Optional | Visual regression testing |
| TSC-L9-03 | L9 | Optional | Cross-browser/responsive layout check |
