# Software Requirements Specification
## Leo — Retail Point of Sale and Shop Operations

| Field | Value |
|---|---|
| Version | 2.0 |
| Status | Current product baseline; roadmap items are labeled |
| Last reviewed | 2026-11-05 |
| Audience | Product, engineering, QA, operations |

This specification distinguishes implemented behavior from planned work. “Current” describes the application represented by the frontend and backend code; it is not a claim that the system has passed production or performance acceptance. For deployment and API details, see [System Architecture](System_Design_and_Architecture.md), [API Reference](../api/API_REFERENCE.md), and [Production Setup](../deployment/production-setup.md).

## 1. Purpose and scope

Leo is a browser-based retail operations application for shop owners and staff. It combines point-of-sale checkout, product and stock management, sales history, customer records, debt tracking, and shop-level financial records. Shop owners can manage multiple outlets, while a separately provisioned platform administrator can view and manage owner accounts.

The current system consists of a React single-page frontend and a Spring Boot REST API backed by a relational database. It is not currently a Progressive Web App and does not provide an M-Pesa payment gateway, subscription billing, email-based password recovery, or a customer-facing storefront.

### 1.1 User roles

| Role | Current responsibilities |
|---|---|
| `SUPER_ADMIN` | View platform tenant statistics and owner accounts; toggle an owner account's status. |
| `OWNER` | Manage owned shops, staff, products, sales, customers, and shop financial workflows. |
| `STAFF` | Use permitted sales, customer, and debt workflows for explicitly assigned shops. |

The API is authoritative for authentication, role checks, shop ownership, and staff-to-shop assignment. Frontend route guards improve navigation but do not grant API access. Unscoped staff product/sales list requests currently require a backend assignment-filter fix before production; see [Security and Data Handling](../security/SECURITY_AND_DATA_HANDLING.md).

### 1.2 Terms

- **Shop / outlet:** A business location owned by an owner account.
- **Tenant:** An owner account and the resources it owns. This is a business boundary, not a separate username namespace.
- **Sale:** A receipt and its line items, recorded for one shop and one actor.
- **Debt:** An outstanding customer amount. It can be entered independently or created from a credit sale.
- **Account ledger:** A shop-scoped record of cash inflows and outflows; it is not itself a profit statement.
- **Idempotency key:** A client-supplied key used by supported mutations to prevent a retried request from posting the same operation twice.

## 2. Product boundaries

### 2.1 Current capabilities

1. Platform owner setup, owner registration, login, and stateless bearer-token authentication.
2. Owner shop creation and listing; shop-scoped access for owners and assigned staff.
3. Product catalogue and stock changes, including quantity-only restocking and costed stock purchases.
4. Sale checkout, receipt data, sales history, inventory deduction, and supported payment-method recording.
5. Staff creation, shop assignment, approval/status management, and removal.
6. Customer records and eligibility controls; independent debt records, credit sales, debt conversion, and repayments.
7. Expenses, personal stock withdrawals, shop account transactions, capital, drawings, and dashboard summaries.
8. Platform-level owner account listing, summary metrics, and account status changes.
9. Financial activity auditing and scheduled deletion of settled debt-specific records after the configured retention period.

### 2.2 Explicitly not current capabilities

These are roadmap items or operational work, not capabilities to assume from the current UI/API:

- M-Pesa STK Push or other payment-provider processing. A payment method can be recorded; this is not payment confirmation from a provider.
- Subscription plans, plan enforcement, or billing.
- Email-driven password reset/recovery.
- A notification-center API, browser push notifications, or promised 45-second notification polling.
- Product category management endpoints or category-specific inventory reporting.
- A full sales analytics API for top-selling products or sales-by-shop reports. The current dashboard provides its implemented summary only.
- An automatic database migration runner.

## 3. Functional requirements

“Current” requirements below describe intended observable behavior that exists in the present implementation. Items marked “Planned” must not be presented as already supported.

### 3.1 Authentication and account lifecycle

- **FR-AUTH-1 — Platform setup (Current):** Expose setup availability and permit creation of one `SUPER_ADMIN`. When a setup key is configured, require it. Without a key, allow setup only from loopback in the development profile. Refuse setup after a platform owner exists.
- **FR-AUTH-2 — Owner registration (Current):** Public registration always creates an active `OWNER`, provisions the initial shop and its account ledger, and ignores any requested role elevation.
- **FR-AUTH-3 — Staff provisioning (Current):** Owners create staff accounts and assign staff to shops. Public registration cannot create staff or platform administrators.
- **FR-AUTH-4 — Login (Current):** Authenticate username or email plus password; only active accounts may use protected services. Reject invalid credentials, pending/deactivated accounts, and staff whose owner account is inactive.
- **FR-AUTH-5 — Access token (Current):** Return a signed, expiring JWT after successful login. Protected API calls require `Authorization: Bearer <token>`. The API reloads the account and checks its active status before establishing the authenticated principal.
- **FR-AUTH-6 — Password recovery (Planned):** Provide an email-based, expiring reset flow only after delivery, token lifecycle, abuse controls, and recovery behavior are implemented and tested.

### 3.2 Shop and access scope

- **FR-SHOP-1 (Current):** An owner can create, list, inspect, and delete their own shops.
- **FR-SHOP-2 (Current):** Owners can access only their own shops. Staff can access only shops owned by their creator and explicitly assigned to them.
- **FR-SHOP-3 (Required):** Products, sales, customers, debts, and account activity must be scoped to a shop and checked server-side. The unscoped staff product/sales list paths are a known implementation gap and must be fixed before production.
- **FR-SHOP-4 (Planned):** Subscription tiers, billing, and plan-based shop limits are not enforced in the current system.

### 3.3 Products and stock

- **FR-INV-1 (Current):** Owners can create, update, deactivate, and list products; staff can read active products for shops to which they are assigned.
- **FR-INV-2 (Current):** Product records include pricing, unit, quantity, and minimum stock level. Product/category taxonomy management is not exposed as a current API capability.
- **FR-INV-3 (Current):** Owners can make quantity-only stock adjustments or record a stock purchase with cost and corresponding shop-account outflow. These operations must be distinguishable in financial views.
- **FR-INV-4 (Current):** A sale must reject missing, inactive, wrong-shop, repriced, or insufficient-stock line items. The server calculates the total from current product prices and rejects a client total that differs.
- **FR-INV-5 (Planned):** Automated low-stock notification delivery and notification-center workflows are not currently specified as implemented.

### 3.4 Sales and checkout

- **FR-POS-1 (Current):** An owner or assigned staff member can submit a non-empty cart for an accessible shop. Products cannot be duplicated within one request.
- **FR-POS-2 (Current):** Validate prices, quantities, shop membership, total, and payment rules on the server. Cash tender must cover the total; non-cash tender must equal the total. Supported recorded methods include `CASH`, `MPESA`, `CARD`, `BANK_TRANSFER`, and `DEBT`.
- **FR-POS-3 (Current):** Persist the sale and line-item snapshots, deduct stock, write the relevant cash-ledger or debt record, and audit the action within a database transaction.
- **FR-POS-4 (Current):** Credit sales require an eligible customer and create linked debt. A customer may also be linked to a non-credit sale for reference.
- **FR-POS-5 (Current):** Require an idempotency key for checkout so client retries do not duplicate a completed sale.
- **FR-POS-6 (Current):** Owners and assigned staff can read sales within authorized shop scope. A printable receipt is a frontend concern based on returned sale data.
- **FR-POS-7 (Planned):** Sale voiding is not part of the current API.

### 3.5 Staff

- **FR-STAFF-1 (Current):** Owners can create and list their staff, approve accounts, toggle activation, assign or remove shop assignments, and delete staff accounts.
- **FR-STAFF-2 (Current):** Staff must be active and assigned to the requested shop. Deactivation or owner suspension prevents successful staff login.
- **FR-STAFF-3 (Current):** Only owners can manage staff and shop assignments.

### 3.6 Customers and debts

- **FR-CUST-1 (Current):** Owners and assigned staff can create and list customers within an accessible shop. New customers are not credit-eligible by default.
- **FR-CUST-2 (Current):** Only owners can change credit eligibility or delete a customer. Customer deletion is rejected when debt records still reference the customer; historical sales are anonymized as implemented.
- **FR-CUST-3 (Current):** An eligible customer can have a debt recorded directly or through a credit sale. A paid sale can also be converted into a debt; the conversion changes the sale payment state and posts the corresponding ledger reversal.
- **FR-CUST-4 (Current):** Owners and assigned staff can record positive partial or full debt repayments that do not exceed the outstanding balance. Persist repayment, debt balance, account inflow, and audit information atomically.
- **FR-CUST-5 (Current):** Settled debts are excluded from open-debt views. Debt-specific records are purged after the configured retention interval (currently scheduled with a seven-day retention); repayment cash effects remain in the account ledger without a customer/debt reference.

### 3.7 Finance and dashboard

- **FR-FIN-1 (Current):** Owners can record and list expenses and expense categories for a shop.
- **FR-FIN-2 (Current):** Owners can record personal stock withdrawals. Validate and deduct stock, record its cost as a non-cash owner-use expense, and record an audit event atomically.
- **FR-FIN-3 (Current):** Each shop has a cash ledger supporting sales, repayments, capital, stock purchases, expenses, and drawings. Corrections should use explicit reversing entries rather than silently rewriting history.
- **FR-FIN-4 (Current):** Owners can review shop account summaries and paginated transactions, add capital, and record drawings. A drawing must not exceed available cash; warn and require explicit confirmation where the application identifies a drawing greater than remaining profit.
- **FR-FIN-5 (Current):** Present operating profit, profit after drawings, and ledger cash as distinct measures. Debt principal, repayments, capital, stock purchases, and drawings must not be mislabeled as sales revenue or operating expenses.
- **FR-DASH-1 (Current):** Owners can view the implemented dashboard summary, optionally scoped to a shop. Do not promise unimplemented top-products, shop-comparison, or notification endpoints.
- **FR-AUD-1 (Current):** Record actor, shop, action, resource, and timestamp for supported financial/security-sensitive mutations.

### 3.8 API and user feedback

- **FR-API-1 (Current):** Return API results in the common response envelope (`body`, `message`, `success`, `timeStamp`) and provide actionable validation, authentication, authorization, and not-found errors.
- **FR-API-2 (Current):** Paginate financial/customer collections where the endpoint implements pagination; enforce a maximum page size in the service.
- **FR-UX-1 (Current):** The frontend must provide clear loading, success, empty, and error feedback for asynchronous operations and must not report success when the API fails.
- **FR-UX-2 (Current):** Keep public routes usable without a token and protect authenticated routes according to the current user role; the backend remains the security boundary.

## 4. Non-functional requirements

These performance values are **acceptance targets**, not measured results. Record a reproducible baseline and actual results separately in the [Testing and Performance Plan](../testing/TESTING_AND_PERFORMANCE.md).

### 4.1 Performance

- **NFR-PERF-1:** At the 75th percentile of real mobile visits, target Core Web Vitals of LCP ≤ 2.5 seconds, INP ≤ 200 milliseconds, and CLS ≤ 0.1.
- **NFR-PERF-2:** On a documented staging-like environment and representative seeded dataset, target p95 ≤ 500 ms for common read/write API requests, excluding health checks and large report/export operations.
- **NFR-PERF-3:** Target p95 ≤ 1 second for a successful sale checkout request under the same test conditions.
- **NFR-PERF-4:** Measure cold and warm page load, JavaScript/CSS transfer size, API latency, and checkout latency. State device, network, dataset, concurrency, and cache conditions with each result.

### 4.2 Security and privacy

- **NFR-SEC-1:** Hash passwords with BCrypt; never return password hashes in API responses.
- **NFR-SEC-2:** Sign bearer tokens with a server-only secret of at least 32 UTF-8 bytes and enforce token expiry.
- **NFR-SEC-3:** Require authentication by default except for explicitly public authentication/setup and health routes. Enforce roles and shop scope server-side.
- **NFR-SEC-4:** In production, use HTTPS, exact configured CORS origins, verified database TLS, non-root database credentials, and production configuration that does not automatically alter the schema.
- **NFR-SEC-5:** Keep JWT/setup/database credentials out of source control, frontend bundles, and logs. Secure browser token storage against XSS as a continuing hardening concern.
- **NFR-PRIV-1:** Collect only the account, shop, staff, customer, sales, and financial data needed for operations. Restrict access by role and shop.
- **NFR-PRIV-2:** Keep retention/deletion behavior transparent. Debt-specific records have the current seven-day post-settlement retention; other records follow the database backup and retention policy selected by the operator.

### 4.3 Reliability and data integrity

- **NFR-REL-1:** A checkout or financial mutation must either commit all related records or none.
- **NFR-REL-2:** Use idempotency protection on supported high-impact mutations and validate balances, prices, and stock on the server.
- **NFR-REL-3:** Production schema changes must use reviewed, versioned migrations before deployment; `ddl-auto=update` is not a production migration strategy.
- **NFR-REL-4:** Provide health and readiness/liveness signals and maintain tested, encrypted database backups before public launch.

### 4.4 Accessibility and compatibility

- **NFR-UX-1:** The interface should be responsive for mobile and desktop widths and avoid horizontal overflow in primary workflows.
- **NFR-UX-2:** Target WCAG 2.2 AA for semantic structure, keyboard access, visible focus, contrast, and form feedback; a full accessibility audit remains to be recorded.

## 5. Acceptance and traceability

Requirements are accepted through API/controller and service tests, frontend workflow checks, and staging validation. Performance acceptance uses the methodology and results table in the testing/performance plan. A feature described as planned is not accepted until its API/UI behavior, tests, security controls, and operational requirements are documented and implemented.

## 6. Related documentation

- [System Architecture](System_Design_and_Architecture.md)
- [API Reference](../api/API_REFERENCE.md)
- [Security and Data Handling](../security/SECURITY_AND_DATA_HANDLING.md)
- [Testing and Performance Plan](../testing/TESTING_AND_PERFORMANCE.md)
- [Production Setup](../deployment/production-setup.md)
