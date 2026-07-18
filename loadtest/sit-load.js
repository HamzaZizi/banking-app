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
//  • Per-endpoint tagging + custom Trend metrics feed the RESULTS breakdown
//    (Endpoint Statistics / per-journey timings) — great for the demo view.
//
// IMPORTANT — WHAT HARNESS ACTUALLY GATES ON
// Harness's k6 gate only evaluates STANDARD k6 metrics. Custom metrics
// (Trend/Rate) and tag-scoped sub-metric thresholds like
// http_req_duration{name:summary} are NOT collected — they show "N/E"
// (Not Evaluated) and do NOT gate. (Confirmed empirically: run #1 evaluated
// only the 4 standard-metric thresholds; the 6 custom/tagged ones were N/E.
// Harness's native threshold model has no field for tags or custom metrics.)
// So the `thresholds` block below intentionally gates ONLY on standard
// metrics. The tags and custom Trend/Rate metrics are kept for DISPLAY, not
// gating — they enrich the results UI without pretending to be live gates.
// ============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Harness's "Host URL" field rejects a port, so it passes the origin only
// (e.g. http://sit-banking-app-backend.banking-app-sit.svc.cluster.local). The
// backend listens on 8080, so if HOST_URL has no explicit port we append :8080.
const RAW = (__ENV.HOST_URL || 'http://localhost:8080').replace(/\/+$/, '');
const HOST = /:\d+$/.test(RAW) ? RAW : `${RAW}:8080`;

// Real SIT seed identifiers (from BankingService.java) — using fake IDs would
// 404 and falsely trip the gate.
const ACCOUNTS = ['acc-001', 'acc-002', 'acc-003'];
const MORTGAGES = ['mtg-001', 'mtg-002'];

// ---- Custom metrics (DISPLAY ONLY — Harness does not gate on these) -------
// Trends give p95/p99/avg/max per business journey in the results view,
// independent of the built-in http_req_duration (which aggregates everything).
const browseDuration = new Trend('journey_browse_duration', true);
const fraudDuration = new Trend('journey_fraud_duration', true);
// A functional error = a 200 wasn't returned or the body was empty. Surfaced
// in the results separately from http_req_failed (transport-level).
const functionalErrors = new Rate('functional_errors');

function get(path, name) {
  // `name` tag groups identical URLs (e.g. per-account) into one row in the
  // results' Endpoint Statistics — display only (Harness doesn't gate on tags).
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
  //
  // Gates ONLY on standard k6 metrics — the ones Harness actually evaluates
  // (see the header note). Custom/tagged thresholds were dropped because they
  // return N/E and would be dead gates. Every threshold here truly runs.
  //
  // Numbers are TUNED to the observed in-cluster baseline (run #1, 315k reqs):
  //   overall p95 ~3ms, p99 ~597ms (a small cold-start/GC tail), 0% errors.
  // p95 sits ~15x above the baseline — tight enough to catch a real regression
  // (a dependency bump that adds tens of ms), loose enough to absorb jitter.
  // p99 is kept generous so the startup tail doesn't flake a healthy deploy.
  // Retune if the baseline shifts materially.
  thresholds: {
    http_req_duration: ['p(95)<50', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],   // <1% transport-level failures
    checks: ['rate>0.99'],            // >99% of status/body checks pass
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
