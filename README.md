# C&I Banking (demo app for NatWest walkthrough)

Fictional demo bank app, two services, one Helm chart per service. Purple palette echoes
NatWest's without using any real logos, marks, or copy. Built to demonstrate a full
Harness CI → CD flow onto EKS across four environments.

## Structure

```
backend/    Spring Boot 3.3 REST API (Java 17), in-memory dummy accounts + mortgages
frontend/   Static HTML/CSS/JS dashboard, served by nginx, calls the backend API
helm/       Two Helm charts, one per Harness Service
  backend/    Deployment + Service + ConfigMap + Secret for the API
  frontend/   Deployment + Service + ConfigMap + Secret + Ingress for the dashboard
start.sh    Local run helper (Colima + Docker), see "Run locally"
stop.sh     Tear down the local run
```

## Backend API

- `GET /api/summary` - totals for the dashboard cards
- `GET /api/accounts` / `GET /api/accounts/{id}` / `GET /api/accounts/{id}/balance`
- `GET /api/mortgages` / `GET /api/mortgages/{id}`
- `GET /actuator/health` - liveness/readiness probes

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

Then open http://localhost:8081. Backend is on http://localhost:8080.

> First-time Docker note: if image pulls fail with `docker-credential-desktop ... not
> found`, remove the `"credsStore": "desktop"` line from `~/.docker/config.json` (a
> leftover from Docker Desktop). Colima doesn't need it.

## Tests

Both services have real unit tests, run by the Harness CI pipeline.

**Backend** (JUnit 5 + MockMvc, working dir `backend/`):
```
mvn -B clean test        # 14 tests; JUnit report at target/surefire-reports/*.xml
```
Note: `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` is
set to `mock-maker-subclass` so tests run on CI runners where Mockito's default inline
mock-maker can't self-attach a Java agent.

**Frontend** (Jest + jsdom, working dir `frontend/`):
```
npm install && npm test  # 8 tests: gbp() formatter, fetchJson, render functions
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

## Notes / known demo shortcuts

- Images use `tag: latest`. Fine for a demo, but for real CD you'd pin the CI-produced
  tag (build number / git SHA) so each deploy is traceable and rollbacks are clean.
- Backend CORS is `*` (`CORS_ALLOWED_ORIGINS`). Because routing is same-origin it's not
  exercised, but you'd lock it down for a real deployment.
- No secrets or real data anywhere in this repo — safe for a client-facing demo.
```
