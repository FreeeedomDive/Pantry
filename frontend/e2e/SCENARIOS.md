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

## E2E-002: Returning user keeps the existing default inventory

- Status: `Implemented`
- Preconditions: The signed E2E Telegram user has already opened the application and received the `Default` inventory.
- Steps: Reload `/`, then leave and open the application again with the same Telegram identity.
- Expected result: The existing `Default` inventory is shown with the `Владелец` and `По умолчанию` badges; no duplicate user or inventory is created.
- Test: `frontend/e2e/auth.spec.ts`

## E2E-003: Invalid Telegram authorization is explained to the user

- Status: `Implemented`
- Preconditions: Requests from the browser are made with missing, invalidly signed, or expired Telegram initData.
- Steps: Open `/` and wait for the inventory request to fail authentication.
- Expected result: The page shows an error state with `Не удалось авторизоваться. Откройте приложение из Telegram.` and a `Повторить` action instead of application data.
- Test: `frontend/e2e/auth.spec.ts`

## E2E-004: Owner creates a named inventory

- Status: `Implemented`
- Preconditions: The signed E2E user is on `/` and owns the default inventory.
- Steps: Open `Новый инвентарь`, verify that an empty or whitespace-only name cannot be submitted, enter a name with surrounding whitespace, and create the inventory.
- Expected result: The name is trimmed, the inventory is persisted, and the browser navigates to `/pantries/{pantryId}` with the new name as the heading. Returning to `/` shows both inventories, while `Default` remains the default one.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-005: Owner renames an inventory

- Status: `Implemented`
- Preconditions: The signed E2E user owns an inventory and is on `/`.
- Steps: Open the inventory actions and choose `Переименовать`; verify that an empty or unchanged name cannot be submitted, then enter a new name and save.
- Expected result: The modal closes and the inventory card shows the trimmed new name after the server update.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-006: User changes the default inventory

- Status: `Implemented`
- Preconditions: The signed E2E user has at least two inventories and is on `/`.
- Steps: Open the actions of a non-default inventory and choose `Сделать инвентарём по умолчанию`.
- Expected result: The `По умолчанию` badge moves to the selected inventory, and the action is no longer offered for that inventory.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-007: Owner cannot delete the last owned inventory

- Status: `Implemented`
- Preconditions: The signed E2E user owns only the automatically created `Default` inventory.
- Steps: Open its actions, choose `Удалить`, review the irreversible-action warning, and confirm deletion.
- Expected result: The server rejects the operation; a notification explains that another owned inventory must be created first, the confirmation modal remains open, and `Default` is not removed.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-008: Owner deletes an additional inventory

- Status: `Implemented`
- Preconditions: The signed E2E user owns `Default` and one additional inventory.
- Steps: Open the additional inventory actions, choose `Удалить`, and confirm the irreversible operation.
- Expected result: The confirmation modal closes and the deleted inventory disappears from `/`, while `Default` remains available.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-009: Owner opens the inventory member list

- Status: `Implemented`
- Preconditions: The signed E2E user owns an inventory and is on `/`.
- Steps: Open the inventory actions, choose `Участники`, and inspect the member list.
- Expected result: The browser navigates to `/pantries/{pantryId}/members`; the current Telegram ID is marked with `(вы)` and `Владелец`, and owner-only invitation controls are visible.
- Test: `frontend/e2e/pantries.spec.ts`

## E2E-010: Empty inventory shows both catalog empty states

- Status: `Implemented`
- Preconditions: The signed E2E user has an inventory without products.
- Steps: Open the inventory, inspect `Все товары`, then switch to `Список покупок`.
- Expected result: `Все товары` explains that there are no products, while `Список покупок` explains that no regular products need to be bought.
- Test: `frontend/e2e/catalog.spec.ts`

## E2E-011: Catalog shows product balances and opens a product

- Status: `Implemented`
- Preconditions: Products are created through the regular product REST API without receipt processing; one has a brand and stock in multiple batches, another has zero stock.
- Steps: Open the inventory on `Все товары`, inspect the product cards, and select a product.
- Expected result: Each card shows its name, optional brand, and total quantity across batches; products with positive stock appear before zero-stock products, and selecting a card opens `/pantries/{pantryId}/products/{productId}`.
- Test: `frontend/e2e/catalog.spec.ts`

## E2E-012: Shopping list contains only depleted regular products

- Status: `Implemented`
- Preconditions: The inventory contains all four combinations of regular/non-regular products with positive/zero stock, prepared through the product and stock REST APIs.
- Steps: Open the inventory and switch from `Все товары` to `Список покупок`.
- Expected result: Only products marked as regular and having a zero total quantity are shown.
- Test: `frontend/e2e/catalog.spec.ts`

## E2E-013: Product page describes stock batches

- Status: `Implemented`
- Preconditions: A product with a brand has multiple stock batches; at least one batch has an expiration date and one has none. Data is prepared through the product and stock REST APIs.
- Steps: Open the product directly or from the inventory catalog and inspect its header and batches.
- Expected result: The page shows the product name, brand, total quantity, localized purchase and expiration dates, each batch quantity, and `Срок годности не указан` for the batch without an expiration date.
- Test: `frontend/e2e/catalog.spec.ts`

## E2E-014: User edits a product name and brand

- Status: `Implemented`
- Preconditions: A product is prepared through the product REST API and its page is open.
- Steps: Open `Переименовать товар`; verify that an empty name and an unchanged form cannot be submitted, then change the name and brand and save. Reopen the form, clear the brand, and save again.
- Expected result: Values are trimmed, the product heading and brand update after each successful request, and a cleared brand is no longer displayed.
- Test: `frontend/e2e/products.spec.ts`

## E2E-015: User toggles a depleted product in the shopping list

- Status: `Implemented`
- Preconditions: A non-regular product with zero stock is prepared through the product REST API.
- Steps: On the product page choose `Сделать постоянным`, return to the inventory and inspect `Список покупок`; then open the product again and choose `Убрать из постоянных`.
- Expected result: The depleted product appears in the shopping list after being made regular and disappears after the setting is removed.
- Test: `frontend/e2e/products.spec.ts`

## E2E-016: Swipe writes off one item from a stock batch

- Status: `Implemented`
- Preconditions: A product has a stock batch with quantity greater than one, prepared through the stock REST API.
- Steps: Open the product and swipe the batch left past the write-off threshold.
- Expected result: Exactly one item is written off; the batch remains, and both its quantity and the product total decrease by one.
- Test: `frontend/e2e/products.spec.ts`

## E2E-017: Writing off the last item depletes but keeps the product

- Status: `Implemented`
- Preconditions: A regular product has one stock batch with quantity one, prepared through the product and stock REST APIs.
- Steps: Open the product and swipe the only batch left past the write-off threshold, then return to the inventory and open `Список покупок`.
- Expected result: The batch disappears, the product total becomes `0 шт`, the page shows `Партий нет — весь запас израсходован.`, the product remains in the catalog, and it appears in the shopping list.
- Test: `frontend/e2e/products.spec.ts`

## E2E-018: User deletes a product with its stock

- Status: `Implemented`
- Preconditions: A product with at least one stock batch is prepared through the product and stock REST APIs.
- Steps: Open the product actions, choose `Удалить товар`, review the irreversible-action warning, and confirm deletion.
- Expected result: The browser returns to `/pantries/{pantryId}` and the deleted product is absent from the catalog.
- Test: `frontend/e2e/products.spec.ts`

## E2E-019: Application routes survive direct opening and reload

- Status: `Implemented`
- Preconditions: An inventory and product are available to the signed E2E user.
- Steps: Directly open and reload `/pantries/{pantryId}`, `/pantries/{pantryId}/products/{productId}`, and `/pantries/{pantryId}/members` through nginx.
- Expected result: Nginx serves the frontend entry point for every route, and each page restores its data and expected parent navigation without a server-side 404.
- Test: `frontend/e2e/reliability.spec.ts`

## E2E-020: Query failure can be retried from the page

- Status: `Implemented`
- Preconditions: Playwright temporarily intercepts an application query and returns a server or network failure before allowing a successful response.
- Steps: Open the affected page, wait for the error state, restore the API response, and choose `Повторить`.
- Expected result: The page shows a connection error and retry action while the query fails, then replaces the error with real application data after the manual retry.
- Test: `frontend/e2e/reliability.spec.ts`

## E2E-021: Mutation failure preserves the user's form

- Status: `Implemented`
- Preconditions: Playwright intercepts the create or rename inventory mutation and returns a server or network failure.
- Steps: Open the corresponding modal, enter a valid name, and submit it while the failure is active.
- Expected result: A `Не получилось` notification is shown, the modal remains open, and the entered value is preserved so the operation can be retried.
- Test: `frontend/e2e/reliability.spec.ts`

## E2E-022: User cannot open another user's inventory

- Status: `Implemented`
- Preconditions: A second signed E2E Telegram user and their inventory are created through the real API; the browser user has no membership in that inventory.
- Steps: Directly open the other user's inventory, product, and member-list URLs.
- Expected result: Protected data is not rendered, and each page shows `Нет доступа к этому инвентарю.` instead of exposing another user's products or members.
- Test: `frontend/e2e/reliability.spec.ts`

## E2E-023: Member sees no owner-only inventory actions

- Status: `Implemented`
- Preconditions: A second signed Telegram user is a `MEMBER` of an inventory owned by another user.
- Steps: Open `/`, inspect the shared inventory actions, and open its member list.
- Expected result: The inventory is marked `Совладелец`; rename, invite, and kick actions are absent, while setting the personal default inventory and leaving the inventory remain available.
- Test: `frontend/e2e/members.spec.ts`

## E2E-024: Owner removes another member

- Status: `Implemented`
- Preconditions: The signed E2E user owns an inventory containing a second member.
- Steps: Open the member list, choose `Исключить` for the other user, review the warning, and confirm.
- Expected result: The confirmation modal closes and the removed member disappears from the list; the owner cannot select their own card for removal.
- Test: `frontend/e2e/members.spec.ts`

## E2E-025: Member leaves a shared inventory

- Status: `Implemented`
- Preconditions: The signed E2E user is a member of another user's inventory and has at least one other accessible inventory.
- Steps: On `/`, choose `Покинуть` for the shared inventory, review the member-specific warning, and confirm.
- Expected result: Only the current user's membership is removed, the shared inventory disappears from their list, and another accessible inventory becomes their default when necessary.
- Test: `frontend/e2e/members.spec.ts`

## E2E-026: Owner creates and shares an invitation link

- Status: `Implemented`
- Preconditions: The signed E2E user owns an inventory and is on its member page.
- Steps: Choose `Пригласить по ссылке`, copy the generated link, and choose `Поделиться`.
- Expected result: A Telegram start link and its expiration date are shown, the copy action changes to `Скопировано`, and sharing opens the correctly encoded Telegram share URL.
- Test: `frontend/e2e/invites.spec.ts`

## Template

### E2E-XXX: Scenario title

- Status: `Planned`
- Preconditions: Required application and data state.
- Steps: User actions and navigation.
- Expected result: Observable product outcome.
- Test: `frontend/e2e/example.spec.ts` or `Not implemented`
