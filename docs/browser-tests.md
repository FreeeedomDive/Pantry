# Browser tests

Pantry browser tests use Playwright Chromium against an isolated PostgreSQL, RabbitMQ, nginx, Spring Boot, and built Vite frontend stack. The browser uses `http://127.0.0.1:8089`; nginx proxies the frontend and `/api/` so the suite exercises the production routing shape.

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

The runner removes stale `pantry-e2e` resources, starts and waits for dependency health checks, runs every spec, captures Compose logs on failure, and always removes containers and volumes. Playwright starts the E2E-profile Spring Boot application and builds and serves the frontend in Vite's `e2e` mode. The lifecycle requires Docker Desktop or a running Docker daemon, npm, Java 24, and the first-time Playwright installation.

## Test identity and setup

The E2E frontend build contains no user authorization value. Before the application starts, the fixtures inject freshly signed initData into `window.__PANTRY_E2E_TELEGRAM_INIT_DATA__` with `addInitScript`; Vite's `e2e` mode reads that runtime value to initialize the Telegram SDK mock. The matching API request context sends the same value as `Authorization: tma <initData>`.

Every test attempt receives a synthetic Telegram identity derived from the run, worker, project, test ID, and retry. Additional users also include a per-test sequence, so tests and retries never intentionally share users. Multi-user tests create a distinct Playwright `BrowserContext`, page, initData value, and API request context for each user instead of replacing credentials in one browser context.

The verified default is two workers for local and CI runs with `fullyParallel: false`. Spec files may run concurrently, while tests inside each file keep their declared order. Increase this only after repeated full runs against the isolated stack remain stable.

Registration is lazy in the application. Tests must call the `registerUser` fixture, or an additional user's `listPantries`, explicitly before relying on that user's `Default` pantry. Prepare products, stock, and other state through the typed builders in `frontend/e2e/api.ts`; keep browser actions for the behavior under test.

Membership setup uses `POST /api/e2e/pantries/{pantryId}/members` through the `addPantryMember` builder. The endpoint authenticates the owner, registers the target synthetic Telegram ID, and invokes the application membership use case. It exists only under the Spring `e2e` profile. Do not enable it outside the isolated E2E application, point tests at a shared environment, alter the database directly, or use a production bot token.

The E2E profile fixes the bot username to `pantry_e2e`, disables the bot, and uses only the repository's dummy token to sign synthetic initData. Invitation tests therefore build deterministic `https://t.me/pantry_e2e` links without calling Telegram `getMe` or sending network requests to Telegram.

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

Run one spec from the repository root while preserving the isolated dependency lifecycle:

```bash
(
  set -euo pipefail
  cleanup() {
    docker compose --project-name pantry-e2e --file compose.e2e.yaml down --volumes --remove-orphans || true
  }
  trap cleanup EXIT
  cleanup
  docker compose --project-name pantry-e2e --file compose.e2e.yaml up --wait --remove-orphans
  npm --prefix frontend run e2e -- e2e/members.spec.ts
)
```

Replace `e2e/members.spec.ts` with any single spec path: `e2e/smoke.spec.ts`, `e2e/auth.spec.ts`, `e2e/pantries.spec.ts`, `e2e/catalog.spec.ts`, `e2e/products.spec.ts`, `e2e/reliability.spec.ts`, or `e2e/invites.spec.ts`. Playwright starts and stops the E2E backend and frontend for the selected spec; the cleanup trap recreates the database and RabbitMQ state before the run and removes their containers and volumes afterward, including when the test fails.

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

Failed tests retain screenshots and video in `frontend/test-results/`, plus the HTML report in `frontend/playwright-report/`. CI retries once and records a trace on the retry; traces include action history, DOM snapshots, network activity, request headers and bodies, and console messages. The local runner also writes `frontend/test-results/compose.log` and `frontend/test-results/runtime.log` (including Spring Boot output) when startup or a test fails.

Treat traces, screenshots, video, reports, and logs as sensitive test diagnostics even though tests use unique synthetic identities and the dummy E2E signing token. Do not put production initData, bot tokens, user data, or other production secrets into E2E configuration or fixtures, and do not publish diagnostic artifacts outside approved CI storage.
