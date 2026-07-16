# Browser scenario catalog

This catalog describes the product behaviors covered by browser tests. Keep each scenario description and its implementation in sync.

## Status legend

- `Planned`: agreed behavior without a browser test yet.
- `Implemented`: covered by an automated browser test.
- `Blocked`: cannot yet be automated, with the reason recorded here.

## E2E-001: New signed Telegram user can open the application

- Status: `Implemented`
- Preconditions: The isolated E2E stack is running with a clean database. The frontend receives freshly signed initData for the synthetic E2E Telegram user.
- Steps: Open `/` through nginx.
- Expected result: The user is authenticated through the real API, is registered, receives a `Default` inventory, and sees the `Мои инвентари` heading.
- Test: `frontend/e2e/smoke.spec.ts`

## Template

### E2E-XXX: Scenario title

- Status: `Planned`
- Preconditions: Required application and data state.
- Steps: User actions and navigation.
- Expected result: Observable product outcome.
- Test: `frontend/e2e/example.spec.ts` or `Not implemented`
