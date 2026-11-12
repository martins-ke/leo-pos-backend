# Production deployment

See the [security and data handling guidance](../security/SECURITY_AND_DATA_HANDLING.md), [API reference](../api/API_REFERENCE.md), and [testing/performance plan](../testing/TESTING_AND_PERFORMANCE.md) before rollout.

## Required backend environment

Run the API with `SPRING_PROFILES_ACTIVE=prod`. Provide these values through the hosting platform's secret/configuration manager; do not commit them:

- `JWT_SECRET`: a randomly generated secret with at least 32 UTF-8 bytes. Rotating it invalidates all existing login tokens.
- `LEO_CORS_ALLOWED_ORIGINS`: comma-separated exact browser origins, including scheme and port where applicable (for example `https://pos.example.com`). Do not use `*`.
- `SPRING_DATASOURCE_URL`: the production MySQL JDBC URL and database name. Configure the database certificate/CA trust needed for Connector/J `sslMode=VERIFY_IDENTITY`.
- `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`: a dedicated application user limited to the Leo database. Do not use `root`.
- `LEO_PLATFORM_SETUP_KEY`: a strong one-time setup key if the platform-owner account has not yet been provisioned.

The production profile requires JWT and CORS settings and has no datasource credential/URL defaults. It enforces MySQL server certificate and hostname verification, disables SQL logging, and sets Hibernate schema handling to `validate`. Provision the database schema with reviewed, versioned migrations before deploying; the project does not currently include an automatic migration runner. Do not change production schema handling to `update`.

## Frontend/API routing

Set `VITE_API_BASE_URL` at frontend build time to the API's public base URL. The checked-in production value is empty, which is appropriate only when the frontend's origin reverse-proxies API requests to the backend. If the API uses a separate origin, set that exact HTTPS URL and include the frontend origin in `LEO_CORS_ALLOWED_ORIGINS`.

Terminate HTTPS at the hosting platform or trusted reverse proxy, redirect HTTP to HTTPS, and do not expose the database publicly. Keep setup keys and database/JWT secrets out of frontend build variables.

## Health and rollout

Only `/actuator/health` is exposed; details are hidden. Configure the platform liveness/readiness probes to use `/actuator/health/liveness` and `/actuator/health/readiness` over the internal service connection.

Before public launch:

1. Apply and verify database migrations against a production-like database.
2. Deploy with the production profile and confirm health reports `UP`.
3. Verify login, owner/cashier authorization, platform setup state, CORS from the production origin, and rejected requests from an unlisted origin.
4. Configure automated encrypted database backups and perform a restore test.
5. Configure centralized logs, uptime/error alerts, and an incident/rollback procedure.
