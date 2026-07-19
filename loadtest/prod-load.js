// ============================================================================
// PROD CV TRAFFIC GENERATOR — Harness Resilience Testing › Load Testing (K6)
// Mode: "Upload K6 script".  Host URL arrives as __ENV.HOST_URL, e.g.
//   http://prod-banking-app-backend.banking-app-prod.svc.cluster.local:8080
//
// WHAT THIS SCRIPT IS FOR (and how it differs from sit-load.js)
// In Prod this runs IN PARALLEL with a Continuous Verification (Verify) step
// during the canary window. Its job is NOT to gate — it is to put sustained,
// realistic traffic on the Service so the canary pod does real work and any
// regression (e.g. a per-request memory leak) actually manifests in the
// metrics CV is watching (heap / GC / working-set memory / restarts).
//
// >>> CV IS THE GATE, NOT THIS SCRIPT. <<<
// We intentionally ship NO k6 `thresholds` here. A k6 threshold breach would
// mark the run Failed and fail the stage from the load test itself — which
// would steal the verdict from CV (and abortOnFail would cut traffic short,
// starving CV of signal right when it matters). By removing thresholds the
// LoadTest step always passes; the Verify (CV) step is the only thing that
// decides pass/fail. If you ever want a k6 backstop, add non-aborting,
// display-only thresholds — never abortOnFail in the CV window.
//
// COVERAGE — the steady-state MUST outlast the Verify step's WALL TIME, not
// just its configured 5-min analysis duration. In practice the Verify step
// takes ~8.5 min wall time for a 5-min duration: it spends ~2-3 min on
// pods-ready + first-scrape data collection before the analysis clock starts,
// and the rate([2m]) PromQL needs a 2-min warm-up on top. If load ramps down
// while CV is still analysing, the canary goes idle and the SYMPTOM metrics
// (latency, 5xx, GC pause) "heal" instantly (a retained-memory leak stops
// allocating the moment traffic stops), diluting the tail buckets toward
// healthy. So we hold steady load for ~9 min (total run ~10 min incl. ramp) to
// cover the entire wall-clock window with margin. If you change the Verify
// duration, re-stretch these to (wall_time + ~1.5 min margin).
//
// The target Service load-balances across all pods (baseline + canary), so the
// canary receives its share of traffic — exactly what CV needs to compare the
// new pod against the baseline.
// ============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Harness's "Host URL" field rejects a port, so it passes the origin only
// (e.g. http://prod-banking-app-backend.banking-app-prod.svc.cluster.local).
// The backend listens on 8080, so if HOST_URL has no explicit port we add :8080.
const RAW = (__ENV.HOST_URL || 'http://localhost:8080').replace(/\/+$/, '');
const HOST = /:\d+$/.test(RAW) ? RAW : `${RAW}:8080`;

// Real seed identifiers (from BankingService.java) — same seed data in every
// env. Using fake IDs would 404 and generate meaningless error traffic.
const ACCOUNTS = ['acc-001', 'acc-002', 'acc-003'];
const MORTGAGES = ['mtg-001', 'mtg-002'];

// ---- Custom metrics (DISPLAY ONLY) ----------------------------------------
// Trends give p95/p99/avg/max per business journey in the results view.
const browseDuration = new Trend('journey_browse_duration', true);
const fraudDuration = new Trend('journey_fraud_duration', true);
// A functional error = a 200 wasn't returned or the body was empty.
const functionalErrors = new Rate('functional_errors');

function get(path, name) {
  // `name` tag groups identical URLs (e.g. per-account) into one row in the
  // results' Endpoint Statistics — display only.
  const res = http.get(`${HOST}${path}`, { tags: { name } });
  const ok = check(res, {
    'status is 200': (r) => r.status === 200,
    'body is non-empty': (r) => r.body && r.body.length > 0,
  });
  functionalErrors.add(!ok);
  return res;
}

// ============================================================================
// SCENARIOS — two workloads run in PARALLEL, sized to hold steady load across
// the full Verify WALL-CLOCK window (steady stage ~9m; total run ~10m).
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
        { duration: '30s', target: 15 },   // ramp up
        { duration: '9m', target: 15 },    // steady state — outlasts CV's ~8.5m wall time
        { duration: '20s', target: 0 },    // ramp down
      ],
    },

    // Journey 2: the fraud-check integration path, driven harder and starting
    // slightly later so it overlaps the browse steady-state. This cross-service
    // call is the most likely thing to degrade under a bad deploy.
    fraud_check_pressure: {
      executor: 'ramping-vus',
      exec: 'fraudJourney',
      startVUs: 0,
      startTime: '20s',
      gracefulStop: '10s',
      stages: [
        { duration: '30s', target: 25 },
        { duration: '8m40s', target: 25 }, // steady state — outlasts CV's ~8.5m wall time
        { duration: '20s', target: 0 },
      ],
    },
  },

  // NO thresholds — see header. CV is the gate; this step just generates load.
};

// ---- Default function -----------------------------------------------------
// Harness requires a literal `export default function`. The parallel scenarios
// drive load via their `exec` fields; this default keeps the script valid for
// the uploader and makes `k6 run loadtest/prod-load.js` work standalone.
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
