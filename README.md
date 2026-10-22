# Enterprise Task & Project Manager

Enterprise Task & Project Manager is a multi-tenant B2B SaaS backend for organizing companies, workspaces, projects, tasks, assignments, and team comments. The product aims to provide secure tenant isolation, strict role-based access control, a governed task lifecycle, and deterministic API behavior.

> **Status:** This repository currently contains the product specification, domain model, architecture documentation, and diagrams. The application source code is not implemented yet.

---

## Product Vision

The system will expose a stateless REST API that supports:

- Multi-tenant organizations with strict isolation.
- Organization membership and role management.
- Project workspaces with lifecycle states.
- Task creation, filtering, assignment, and governed state transitions.
- Task comments with author and administrator authorization rules.
- Authentication using JWT access and refresh tokens.
- Secure account deactivation without permanent data loss.
- Deterministic persistence through Flyway migrations.

---

## Planned Technology Stack

The implementation will use the following technologies:

- **Language:** Java
- **Runtime:** Java 21 with Spring Boot
- **Security:** Spring Security with JWT
- **Persistence:** Spring Data JPA and Hibernate
- **Database:** PostgreSQL
- **Database migrations:** Flyway
- **Build system:** Maven
- **Containerization:** Docker and Docker Compose
- **API gateway:** Nginx or Spring Cloud Gateway
- **Testing:** JUnit 5, Mockito and Testcontainers
- **API format:** REST over JSON
- **Error format:** RFC 7807 Problem Details

The target architecture is Clean Architecture together with DDD and a ports-and-adapters boundary. The domain module must remain independent from framework dependencies.

---

## Architecture Principles

- **Domain isolation:** Business rules and aggregates must not depend on Spring, JPA, HTTP, or other infrastructure frameworks.
- **Application use cases:** Business workflows must be represented as explicit use cases and transactions.
- **Infrastructure adapters:** Controllers, repositories, security filters, persistence mappings and external integrations must be isolated in infrastructure modules.
- **Tenant isolation:** Every organization, project, task and membership operation must enforce the current authenticated user's tenant context.
- **Least privilege:** Every operation must require the minimum role and resource scope needed.
- **Immutable audit data:** Deactivation and deletion behavior must preserve required historical information.
- **Deterministic deployment:** Schema changes must be controlled through Flyway migrations.

---

## Organization Name Policy

Organization names are **globally unique across the entire platform**.

This means that no organization name may be duplicated between different companies or tenants, under any circumstances. A unique organization name is a global business identifier and must be enforced through a database-level uniqueness constraint.

The organization name must therefore satisfy all of the following rules:

1. It is required and between 2 and 100 characters.
2. It is normalized before validation and comparison.
3. It is unique globally, not only within a user or tenant.
4. Duplicate values return `409 Conflict`.
5. A changed name must also be checked against the global uniqueness rule.

---

## API Contract

The API is versioned under `/api/v1` and uses semantic resource identifiers:

- `{organizationId}` for organizations.
- `{projectId}` for projects.
- `{taskId}` for tasks.
- `{userId}` for users.
- `{commentId}` for comments.

The API contract is documented in the [Product Requirements Document](docs/PRD.md).

### API Endpoint Summary

| Module | Method | Endpoint | Description | Authorization |
|---|---|---|---|---|
| IAM | `POST` | `/api/v1/auth/register` | Register a user | Public |
| IAM | `POST` | `/api/v1/auth/login` | Authenticate and issue tokens | Public |
| IAM | `POST` | `/api/v1/auth/refresh` | Rotate access and refresh tokens | Valid refresh token |
| IAM | `GET` | `/api/v1/users/me` | Read the authenticated profile | Authenticated user |
| IAM | `PUT` | `/api/v1/users/me` | Update the authenticated profile | Authenticated user |
| IAM | `DELETE` | `/api/v1/users/me` | Deactivate the authenticated account | Account owner |
| Organization | `POST` | `/api/v1/organizations` | Create an organization | Authenticated user |
| Organization | `GET` | `/api/v1/organizations` | List organizations for the authenticated user | Authenticated user |
| Organization | `GET` | `/api/v1/organizations/{organizationId}` | Read organization details | Active organization member |
| Organization | `PUT` | `/api/v1/organizations/{organizationId}` | Update organization metadata | Organization `ADMIN` |
| Organization | `DELETE` | `/api/v1/organizations/{organizationId}` | Delete an organization | Organization `ADMIN` |
| Organization | `POST` | `/api/v1/organizations/{organizationId}/members` | Add an organization member | Organization `ADMIN` |
| Organization | `PUT` | `/api/v1/organizations/{organizationId}/members/{userId}` | Update a member role | Organization `ADMIN` |
| Organization | `DELETE` | `/api/v1/organizations/{organizationId}/members/{userId}` | Remove an organization member | Organization `ADMIN` |
| Project | `POST` | `/api/v1/organizations/{organizationId}/projects` | Create a project | Active organization member |
| Project | `GET` | `/api/v1/organizations/{organizationId}/projects` | List organization projects | Active organization member |
| Project | `GET` | `/api/v1/projects/{projectId}` | Read project details | Active organization member |
| Project | `PUT` | `/api/v1/projects/{projectId}` | Update project metadata | Organization `ADMIN` |
| Project | `PATCH` | `/api/v1/projects/{projectId}/archive` | Archive a project | Organization `ADMIN` |
| Project | `DELETE` | `/api/v1/projects/{projectId}` | Permanently delete a project | Organization `ADMIN` |
| Task | `POST` | `/api/v1/projects/{projectId}/tasks` | Create a task | Active organization member |
| Task | `GET` | `/api/v1/projects/{projectId}/tasks` | List and filter tasks | Active organization member |
| Task | `GET` | `/api/v1/tasks/{taskId}` | Read task details | Active organization member |
| Task | `PUT` | `/api/v1/tasks/{taskId}` | Update general task fields | Author or authorized project member |
| Task | `PATCH` | `/api/v1/tasks/{taskId}/status` | Execute a task state transition | Authorized project member |
| Task | `DELETE` | `/api/v1/tasks/{taskId}` | Permanently delete a task | Authorized project member or administrator |
| Comment | `POST` | `/api/v1/tasks/{taskId}/comments` | Add a task comment | Active organization member |
| Comment | `GET` | `/api/v1/tasks/{taskId}/comments` | List task comments | Active organization member |
| Comment | `DELETE` | `/api/v1/comments/{commentId}` | Delete a comment | Comment author or organization `ADMIN` |

---

## Planned Project Structure

The implementation is a Maven multi-module project. The Maven root is `implementation/ProjectManager/`:

```text
implementation/ProjectManager/
├── pom.xml                         # Maven aggregator and Java 21 configuration
├── core-domain/                    # Pure business rules and domain models
├── app-service/                    # Use cases and application ports
├── infrastructure-adapter/         # REST, security, persistence and adapters
└── app-bootstrap/                  # Spring Boot application entry point
```

Module dependencies point inward: `app-service` depends on `core-domain`, `infrastructure-adapter` implements application ports, and `app-bootstrap` assembles the application. The domain and application modules must not depend on Spring or persistence frameworks.

Organize production packages by feature within each layer (for example, `domain.organization`, `application.organization`, and `infrastructure.persistence.organization`). Keep unit tests in the matching module under `src/test/java`; use plain JUnit/Mockito tests for domain and application behavior, and reserve Spring context/database tests for infrastructure and bootstrap.

The Maven modules are in place; domain models, use cases, and infrastructure adapters will be added incrementally as features are implemented.

---

## Local Development

Run Maven commands from the implementation root:

```powershell
cd implementation\ProjectManager
mvn clean install
mvn -pl app-bootstrap spring-boot:run
```

The application reads `DB_PASSWORD` and `JWT_SECRET` from the environment. Set both in the IntelliJ run configuration for `TaskManagerApplication`; `JWT_SECRET` must be Base64 encoding of at least 32 random bytes. Local development should use Docker Compose for PostgreSQL and optional application dependencies. Flyway migrations run through the application configuration when the database is configured.

Database migrations live in `infrastructure-adapter/src/main/resources/db/migration/` and use Flyway's versioned naming convention, for example `V1__create_initial_schema.sql`. They are applied automatically when `app-bootstrap` starts.

---

## Testing Strategy

The implementation must include:

- Unit tests for domain invariants and use cases.
- Integration tests for Spring Data JPA repositories.
- Security and authorization tests.
- Real PostgreSQL integration tests through Testcontainers.
- Migration validation.
- API contract or controller tests.
- A coverage target of at least 70%.

---

## Important Documentation

- [Product requirements](docs/PRD.md)
- [Architecture overview](docs/architecture/4+1-architecture.md)
- [Domain model](docs/architecture/domain-model.md)
- [Use cases](docs/architecture/use-cases.md)
- [Logical view](docs/architecture/logical-view.md)
- [Process view](docs/architecture/process-view.md)
- [Deployment view](docs/architecture/deployment-view.md)

---

## License

This project is distributed under the MIT License. See [LICENSE](LICENSE) for details.