# Browser tests

Pantry browser tests use Playwright Chromium against an isolated PostgreSQL, RabbitMQ, nginx, Spring Boot, and built Vite frontend stack. The browser uses `http://127.0.0.1:8089`; nginx proxies the frontend and `/api/` so the smoke test exercises the production routing shape.

## First-time setup

Install the frontend dependencies and Playwright's Chromium once:

```bash
cd frontend
npm ci
npx playwright install chromium
cd ..
```

## Ordinary run

Run the complete isolated lifecycle from the repository root:

```bash
./scripts/run-e2e.sh
```

The runner removes stale `pantry-e2e` resources, starts and waits for dependency health checks, runs Playwright, captures Compose logs on failure, and always removes containers and volumes. It requires Docker Desktop or a running Docker daemon, npm, Java 24, and the first-time Playwright installation.

## Useful commands

Type-check E2E test code without starting the stack:

```bash
cd frontend
npm run e2e:typecheck
```

Inspect the HTML report after a run:

```bash
cd frontend
npx playwright show-report playwright-report
```

For Playwright UI or headed debugging, start the isolated dependencies manually in one terminal, then run Playwright in another. Clean up afterward:

```bash
docker compose --project-name pantry-e2e --file compose.e2e.yaml up --wait
cd frontend
npm run e2e -- --ui
cd ..
docker compose --project-name pantry-e2e --file compose.e2e.yaml down --volumes --remove-orphans
```

The Playwright configuration starts Spring Boot and the built Vite preview server. Keep the manually started dependency stack running for the entire debug session.

## Diagnostics

Failed tests retain screenshots and video in `frontend/test-results/`, plus the HTML report in `frontend/playwright-report/`. CI retries once and records a trace on the retry; traces include action history, DOM snapshots, network activity, and console messages. The local runner also writes `frontend/test-results/compose.log` and `frontend/test-results/runtime.log` (including Spring Boot output) when startup or a test fails.

Treat traces as sensitive test diagnostics: they can contain dummy authorization data and request bodies. They are generated only for the synthetic E2E identity.
