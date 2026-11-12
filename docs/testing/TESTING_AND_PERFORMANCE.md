# Testing and Performance Plan

| Field | Value |
|---|---|
| Version | 1.0 |
| Status | Test guidance and performance targets; local frontend bundle comparison recorded |
| Last reviewed | 2026-11-07 |

This plan describes how to validate Leo and how to measure its performance. The targets below are goals, not observed results. A local frontend bundle comparison is recorded below; API load and post-change browser experience metrics have not been measured.

## 1. Test layers

| Layer | Responsibility | Current tools |
|---|---|---|
| Backend unit tests | Validate business calculations and isolated service behavior. | JUnit / Spring test support |
| Backend integration tests | Validate HTTP routes, authorization, persistence mappings, and workflows. | Spring Boot Test, MockMvc, H2 |
| Frontend static checks | Detect syntax, lint, and production-bundle problems. | ESLint, Vite build |
| Browser workflow checks | Validate responsive behavior, accessibility, and complete owner/staff journeys. | Manual browser checks; no committed frontend E2E runner is currently configured. |
| Performance tests | Measure browser experience, API latency, and checkout under documented conditions. | Browser DevTools/Lighthouse and a controlled API load-test tool selected for the run. |

## 2. Repeatable local checks

Run commands from the relevant project directory:

```powershell
# Backend, using the checked-in Maven wrapper and H2 test configuration
.\mvnw.cmd test

# Frontend
npm run lint
npm run build
```

On Windows PowerShell, use `npm.cmd` if script execution policy blocks `npm.ps1`. The frontend currently has no dedicated unit-test or browser-test command. Do not treat a successful build or lint run as proof of end-to-end correctness.

For each behavioral change, test the smallest relevant service/controller or page workflow. Before release, cover at least:

- Platform setup, owner registration, login, invalid/expired token handling, and deactivated users.
- Owner/staff role boundaries and shop assignment checks, including cross-owner identifiers and absent optional shop filters.
- Product creation/stock mutation, checkout validation, stock deduction, retries with the same idempotency key, and receipt display.
- Customer eligibility, debt creation/conversion/repayment, settlement and scheduled retention cleanup.
- Expense, stock purchase/withdrawal, account ledger, drawing confirmation, and dashboard calculations.
- Loading, empty, validation, network, authorization and server-error UI states at desktop and mobile widths.
- Keyboard navigation, focus visibility, labels, contrast, zoom/reflow, and screen-reader semantics for critical forms.

## 3. Performance acceptance targets

### Real-user experience

Target the 75th percentile on mobile for:

| Metric | Target |
|---|---:|
| Largest Contentful Paint (LCP) | ≤ 2.5 seconds |
| Interaction to Next Paint (INP) | ≤ 200 milliseconds |
| Cumulative Layout Shift (CLS) | ≤ 0.1 |

These are field targets. A single Lighthouse run does not establish real-user p75. Until real-user monitoring is deployed, report lab measurements separately and mark field metrics “not measured.”

### API experience

On a documented staging-like environment and representative seeded data, target:

| Request group | Target |
|---|---:|
| Common read/write API operations | p95 ≤ 500 ms |
| Successful sale checkout | p95 ≤ 1 second |

Exclude health checks from business-operation latency. Report dashboard/reporting requests separately. Never run load tests against production without explicit authorization and a safe test plan.

## 4. Browser measurement protocol

1. Build the production bundle and serve the built output, not the Vite development server.
2. Record the tested commit, browser/version, operating system, viewport, CPU/network throttling, API origin, and whether the cache is cold or warm.
3. Use a representative mobile viewport and a desktop viewport. Test the public landing page and authenticated owner POS/inventory workflows.
4. Run at least five repeatable cold-load lab runs per route and report median and range, not only the best result.
5. Record LCP, CLS, Total Blocking Time (as a lab diagnostic, not as INP), initial JavaScript/CSS transfer, total transferred bytes, and console/network errors.
6. Exercise important interactions (navigation, product search/add-to-cart, cart updates, checkout, and data filtering); record the slowest meaningful interaction and its trace.
7. Use field INP only when actual user interaction data is collected. Do not present lab TBT as INP.

## 5. API measurement protocol

1. Use a staging-like API and database with no production customer data. Seed and document enough shops, users, products, sales, customers, and ledger entries to represent the intended workload.
2. Record API/DB versions, host CPU and memory, connection-pool settings, dataset size, network location, and application commit.
3. Warm up the application, then run a fixed, documented request mix with at least 10 concurrent virtual users for two minutes. Keep authentication, shop scope, and payloads realistic.
4. Measure latency percentiles (p50/p95/p99), throughput, error rate, database query time, and resource utilization. Run checkout as a distinct scenario with stock contention and safe test records.
5. Run the same workload at least three times. Report each run and the median; do not omit failures or outliers without explanation.
6. Compare measurements against the targets in this document. Profile the slow path before changing code, then repeat the same test to verify the result.

## 6. Frontend bundle snapshot

A local production-build comparison was captured on 2026-11-07 before and after route-level lazy loading. The earlier build loaded all page code and styles with the initial route; the updated build loads the shared entry and landing-page chunks, with other page chunks deferred until navigation.

| Build snapshot | Initial landing-page JavaScript | Initial landing-page CSS |
|---|---:|---:|
| Before route splitting | 508.63 kB (142.27 kB gzip) | 105.53 kB (18.60 kB gzip) |
| After route splitting | 329.40 kB (101.54 kB gzip) | 44.63 kB (10.37 kB gzip) |

These are sums of the relevant Vite build artifacts, not a network-throttled transfer measurement. The initial JavaScript artifacts are 35% smaller uncompressed and 29% smaller gzip; initial CSS artifacts are 58% smaller uncompressed and 44% smaller gzip. The landing page and login route were verified in the local production preview. LCP, CLS, and throttled-network results have not been re-measured after this change; no API or checkout load test was run.

## 7. Baseline record

The frontend bundle comparison above is the current local baseline. Fill remaining fields during the next performance-measurement step:

| Date / commit | Environment and workload | Browser metrics | API / checkout latency | Bundle transfer | Errors / notes |
|---|---|---|---|---|---|
| 2026-11-07 / local worktree | Windows, Node 24, Vite production build; landing page and login route | LCP / CLS after route split: not measured | Not measured; staging target unavailable | Build artifact sizes in section 6; network transfer pending | Production preview used a local non-routable API target; no production API traffic sent. |

Do not label unmeasured values as “fast,” “optimized,” or compliant with the targets. If the measured baseline misses a target, record the bottleneck, owner, and follow-up measurement.

## Related documentation

- [Software Requirements Specification](../architecture/SRS_Document.md)
- [System Architecture](../architecture/System_Design_and_Architecture.md)
- [API Reference](../api/API_REFERENCE.md)
- [Production Setup](../deployment/production-setup.md)
