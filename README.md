# C&I Banking (demo app for NatWest walkthrough)

Fictional demo bank app, two services, one Helm chart. Purple palette echoes NatWest's
without using any real logos, marks, or copy.

## Structure

```
backend/    Spring Boot 3.3 REST API (Java 17), in-memory dummy accounts + mortgages
frontend/   Static HTML/CSS/JS dashboard, served by nginx, calls the backend API
helm/       Two Helm charts, one per Harness Service
  backend/    Deployment + Service + ConfigMap + Secret for the API
  frontend/   Deployment + Service + ConfigMap + Secret for the dashboard
```

## Backend API

- `GET /api/summary` - totals for the dashboard cards
- `GET /api/accounts` / `GET /api/accounts/{id}` / `GET /api/accounts/{id}/balance`
- `GET /api/mortgages` / `GET /api/mortgages/{id}`
- `GET /actuator/health` - liveness/readiness probes

Build and run locally:

```
cd backend
mvn clean package
java -jar target/ci-banking-backend-1.0.0.jar
```

## Frontend

Plain HTML/CSS/JS, no build step. `config.template.js` is rendered into `config.js` at
container startup (via `docker-entrypoint.sh` + `envsubst`) using the `API_BASE_URL`
env var, so the same image works in any environment without a rebuild.

Local run:

```
cd frontend
python3 -m http.server 8081
# edit config.template.js -> config.js manually with your backend URL for a quick local test
```

## Docker images

```
docker build -t <registry>/ci-banking-backend:latest backend/
docker build -t <registry>/ci-banking-frontend:latest frontend/
docker push <registry>/ci-banking-backend:latest
docker push <registry>/ci-banking-frontend:latest
```

## Helm charts

Two independent charts, one per Harness Service, matching how Harness actually models
deployable units. Local test (substitute the Harness expressions with real values first,
Harness does this automatically at deploy time):

```
helm lint helm/backend
helm template backend helm/backend \
  --set name=ci-banking-backend --set namespace=demo --set image=myregistry/ci-banking-backend:1.0.0

helm lint helm/frontend
helm template frontend helm/frontend \
  --set name=ci-banking-frontend --set namespace=demo --set image=myregistry/ci-banking-frontend:1.0.0
```

Each chart deploys: Deployment, Service, ConfigMap, Secret (config/secrets come from the
`env.config` / `env.secrets` maps in values.yaml and are mounted via `envFrom`, so config
changes don't need a new image).

## Wiring into Harness CD

Both `values.yaml` files use Harness expressions directly as defaults, this is the
idiomatic pattern: Harness resolves `<+...>` expressions in the values file before Helm
renders it, so the values file itself is the parameterization layer.

- `name: <+stage.name>` - resource naming driven by the Stage
- `image: <+artifact.image>` - bound to the Service's Artifact Source
- `namespace: <+infra.namespace>` - driven by the Environment/Infrastructure Definition

Per-environment differences (replica count, resource limits, `CORS_ALLOWED_ORIGINS` on the
backend, `API_BASE_URL` on the frontend, `serviceType`/ingress) go in a **Values YAML**
override at the Environment level, not hardcoded in the chart. `frontend.env.config.API_BASE_URL`
is the one that has to change per environment/cluster since it's how the frontend finds
the backend, everything else is safe to leave at the chart defaults for a demo.

No secrets or real data anywhere in this repo, safe to use in a client-facing demo.
