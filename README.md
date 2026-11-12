# Leo Backend

Leo's Spring Boot REST API for retail point of sale, inventory, customer, staff, and shop-finance workflows.

## Requirements

- Java 17 or newer.
- MySQL for local development and production. Automated tests use the configured H2 test database.
- Node.js and the frontend dependencies are required only when running the separate frontend project.

## Run locally

Configure a local MySQL database, then start the API from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The default server port is `8181`. Development defaults are provided by `application.properties`; use a dedicated local database account rather than production credentials.

Run backend tests:

```powershell
.\mvnw.cmd test
```

The production profile must be explicitly configured with secrets and database settings. Do not use development defaults or Hibernate schema updates for production. See [Production Setup](docs/deployment/production-setup.md).

## Documentation

- [Documentation index](docs/README.md)
- [Software Requirements Specification](docs/architecture/SRS_Document.md)
- [System Architecture](docs/architecture/System_Design_and_Architecture.md)
- [REST API Reference](docs/api/API_REFERENCE.md)
- [Security and Data Handling](docs/security/SECURITY_AND_DATA_HANDLING.md)
- [Testing and Performance Plan](docs/testing/TESTING_AND_PERFORMANCE.md)
- [Production Setup](docs/deployment/production-setup.md)

The React/Vite frontend is maintained in the companion Leo frontend project.
