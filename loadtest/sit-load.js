// ============================================================================
// SIT PERFORMANCE GATE — Harness Resilience Testing › Load Testing (K6)
// Mode: "Upload K6 script".  Host URL arrives as __ENV.HOST_URL, e.g.
//   http://sit-banking-app-backend.banking-app-sit.svc.cluster.local:8080
//
// WHY THIS SCRIPT IS THE GATE
// Per Harness docs, a high error rate ALONE does not fail a run — only a
// breached k6 `threshold` marks the run Failed. A Failed run fails the SIT
// stage, which blocks promotion. So the `thresholds` block below IS the
// release gate; everything else exists to feed it meaningful signal.
//
// WHAT THIS DEMONSTRATES (the demo talking points)
//  • Two PARALLEL scenarios modelling real user journeys (the docs' "browse
//    flow plus a checkout flow" pattern): a customer browsing accounts, and
//    the fraud-check integration path being hammered independently.
//  • Per-endpoint tagging so the results table breaks latency down by route.
//  • Custom Trend metrics so we can gate the business-critical journeys
//    tighter than the overall aggregate.
//  • A layered threshold table: global SLOs + per-journey + per-endpoint.
// ============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const HOST = __ENV.HOST_URL || 'http://localhost:8080';

// Real SIT seed identifiers (from BankingService.java) — using fake IDs would
// 404 and falsely trip the gate.
const ACCOUNTS = ['acc-001', 'acc-002', 'acc-003'];
const MORTGAGES = ['mtg-001', 'mtg-002'];

// ---- Custom metrics -------------------------------------------------------
// Trends give us p95/p99/avg/max per business journey, independent of the
// built-in http_req_duration (which aggregates every request together).
const browseDuration = new Trend('journey_browse_duration', true);
const fraudDuration = new Trend('journey_fraud_duration', true);
// A functional error = a 200 wasn't returned or the body was empty. Kept
// separate from http_req_failed (transport-level) so we can gate on both.
const functionalErrors = new Rate('functional_errors');

function get(path, name) {
  // `name` tag groups identical URLs (e.g. per-account) into one results row
  // and lets us write per-endpoint thresholds below.
  const res = http.get(`${HOST}${path}`, { tags: { name } });
  const ok = check(res, {
    'status is 200': (r) => r.status === 200,
    'body is non-empty': (r) => r.body && r.body.length > 0,
  });
  functionalErrors.add(!ok);
  return res;
}

// ============================================================================
// SCENARIOS — two workloads run in PARALLEL (docs: "Add more than one scenario
// to run workloads in parallel"). Each has its own ramping-vus schedule.
// ============================================================================
export const options = {
  scenarios: {
    // Journey 1: a customer opening the app and browsing their finances.
    // Steady, moderate load — the everyday case.
    customer_browse: {
      executor: 'ramping-vus',
      exec: 'browseJourney',
      startVUs: 0,
      gracefulStop: '10s',
      stages: [
        { duration: '30s', target: 15 }, // ramp up
        { duration: '2m', target: 15 },  // steady state — the judged window
        { duration: '20s', target: 0 },  // ramp down
      ],
    },

    // Journey 2: the fraud-check integration path, driven harder and starting
    // slightly later so it overlaps the browse steady-state. This is the
    // cross-service call — the most likely thing to degrade under a bad deploy.
    fraud_check_pressure: {
      executor: 'ramping-vus',
      exec: 'fraudJourney',
      startVUs: 0,
      startTime: '20s',
      gracefulStop: '10s',
      stages: [
        { duration: '30s', target: 25 },
        { duration: '90s', target: 25 },
        { duration: '20s', target: 0 },
      ],
    },
  },

  // ---- THE GATE ----------------------------------------------------------
  // Any breach => run Failed => SIT stage fails => no promotion.
  // Layered so a regression is caught at the tightest relevant level.
  thresholds: {
    // Global SLOs across all traffic.
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    http_req_failed: ['rate<0.01'],       // <1% transport failures
    functional_errors: ['rate<0.01'],     // <1% non-200 / empty bodies
    checks: ['rate>0.99'],                // >99% of checks pass

    // Per-journey SLOs — browse is the everyday path, held tight.
    journey_browse_duration: ['p(95)<600'],
    // Fraud path crosses a service boundary, so a slightly looser bound.
    journey_fraud_duration: ['p(95)<1000'],

    // Per-endpoint SLOs via the `name` tag — pinpoints WHICH route regressed.
    'http_req_duration{name:summary}': ['p(95)<500'],
    'http_req_duration{name:accounts_list}': ['p(95)<500'],
    'http_req_duration{name:fraud_check}': ['p(95)<1000'],
  },
};

// ---- Default function -----------------------------------------------------
// Harness requires a literal `export default function`. The parallel scenarios
// above drive load via their `exec` fields (browseJourney / fraudJourney), so
// this default is not used by them — but it keeps the script valid for the
// uploader and makes `k6 run loadtest/sit-load.js` work standalone (no
// scenarios) by exercising both journeys back-to-back.
export default function () {
  browseJourney();
  fraudJourney();
}

// ---- Journey 1: browse ----------------------------------------------------
export function browseJourney() {
  group('browse', () => {
    const start = Date.now();
    get('/api/summary', 'summary');
    get('/api/accounts', 'accounts_list');

    // Drill into one account's detail, balance, and transactions — a realistic
    // click-through rather than hammering a single URL.
    const acc = ACCOUNTS[Math.floor(Math.random() * ACCOUNTS.length)];
    get(`/api/accounts/${acc}`, 'account_detail');
    get(`/api/accounts/${acc}/balance`, 'account_balance');
    get(`/api/accounts/${acc}/transactions`, 'account_transactions');

    get('/api/mortgages', 'mortgages_list');
    const mtg = MORTGAGES[Math.floor(Math.random() * MORTGAGES.length)];
    get(`/api/mortgages/${mtg}`, 'mortgage_detail');

    browseDuration.add(Date.now() - start);
  });
  sleep(1);
}

// ---- Journey 2: fraud-check integration ----------------------------------
export function fraudJourney() {
  group('fraud', () => {
    const start = Date.now();
    get('/api/fraud-check', 'fraud_check');
    fraudDuration.add(Date.now() - start);
  });
  sleep(0.5);
}
