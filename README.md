# C&I Banking (demo app for NatWest walkthrough)

Fictional demo bank app, two services, one Helm chart per service. Purple palette echoes
NatWest's without using any real logos, marks, or copy. Built to demonstrate a full
Harness CI → CD flow onto EKS across four environments.

## Structure

```
backend/              Spring Boot 3.3 REST API (Java 17), in-memory dummy data
frontend/             Static HTML/CSS/JS dashboard, served by nginx, calls the backend API
fraud-check-service/  Downstream "Payments Fraud Check" stub (plain k8s YAML + local Dockerfile)
helm/                 Two Helm charts, one per Harness Service
  backend/    Deployment + Service + ConfigMap + Secret for the API
  frontend/   Deployment + Service + ConfigMap + Secret + Ingress for the dashboard
start.sh    Local run helper (Colima + Docker), see "Run locally"
stop.sh     Tear down the local run
```

## Backend API

- `GET /api/summary` - totals for the dashboard cards
- `GET /api/accounts` / `GET /api/accounts/{id}` / `GET /api/accounts/{id}/balance`
- `GET /api/accounts/{id}/transactions` - recent transactions for an account (drill-down)
- `GET /api/mortgages` / `GET /api/mortgages/{id}`
- `GET /api/fraud-check` - calls the downstream Payments Fraud Check service; returns
  `200` + `integration: ok` when the dependency is reachable, `502` +
  `integration: unavailable` when it is not (used by the DEV integration gate)
- `GET /actuator/health` - liveness/readiness probes

The frontend is a NatWest-style dashboard: clickable account rows open a slide-in drawer
showing balances and recent transactions.

## Frontend

Plain HTML/CSS/JS, no build step. `config.template.js` is rendered into `config.js` at
container startup (via `docker-entrypoint.sh` + `envsubst`) using the `API_BASE_URL`
env var. `API_BASE_URL=""` means **same origin**: the browser calls `/api/...` on
whatever host served the page (the shared ALB), so there is no CORS and no need for a
browser-resolvable backend hostname.

---

## Run locally

The app runs in containers via Colima (no Java/Maven/Docker Desktop needed). On Apple
Silicon the images build for `linux/amd64` (the backend's alpine JRE base has no arm64
build), so they run under emulation.

```
./start.sh            # start Colima if needed, build images if missing, run both
./start.sh --rebuild  # force a rebuild after code changes
./stop.sh             # remove the containers
./stop.sh --engine    # also stop the Colima engine (full teardown)
```

Then open http://localhost:8081. Backend is on http://localhost:8080, and the downstream
fraud-check integration is at http://localhost:8080/api/fraud-check.

`start.sh` runs **three** containers on a shared docker network: backend, frontend, and
the downstream fraud-check stub (`:9090`). The backend reaches the downstream by name
via `DOWNSTREAM_URL`.

> First-time Docker note: if image pulls fail with `docker-credential-desktop ... not
> found`, remove the `"credsStore": "desktop"` line from `~/.docker/config.json` (a
> leftover from Docker Desktop). Colima doesn't need it.

## Tests

Both services have real unit tests, run by the Harness CI pipeline.

**Backend** (JUnit 5 + MockMvc, working dir `backend/`):
```
mvn -B clean test        # 21 tests; JUnit report at target/surefire-reports/*.xml
```
Covers the banking service, account/transaction controllers, and the downstream
fraud-check integration (`FraudCheckServiceTest` uses okhttp3 MockWebServer for the
healthy + unavailable paths). Notes:
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` is set
  to `mock-maker-subclass` so tests run on CI runners where Mockito's default inline
  mock-maker can't self-attach a Java agent.

**Frontend** (Jest + jsdom, working dir `frontend/`):
```
npm install && npm test  # 14 tests: gbp/formatDate/initials, fetchJson, render functions
```
`app.js` has a `module.exports` shim that only activates under Node/Jest (no effect in
the browser).

## Docker images

```
docker build -t <registry>/banking-app-backend:latest backend/
docker build -t <registry>/banking-app-frontend:latest frontend/
```

CI publishes to Docker Hub: `hamzaziziharness/banking-app-backend` and
`hamzaziziharness/banking-app-frontend`.

---

# Deployment: Harness CD onto EKS

This section documents the full foundation, exactly as set up for the demo.

## Target cluster

- **Cluster**: `online-boutique` EKS cluster, region **eu-west-2**, AWS account `759984737373`.
- **Mode**: **EKS Auto Mode** (Bottlerocket nodes). Auto Mode provides native, built-in
  load balancing — it provisions ALBs from `Ingress` objects and NLBs from `LoadBalancer`
  Services. There is **no** `aws-load-balancer-controller` to install.
- **Delegate**: a Harness delegate already runs in-cluster (namespace `harness-delegate-ng`),
  so the Harness Kubernetes connector uses **"connect through delegate"** — no AWS
  credentials or cloud connector needed.

## AWS / cluster setup (one-time)

These steps were done once against the cluster (via AWS CloudShell + `kubectl`):

1. **IngressClass for Auto Mode's ALB.** Auto Mode ships the capability but doesn't
   create an IngressClass; we created one:
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: IngressClass
   metadata:
     name: eks-alb
   spec:
     controller: eks.amazonaws.com/alb
   ```

2. **Removed a stale nginx install.** The cluster had a leftover `nginx` IngressClass and,
   critically, an orphaned `ValidatingWebhookConfiguration` named `ingress-nginx-admission`
   whose backing service (`ingress-nginx-controller-admission`) no longer existed. It
   intercepted **all** Ingress creation cluster-wide and failed. Removed with:
   ```
   kubectl delete validatingwebhookconfiguration ingress-nginx-admission
   kubectl delete ingressclass nginx
   ```

3. **Namespaces** (one per environment, each holding BOTH services):
   ```
   kubectl create ns banking-app-dev
   kubectl create ns banking-app-sit
   kubectl create ns banking-app-staging
   kubectl create ns banking-app-prod
   ```

4. **Pinned ALB subnets.** This VPC (`vpc-3a5b8252`) is shared with another cluster, so
   ALB subnet auto-discovery failed (`3 tagged for other cluster`). We pinned the three
   public subnets (one per AZ, `MapPublicIpOnLaunch=true`) directly on the frontend
   Ingress via `alb.ingress.kubernetes.io/subnets`:
   `subnet-1e0cf064` (eu-west-2a), `subnet-5fa7a512` (eu-west-2b), `subnet-dfbb05b6` (eu-west-2c).
   These are VPC-wide, so the same three work for every environment in this cluster.

## Networking design

One **ALB per environment**, provisioned by EKS Auto Mode from a single Ingress in the
frontend chart, with path-based routing on one hostname:

```
browser ──▶ ALB (per environment)
              ├── /api/*  ──▶ <env>-banking-app-backend   Service (ClusterIP :8080)
              └── /*      ──▶ <env>-banking-app-frontend  Service (ClusterIP :80)
   one hostname · same origin · no CORS · no browser-DNS problem
```

- Both services are **ClusterIP** — the ALB is the only public entry point.
- The frontend calls relative `/api/...` paths, and the Ingress routes `/api` to the
  backend on the **same host**, so there is no cross-origin request (no CORS) and no
  need for a browser-resolvable backend URL.
- No path rewriting: the Spring backend already serves under `/api/*`, so `/api` must
  **not** be stripped.
- ALB health checks are set per-service via annotations on each Service
  (`alb.ingress.kubernetes.io/healthcheck-path`) — backend `/actuator/health/readiness`,
  frontend `/healthz` — so targets pass health checks instead of getting 404s on `/`.

## Environments

Four environments, one namespace each, both services inside:

| Harness Environment | Namespace              | Resource name prefix |
| ------------------- | ---------------------- | -------------------- |
| Dev                 | `banking-app-dev`      | `dev-`               |
| SIT                 | `banking-app-sit`      | `sit-`               |
| Staging             | `banking-app-staging`  | `staging-`           |
| Prod                | `banking-app-prod`     | `prod-`              |

Each environment gets its **own ALB and its own URL**. No chart changes are needed per
environment — only a Harness Environment + Infrastructure Definition (K8s connector +
namespace).

## How the charts parameterize

`values.yaml` uses Harness expressions as defaults, resolved before Helm renders:

- `name: <+env.name.toLowerCase()>-<+service.name>` — resource names are prefixed with
  the environment, e.g. `dev-banking-app-backend`. `.toLowerCase()` is **required**:
  Kubernetes names must be RFC 1123 lowercase, but environment names like `SIT`/`Staging`
  are mixed-case.
- `image: <+artifact.image>` — bound to the Service's Docker Hub Artifact Source.
- `namespace: <+infra.namespace>` — driven by the Infrastructure Definition.
- The frontend Ingress `/api` target (`backendServiceName`) is also
  `<+env.name.toLowerCase()>-banking-app-backend` so it matches the backend's prefixed
  name in the same environment.

Config/secrets come from the `env.config` / `env.secrets` maps and are mounted via
`envFrom`, so config changes don't need a new image — just a redeploy.

## Harness Services

Two `NativeHelm` services (org `sandbox`, project `devx_hamza`):

| Service               | Chart path (Git)  | Artifact (Docker Hub)                       |
| --------------------- | ----------------- | ------------------------------------------- |
| `banking-app-backend` | `helm/backend`    | `hamzaziziharness/banking-app-backend:latest` |
| `banking-app-frontend`| `helm/frontend`   | `hamzaziziharness/banking-app-frontend:latest`|

- Manifest store: GitHub connector `hamzagit`, repo `banking-app`, branch `main`.
  **The manifest folder path is the chart directory** (`helm/backend`), not a file —
  Harness reads `Chart.yaml`, `values.yaml`, and all `templates/`.
- Artifact: Docker Registry connector `hamza_docker`.

Deploy **backend before frontend** so the backend Service exists when the frontend
Ingress references it for the `/api` route.

## Get the app URLs

```
for ns in banking-app-dev banking-app-sit banking-app-staging banking-app-prod; do
  echo "$ns: http://$(kubectl get ingress -n $ns -o jsonpath='{.items[0].status.loadBalancer.ingress[0].hostname}')"
done
```

Each URL serves the dashboard at `/` and the API at `/api/summary` on the same host.
Use `http://` — the demo ALB has no TLS. The ALB takes ~2-3 min to provision after the
Ingress is first created.

Live demo URLs (one ALB per environment; hostnames change if the Ingress is recreated):

| Environment | URL |
| ----------- | --- |
| Dev     | http://k8s-bankinga-devbanki-e43bbb9b08-1936307432.eu-west-2.elb.amazonaws.com |
| SIT     | http://k8s-bankinga-sitbanki-70b320897a-1006193120.eu-west-2.elb.amazonaws.com |
| Staging | http://k8s-bankinga-stagingb-e65599b8f5-837134503.eu-west-2.elb.amazonaws.com |
| Prod    | http://k8s-bankinga-prodbank-4f150074a0-1611983985.eu-west-2.elb.amazonaws.com |

## Downstream Payments Fraud Check service

The backend integrates with a downstream **Payments Fraud Check** service — a *stub*
(stock `nginx` serving canned JSON) that stands in for a real external dependency
(fraud/payments API or datastore). It exists so the DEV stage can do a real integration
test, and so a failure can be injected in prod later for the canary/rollback story.

It is **not** part of the Helm charts or the pipeline — it is a pre-existing dependency,
deployed once per namespace with a plain manifest (no image build needed):

```
URL=https://raw.githubusercontent.com/HamzaZizi/banking-app/main/fraud-check-service/k8s-fraud-check.yaml
kubectl apply -n banking-app-dev -f "$URL"
# repeat for banking-app-sit / -staging / -prod
```

The backend reaches it in-namespace at `http://fraud-check:9090` (set via `DOWNSTREAM_URL`
in the backend chart). Verify: `curl http://<env-alb>/api/fraud-check` returns
`integration: ok`.

---

# CI/CD pipeline (Harness)

Pipeline `hamza_devx` (org `sandbox`, project `devx_hamza`). The intended operating model
is: **Renovate proposes a dependency patch → Harness AI assesses the PR → developer
approves → Harness validates and promotes the same immutable artifact through to prod
with progressive delivery and automatic rollback.**

Stages:

1. **Build (parallel: frontend + backend)** — unit tests, SAST/SCA, Build-and-Push to
   Docker Hub, then supply-chain (SBOM, SLSA provenance, artifact signing — keyless/cosign).
2. **Harness AI PR Review** — AI agent assesses the merge request (confidence, rationale,
   risks, build/test evidence, recommendation).
3. **Deploy to Dev** — artifact verification, then Helm deploy. *(DEV = integration
   validation against the downstream fraud-check.)*
4. **Deploy to SIT** — promotes the same artifact, then runs a k6 load test as a
   performance gate. *(SIT = performance / regression — see "SIT gate" below.)*
5. **Deploy to Staging** — canary. *(STAGING = end-to-end.)*
6. **Deploy to Prod** — canary + progressive rollout with verification and auto-rollback.

## DEV gate — integration tests

The DEV stage's job (per the operating model) is to *deploy the patch and confirm the
updated dependency works correctly with the application and its immediate integrations*.
So after the Helm deploy, a **DEV Integration Tests** step group runs a series of distinct
checks against the **deployed** app (in-cluster, `http://dev-banking-app-backend:8080`).
Each check is its own step so it shows up individually in the pipeline graph, and any
failure fails the stage and triggers rollback (`StageRollback` / `HelmRollback`).

These are **integration/smoke tests against the running deployment** — distinct from the
unit tests, which run earlier in CI against the source before the image is even built.

| Step | Checks | What it proves | On fail |
| ---- | ------ | -------------- | ------- |
| **Health Check** | `GET /actuator/health` returns `status: UP` | the patched image boots and serves | rollback |
| **Summary Contract** | `GET /api/summary` returns `accountCount` | the app's own core contract survived the change | rollback |
| **Accounts List** | `GET /api/accounts` returns the seeded accounts | the primary data endpoint works | rollback |
| **Transactions Drilldown** | `GET /api/accounts/{id}/transactions` returns transactions | the drill-down endpoint works | rollback |
| **Downstream Fraud Integration** ⭐ | `GET /api/fraud-check` returns `200` + `integration: ok` | the backend can still reach and call the downstream Payments Fraud Check service after the patch — the core integration gate | rollback |
| **Negative Path 404** | `GET /api/accounts/<unknown>` returns `404` | error paths still behave correctly, not just the happy path | rollback |

The starred **Downstream Fraud Integration** check is the heart of the DEV gate: if a
dependency bump broke the HTTP client, serialization, or connectivity to the fraud
service, this is where it surfaces — before the artifact is ever promoted to SIT.

Each environment integrates with **its own** fraud-check instance (dev tests the dev
instance, never production). In a real system dev/SIT would target the vendor's sandbox
and only prod would hit the live fraud engine; here each namespace runs its own stub —
which is exactly what lets prod's instance be failed deliberately later to demonstrate
canary rollback.

## SIT gate — performance / load testing (k6 via Harness Resilience Testing)

Where DEV proves the patch *works* (integration/smoke), **SIT proves it still performs**.
The operating-model line for SIT is *"regression, compatibility, and performance checks"* —
this section covers the **performance** half: a k6 load test that runs against the freshly
deployed SIT app and gates promotion on latency/error SLOs.

### Where it runs in the stage

The load test runs **after** the Helm deploy, against the **deployed** SIT service
(exactly like the DEV integration checks run after the DEV deploy):

```
SIT stage
 ├─ 1. Helm deploy   → same immutable artifact promoted into banking-app-sit
 ├─ 2. pods ready    → readiness probes pass
 └─ 3. Load test     → k6 drives traffic at http://sit-banking-app-backend.banking-app-sit.svc.cluster.local:8080
        └─ any k6 threshold breached → step Failed → stage Failed → StageRollback
```

There must be a running deployment to load, so the test is ordered after deploy; it
targets the in-cluster Service DNS because the load runner executes inside the cluster.

### How the load test is set up

It's built in **Resilience Testing › Load Testing** (not a hand-rolled Run step), using
**Load Test Type = K6**, **Target Type = Kubernetes**, on the project's Kubernetes chaos
infrastructure (the same in-cluster infra used for chaos experiments). Mode is **Upload
K6 script** — the script lives in the repo at [`loadtest/sit-load.js`](loadtest/sit-load.js)
so the gate is version-controlled alongside the app (it flows through the same Renovate/
GitOps story as everything else). Host URL is passed to the script as `__ENV.HOST_URL`.

The load test is invoked from the pipeline via a **`LoadTest` step** (`spec.loadTestRef:
sitbankingappload`) inside the SIT stage, right after the Helm deploy. Because SIT is a
multi-service stage, the whole step block loops once per service (frontend, then backend);
the LoadTest step is gated with `when: <+service.identifier> == "bankingappbackend"` so it
runs **once**, on the backend iteration, after both services are deployed.

### What the k6 script tests

The script models **two real user journeys running in parallel** (k6 *scenarios*), not a
flat ping, so the load looks like production traffic:

| Scenario | Executor | Load | What it represents |
| -------- | -------- | ---- | ------------------ |
| `customer_browse` | `ramping-vus` | ramp to 15 VUs, hold 2m | A customer opening the app: summary → accounts list → drill into one account's detail / balance / transactions → mortgages. |
| `fraud_check_pressure` | `ramping-vus` | ramp to 25 VUs, hold 90s, overlapping browse | The cross-service `/api/fraud-check` path hammered independently — the boundary most likely to degrade after a bad dependency bump. |

Every request runs k6 **checks** (`status is 200`, non-empty body) and is **tagged by
endpoint** so the results table breaks latency down per route. Custom **Trend** metrics
(`journey_browse_duration`, `journey_fraud_duration`) measure each journey end-to-end, and
a custom **Rate** (`functional_errors`) tracks non-200/empty responses. Requests use the
**real** SIT seed IDs (`acc-001/002/003`, `mtg-001/002`) so `/{id}` routes return 200
rather than false-404ing the gate. **Note:** the tags and custom metrics feed the
*results display* (the Endpoint Statistics breakdown) — they are **not** what gates the
build (see the threshold note below).

### The gate itself (thresholds)

Per Harness's results model, **a high error rate alone does not fail a run — only a
breached k6 `threshold` marks it Failed.** So the script's `thresholds` block *is* the
gate:

```js
thresholds: {
  http_req_duration: ['p(95)<50', 'p(99)<1000'],                        // latency SLO
  http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' }],
  checks:          [{ threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '30s' }],
}
```

Two hard-won lessons are baked into that block:

**1. Gate only on STANDARD k6 metrics.** Harness's k6 gate evaluates only k6's built-in
metrics. **Custom metrics** (`Trend`/`Rate`) and **tag-scoped sub-metric thresholds** like
`http_req_duration{name:summary}` are *not* collected — they show **`N/E` (Not Evaluated)**
and silently never gate. An earlier layered version had 10 thresholds; a run showed **4
passed, 6 N/E** — a perfect split: the 4 standard metrics ran, every custom/tagged one was
dead. `N/E` is *not* passing, it's *not running* — so the gate now uses only
`http_req_duration`, `http_req_failed`, and `checks`, and every threshold genuinely runs.
The per-journey / per-endpoint numbers still appear in the results view for diagnosis, they
just don't gate.

**2. Tuned to the real baseline + `abortOnFail`.** The numbers are tuned to the observed
in-cluster baseline (p95 ~3ms, p99 ~597ms, 0% errors) — p95 sits ~15× above baseline: tight
enough to catch a real regression, loose enough to absorb jitter; p99 is generous so the
cold-start tail can't flake a healthy deploy. The **error** thresholds carry
`abortOnFail: true` (with `delayAbortEval: '30s'` to skip ramp-up): if a build is throwing
errors or failing checks, k6 **kills the run in seconds** rather than loading a known-broken
deploy for the full duration. The **latency** threshold deliberately does *not* abort —
percentiles need enough samples to be a trustworthy verdict.

> **Validated (run #3):** 3m26s, **PASS 4/4 (zero N/E)**, p95 3ms / p99 222ms, 0.00% errors,
> 100% checks (41,684/41,684) — *"All pass criteria met. Build can proceed."* Both scenarios
> `finished`. The Endpoint Statistics breakdown renders per route for diagnosis.

> **Gotcha — the UI "Load Profile Overrides" fields.** `Users` and `Duration` are marked
> optional but must be **> 0** — clearing them sends `0`, and k6 rejects `duration=0`
> (`E3002: duration should be more than 0`). Set `Duration` ≥ the script's total run length
> (e.g. 200s) as the overall run window, and any `Users > 0`. Also, the `Host URL` field
> rejects a port, so it passes the **origin only** — the script appends `:8080` when
> `HOST_URL` has no explicit port. With scenarios declared in the script, the UI Users value
> still injects a small background scenario, but the script's ramps drive the meaningful
> load and the gate is evaluated globally.

### Why do this with Harness rather than raw k6

Raw `k6 run` gives you the load engine and the pass/fail exit code — but nothing around
it. Doing it *through Harness Resilience Testing* adds:

- **It's a native pipeline gate.** The load test is a first-class object invoked by a step;
  a Failed run fails the stage and triggers the **same StageRollback/HelmRollback** the
  rest of the pipeline uses. No glue scripts, no parsing k6 JSON, no bespoke CI wiring.
- **In-cluster runner, managed for you.** k6 runs as pods on the existing chaos
  infrastructure inside the EKS cluster, so it reaches ClusterIP Services directly — no
  public exposure, no ingress hop, no runner to maintain. Distributed execution (splitting
  load across replica pods) is a config toggle, not a Kubernetes job you write.
- **Results are stored, visual, and comparable.** p95/p99, RPS, error rate, a response-time
  histogram, the checks table, and the pass/fail threshold table are rendered per run and
  kept for run-over-run comparison — versus raw k6's console output that scrolls away.
- **One tool, two jobs.** The *same* load test object is reused in Staging **in parallel
  with a chaos experiment** (fault injected *while* under load) to become a resilience
  gate. Raw k6 has no concept of coordinated fault injection; Harness does.
- **Fail fast on a bad build.** `abortOnFail` on the error thresholds means a build that's
  throwing errors is killed in *seconds*, not after a full load run — the stage fails and
  rolls back immediately, so a known-broken deploy never wastes the pipeline's time.
- **Governable (see below).** Because the gate is a pipeline object, policy-as-code can
  *require* it to exist — you can't quietly delete the performance gate and still ship.

### Logic vs. verdict vs. proof — where the script ends and Harness begins

A fair question: *if the thresholds are in the script, what is Harness actually adding?*
Three different things, and only the first lives in the script:

1. **The gate LOGIC — "what counts as passing?"** This is the `thresholds` block. Raw k6
   has it too. Nothing special yet.
2. **The VERDICT — "did it pass?"** Raw k6 emits a **process exit code** (0 = pass,
   non-zero = a threshold was breached) and prints to the console. That exit code lives
   for a few seconds in a terminal and is then gone. To *use* it you'd hand-build the glue:
   capture the code, decide what to do, wire a rollback, stash the output somewhere.
3. **The PROOF — "prove it passed, tied to this exact artifact, months later."** Raw k6
   has nothing here. Harness records the verdict as a stored step result — timestamped,
   bound to the specific image (`banking-app-backend:1.0.<seq>`), with the exact thresholds
   evaluated and the measured p95/p99/RPS/error-rate/histogram kept for audit and
   run-over-run comparison. The artifact reached Staging *because* this step passed, so the
   promotion itself is the evidence.

The one-liner: **k6 decides pass/fail in the moment; Harness makes that verdict durable,
enforced, and auditable.** For a bank, the proof is the product — an auditor or change
board wants evidence that *this version* met its performance SLO before it went near prod,
not a console log that scrolled away.

### Why the verdict can't be faked (anti-bypass)

The sharpest version of the question: *what stops someone forcing a green gate — either
deleting the whole step, or short-circuiting the script to `exit 0` so a slow build
"passes"?* This is exactly why the gate is a **platform object, not a shell script**:

- **You don't own the exit code.** With a hand-rolled `k6 run` in a Run step, the verdict
  is whatever the shell returns — so `k6 run … || true`, a trailing `exit 0`, or a script
  edited to skip the assertions all *simulate success*, and nothing notices. In Harness the
  load test is a **first-class step whose pass/fail is computed by the platform from the
  threshold results**, not from a shell exit code you can override. There is no `|| true`
  to add — you're not the one deciding the return value.
- **Deleting/disabling the step is itself blocked.** Because the pipeline is versioned YAML
  governed by **OPA policy-as-code**, a Rego rule inspects the pipeline on every run and
  *denies execution* if the SIT load-test step is missing, disabled, or its
  `continueOnFailure`/failure-strategy has been loosened to ignore a breach. So "just remove
  the gate" fails the pipeline before it starts. (See the OPA section below.)
- **Tampering is on the record.** The pipeline YAML lives in Git and every execution is
  audit-logged — *who* changed the gate, *when*, and *which run* skipped it are all visible.
  Faking a pass isn't a silent edit; it's a tracked, attributable, policy-violating change.
- **The proof is bound to the artifact, not re-assertable after the fact.** You can't
  retroactively stamp "passed" onto an image. The only way `1.0.41` shows a passing SIT
  gate is if *that run* actually evaluated the thresholds and they held.

Net: to bypass the gate you'd have to defeat *three independent layers* — the
platform-computed verdict, an OPA policy that requires the gate to exist and stay
fail-closed, and a Git/audit trail that records the attempt. A raw-k6-in-CI setup has none
of these; a single `|| true` defeats it silently.

### Optional: enforcing the gate with OPA (policy-as-code / governance)

The k6 threshold answers *"did the app stay fast?"*. **OPA answers a different question:
*"was the process followed?"*** — and that's where it belongs, not as the latency check
itself (the threshold already does that far more directly).

Concretely, a Harness **Governance / OPA policy** (Rego) evaluated on the pipeline can
enforce guardrails such as:

- **The SIT performance gate must exist and must run before Prod.** A Rego rule that
  inspects the pipeline YAML and *denies* execution if the SIT load-test step has been
  removed or disabled — so nobody can ship to Prod having skipped the gate.
- **No promotion without the required approvals / signed image / etc.** (governance rules
  that have nothing to do with latency but everything to do with a trustworthy pipeline).

So the mental model for SIT is two complementary layers: **k6 thresholds = the performance
gate** (fail-closed on latency/errors), and **OPA = the governance gate that guarantees the
performance gate is present and enforced.** For the demo, get the k6 gate green first; the
OPA policy is a strong second beat that shows the gate can't be bypassed.

## Immutable, promotable artifacts

Images are tagged **`1.0.<+pipeline.sequenceId>`** (e.g. `banking-app-backend:1.0.41`),
**not** `latest`. This matters for two reasons:

- **Rollouts actually happen.** With `latest`, Helm renders an identical manifest every
  time, sees no change, and never restarts the pods — so a "successful" deploy shipped
  nothing. A changing tag makes the manifest change, so Helm rolls the pods.
- **Same artifact promotes unchanged.** Build and deploy run in one pipeline execution,
  so `1.0.<+pipeline.sequenceId>` resolves to exactly what was just built and is promoted
  identically through dev → sit → staging → prod (SIT/Staging/Prod use
  `useFromStage: Deploy to Dev`).

The image tag must be consistent across the whole run: the **build** step pushes it, and
the **SBOM / SLSA / signing / verification** steps must all reference the *same*
`...:1.0.<+pipeline.sequenceId>` — signing one tag while verifying another gives
"no signatures found".

> Evidence Vault is not enabled on this account, so signatures live in the registry
> (keyless cosign) rather than Harness's managed store — verification falls back to the
> registry, which works.

## Building a PR branch (the Renovate flow)

The codebase `build` input selects which branch CI builds. For PR validation, set it to
the PR's source branch — in the automated flow a **PR webhook trigger** fills this with
`<+trigger.sourceBranch>` so Renovate PRs build automatically. `main` is never touched
until the PR is merged; the PR's build is deployed and validated first.

## Notes / known demo shortcuts

- Backend CORS is `*` (`CORS_ALLOWED_ORIGINS`). Because routing is same-origin it's not
  exercised, but you'd lock it down for a real deployment.
- The fraud-check downstream is a stub returning canned data — in production it would be
  a real external service or datastore.
- No secrets or real data anywhere in this repo — safe for a client-facing demo.
```
