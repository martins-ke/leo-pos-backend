# Leo REST API Reference

| Field | Value |
|---|---|
| Version | 1.0 |
| Status | Endpoint catalog for the current backend |
| Last reviewed | 2026-11-05 |

This catalog is based on the Spring MVC controller mappings. Request/response DTOs and validation annotations in source remain the final contract where a detail is not stated here. All paths are relative to the API origin. The common response envelope is:

```json
{
  "body": {},
  "message": "Result description",
  "success": true,
  "timeStamp": "2026-11-05T00:00:00Z"
}
```

Handled API errors use the same envelope with `success: false` and an appropriate HTTP status. Authenticated requests use `Authorization: Bearer <token>`. JSON request bodies use `Content-Type: application/json`.

## Authentication and platform setup

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/auth/setup/status` | Public | Reports whether one-time platform setup is available and whether a setup key is required. |
| `POST` | `/auth/setup` | Public, guarded by setup policy | Creates the one-time `SUPER_ADMIN`; key or development-loopback check applies. |
| `POST` | `/auth/register` | Public | Creates an active shop owner, the first shop, and its account. Request role cannot elevate the account. |
| `POST` | `/auth/login` | Public | Accepts username or email and password; returns user and JWT on success. |

Email password recovery is not currently exposed.

## Platform administration

All routes require `SUPER_ADMIN`.

| Method | Path | Notes |
|---|---|---|
| `GET` | `/admin/tenants` | Lists owner accounts. |
| `GET` | `/admin/tenants/stats` | Returns platform summary metrics. |
| `PATCH` | `/admin/tenants/{ownerId}/toggle-status` | Toggles the owner's status. |

## Shops and staff

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/shops` | Owner or staff | Lists shops visible to the authenticated user. |
| `POST` | `/shops` | Owner | Creates a shop and its account. |
| `GET` | `/shops/{id}` | Owner | Returns an owned shop. |
| `DELETE` | `/shops/{id}` | Owner | Deletes an owned shop, subject to database relationships. |
| `GET` | `/staff` | Owner | Lists the owner's staff. |
| `POST` | `/staff` | Owner | Creates a staff account. |
| `PATCH` | `/staff/{id}/approve` | Owner | Activates/approves a staff account. |
| `PATCH` | `/staff/{id}/deactivate` | Owner | Toggles staff activation. |
| `PATCH` | `/staff/{id}/shops/{shopId}` | Owner | Assigns staff to a shop. |
| `DELETE` | `/staff/{id}/shops/{shopId}` | Owner | Removes a staff shop assignment. |
| `DELETE` | `/staff/{id}` | Owner | Deletes staff. |

Shop ownership and assignment are validated by the API; possession of an identifier does not grant access.

## Products and inventory

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/products` | Owner or staff | Lists active products; optional `shopId` scopes the result. Unscoped staff requests include only assigned shops. |
| `GET` | `/products/{shopId}` | Owner or assigned staff | Lists active products for one accessible shop. |
| `GET` | `/products/detail/{id}` | Owner or assigned staff | Returns product details when the product's shop is accessible. |
| `POST` | `/products` | Owner | Creates a product in the owner's default/first shop; optional `categoryId` must refer to an active category in that shop. |
| `POST` | `/products/{shopId}` | Owner | Creates a product in the specified owned shop; optional `categoryId` must refer to an active category in that shop. |
| `PUT` | `/products/{id}` | Owner | Updates product details. Omitting `categoryId` preserves the existing assignment; `clearCategory: true` removes it. |
| `DELETE` | `/products/{id}/category` | Owner | Removes a product's category assignment. |
| `PATCH` | `/products/{id}/stock?quantity={positiveNumber}` | Owner | Applies a quantity-only restock adjustment. |
| `DELETE` | `/products/{id}` | Owner | Deactivates a product. |

Category names are unique within each shop. Categories are soft-archived; archiving hides them from active category listings and new assignments while preserving links from existing products.

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/product-categories?shopId={id}` | Owner or assigned staff | Lists active categories for an accessible shop; omit `shopId` to list across accessible shops. |
| `GET` | `/product-categories/suggestions` | Owner or staff | Returns suggested names that owners can use when creating shop-specific categories. |
| `POST` | `/product-categories` | Owner | Creates a category in the owner's default/first shop. |
| `POST` | `/product-categories/{shopId}` | Owner | Creates a category in the specified owned shop. |
| `PUT` | `/product-categories/{id}` | Owner | Renames an active category. |
| `DELETE` | `/product-categories/{id}` | Owner | Soft-archives a category without deleting product assignments. |
| `PATCH` | `/product-categories/{id}/restore` | Owner | Restores an archived category unless another active category has the same name. |

Costed inventory purchases are documented under Finance.

## Sales and dashboard

| Method | Path | Access | Notes |
|---|---|---|---|
| `POST` | `/sales` | Owner or assigned staff | Creates a sale; requires `Idempotency-Key`. Validates current stock, price, totals, payment method and shop assignment. |
| `GET` | `/sales` | Owner or staff | Lists sales newest first; optional `shopId`. Current response is not paginated. **Known gap:** an unscoped staff request currently aggregates the owner's shops without filtering assignments; staff clients should provide `shopId`, and this backend behavior must be fixed before production. |
| `GET` | `/sales/{id}` | Owner or assigned staff | Returns an authorized sale and line items. |
| `GET` | `/dashboard/summary` | Owner | Returns dashboard summary; optional `shopId` scopes it to one owned shop. |

There are no current sale-void, top-products, sales-by-shop, or notification-center endpoints.

## Customers and debts

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/customers?shopId={id}&page={n}&size={n}` | Owner or assigned staff | Lists customers in an accessible shop. |
| `POST` | `/customers` | Owner or assigned staff | Creates a customer; requires `Idempotency-Key`. |
| `PATCH` | `/customers/{id}/eligibility` | Owner | Changes credit eligibility. |
| `DELETE` | `/customers/{id}` | Owner | Deletes a customer only when debt references permit; anonymizes linked historical sale customer details as implemented. |
| `GET` | `/debts?shopId={id}&status=OPEN&page={n}&size={n}` | Owner or assigned staff | Lists shop debts; `status=ALL` includes all retained statuses. |
| `POST` | `/debts` | Owner or assigned staff | Creates an independent customer debt; requires `Idempotency-Key`. |
| `POST` | `/debts/from-sale/{saleId}` | Owner or assigned staff | Converts an eligible customer's paid sale into debt; requires `Idempotency-Key`. |
| `POST` | `/debts/{id}/payments` | Owner or assigned staff | Records a partial/full repayment; requires `Idempotency-Key`. |
| `GET` | `/debts/{id}/payments` | Owner or assigned staff | Lists repayment records while retained. |

Paginated debt/customer collection sizes are bounded in the service; defaults are `page=0` and `size=50`.

## Finance and accounts

All routes in this section require `OWNER`; the shop must belong to the authenticated owner. Mutating POST routes listed below require an `Idempotency-Key`.

| Method | Path | Notes |
|---|---|---|
| `GET` | `/expenses?shopId={id}&page={n}&size={n}` | Lists paginated shop expenses. |
| `GET` | `/expenses/daily-summary?shopId={id}` | Returns the current day's expense total. |
| `POST` | `/expenses` | Records an expense. |
| `GET` | `/expenses/categories?shopId={id}` | Lists expense categories. |
| `POST` | `/expenses/categories` | Creates an expense category. |
| `GET` | `/inventory/withdrawals?shopId={id}&page={n}&size={n}` | Lists paginated owner stock withdrawals. |
| `GET` | `/inventory/withdrawals/daily-summary?shopId={id}` | Returns today's stock-use total. |
| `POST` | `/inventory/withdrawals` | Records owner stock use and its financial/audit effects. |
| `GET` | `/inventory/purchases?shopId={id}&page={n}&size={n}` | Lists paginated stock purchases. |
| `POST` | `/inventory/purchases` | Records a costed stock purchase. |
| `GET` | `/accounts/{shopId}/summary` | Returns shop account and finance summary. |
| `GET` | `/accounts/{shopId}/transactions?page={n}&size={n}` | Lists paginated account transactions. |
| `POST` | `/accounts/{shopId}/capital` | Records opening/additional capital. |
| `POST` | `/accounts/{shopId}/drawings` | Records an owner drawing subject to cash/profit validation. |

## Health and API conventions

| Method | Path | Access | Notes |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Health status; details are hidden. |
| `GET` | `/actuator/health/liveness` | Public | Liveness probe when enabled. |
| `GET` | `/actuator/health/readiness` | Public | Readiness probe when enabled. |

- Mutating operations must validate role, shop scope, and business invariants on the server.
- A POST endpoint requires an idempotency key only where listed or where the controller requires the header; a frontend-generated key alone does not guarantee server-side deduplication.
- Monetary values use decimal JSON numbers. Use the exact field names and validation rules defined by each request record.
- Pagination is endpoint-specific; do not assume every collection is paginated.
- The API does not promise a stable OpenAPI/Swagger schema at this time.
