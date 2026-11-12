# Security and Data Handling

| Field | Value |
|---|---|
| Version | 1.0 |
| Status | Current controls and production checklist |
| Last reviewed | 2026-11-05 |

This document records current application controls and operator responsibilities. It is not a legal privacy notice, compliance certification, or substitute for a deployment-specific threat assessment.

## 1. Security boundaries

- The backend is the authorization boundary. Frontend route guards are a navigation aid only.
- An authenticated principal is loaded from a signed bearer JWT and the current user record. Shop ownership and staff assignment are checked server-side.
- `SUPER_ADMIN`, `OWNER`, and `STAFF` are separate roles. Financial operations and platform administration are role-restricted.
- Public endpoints are limited by Spring Security to `/auth/**`, `/error`, and actuator health. Platform setup adds one-time/key/loopback checks in the service.
- Supported high-impact mutations use server-side idempotency records, validation, and database transactions.

## 2. Authentication and secrets

- Passwords are stored as BCrypt hashes (strength 10); raw passwords must not be logged or returned.
- JWTs are signed with a server-side HMAC key of at least 32 UTF-8 bytes and expire after a configurable interval (seven days by default). In production provide a high-entropy `JWT_SECRET` using the host's secret manager.
- The frontend currently stores the JWT and user record in `localStorage`. This is convenient for the SPA but exposes the token to successful same-origin script injection. Prevent XSS, keep dependencies reviewed, avoid unsafe HTML, and consider moving to a secure HttpOnly cookie as a future architecture change.
- Configure `LEO_PLATFORM_SETUP_KEY` before first setup outside local development. Use it once, verify the `SUPER_ADMIN` was created, then remove the setup key from the runtime environment.
- Never put database credentials, JWT keys, platform setup keys, or private credentials in `VITE_*` variables, source control, browser storage, or client-visible logs.

## 3. Transport, CORS, and database

Production operators must:

1. Serve the frontend and API over HTTPS and redirect HTTP to HTTPS.
2. Set `LEO_CORS_ALLOWED_ORIGINS` to exact trusted browser origins; do not use `*`.
3. Use a dedicated least-privilege MySQL account, verified database TLS, and a private database network.
4. Run with the `prod` profile, `SPRING_DATASOURCE_*`, and `JWT_SECRET` configured.
5. Apply reviewed schema migrations before rollout. Production Hibernate schema handling is validation, not a migration mechanism.
6. Configure encrypted backups, access-controlled logs, health probes, alerting, and a rehearsed restore/rollback procedure.

See [Production Setup](../deployment/production-setup.md) for required configuration and rollout steps.

## 4. Data handled

| Data group | Examples | Primary use and access |
|---|---|---|
| Account and authentication | Username, optional email, password hash, role, status | Login and account administration; password hash is write-only in serialized user data. |
| Shop and staff | Shop identity/location, staff identity, assignments, status | Shop operations; owner scope and staff assignment restrict access. |
| Catalogue and transactions | Product names/prices/stock, sales and line items, receipts | Checkout, inventory and sales history; shop-scoped. |
| Customer and credit | Customer name/contact, eligibility, debts and repayments | Customer service and credit tracking; shop-scoped and role-restricted. |
| Financial and audit records | Expenses, stock purchases/withdrawals, account entries, actor/action timestamps | Cash/profit reporting and accountability; owner-scoped. |

Production operators should document their lawful basis, customer notice/consent where required, data-subject request handling, breach response contacts, and backup retention for their deployment. Do not collect customer details that are not needed for the chosen workflow.

## 5. Retention and deletion

- Settled debt records, repayment details, and debt-specific audit references are scheduled for deletion seven days after settlement; an unlinked shop-account cash entry remains for reconciliation.
- Customer deletion is restricted while debt references exist. Where permitted, linked historical sales retain transaction values while customer-identifying details are anonymized.
- Other business records are not automatically expired by the described policy. Define and communicate a retention and backup-expiry period before production use.
- Backups can retain records after application-level deletion until their configured expiry. Document and test that lifecycle separately.

## 6. Known limitations and release checks

- Browser token storage in `localStorage` increases the impact of an XSS defect; CSP and a cookie-based token strategy should be evaluated before a higher-risk deployment.
- Product and sales collection requests now scope staff results to shops assigned to that staff member, including requests without a `shopId`. Backend regression tests cover these unfiltered collection paths; continue verifying shop assignments and cross-shop access before rollout.
- Database and unhandled server errors return generic client messages while diagnostic details are logged server-side. Production operators must keep those logs access-controlled and protected.
- Authentication rate limiting, automated dependency/security scanning, independent penetration testing, formal incident response, and an external privacy notice are not evidenced as configured by these application repositories.
- Payment method labels do not imply a provider integration or confirmation. No payment credentials or card data should be stored in Leo.

## 7. Security verification

Before each production rollout, verify at minimum:

- Invalid, expired, and revoked/deactivated-account tokens cannot access protected operations.
- Each role is denied disallowed routes, and cross-owner/cross-shop resource identifiers do not disclose data.
- Staff collection and detail paths honor explicit shop assignments, including requests that omit optional shop filters; regression tests cover unfiltered product and sales collections.
- Database and unhandled server responses do not disclose SQL, driver, or internal exception details.
- Setup is one-time, requires the configured key outside local development, and cannot be invoked from untrusted origins/networks.
- CORS rejects unlisted origins; production transport and database TLS are verified.
- Secrets are absent from built frontend assets, repository history, logs, and support bundles.
- Backup restore, debt-retention cleanup, and audit/ledger reconciliation have been tested.

## Related documentation

- [Software Requirements Specification](../architecture/SRS_Document.md)
- [System Architecture](../architecture/System_Design_and_Architecture.md)
- [API Reference](../api/API_REFERENCE.md)
- [Production Setup](../deployment/production-setup.md)
