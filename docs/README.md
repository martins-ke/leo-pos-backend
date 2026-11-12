# Leo Documentation

## Product and design

- [System Architecture](architecture/Leo_System_Architecture.svg) — browser, frontend host, optional proxy, REST API, and database.
- [Feature Map](architecture/Leo_Feature_Map.svg) — current product capabilities and integrations that are not currently implemented.
- [Software Requirements Specification](architecture/SRS_Document.md) — current requirements, role boundaries, and explicit roadmap items.
- [System Architecture](architecture/System_Design_and_Architecture.md) — runtime components, authentication, data ownership, and transaction design.
- [Authentication Flow](architecture/Leo_Authentication_Flow.svg) — owner registration, login, and one-time platform-owner setup.
- [POS Checkout Flow](architecture/Leo_POS_Checkout_Flow.svg) — current validation, stock, payment/debt, ledger, and idempotency path.
- [REST API Reference](api/API_REFERENCE.md) — implemented controller routes, access roles, and request conventions.

## Database design

- [Core Database ERD](database/Leo_Core_Database_ERD.svg) — users, shops, products, sales, customers, and debts.
- [Finance and Support ERD](database/Leo_Finance_Database_ERD.svg) — account ledger, expenses, stock movement, audit, and idempotency entities.

## Security, testing, and operations

- [Security and Data Handling](security/SECURITY_AND_DATA_HANDLING.md) — security controls, operator responsibilities, data categories, retention, and known gaps.
- [Testing and Performance Plan](testing/TESTING_AND_PERFORMANCE.md) — test layers, performance targets, repeatable measurement protocol, and baseline record.
- [Production Setup](deployment/production-setup.md) — environment configuration, deployment requirements, and rollout checks.

Requirements describe product behavior; architecture describes implementation boundaries; the API reference is the endpoint catalog. Performance goals are not measurements. Planned features and known implementation gaps are labeled in their respective documents.
