# Architecture Documentation: 4+1 View Model
# Enterprise Task & Project Manager (B2B SaaS)

This document describes the software architecture of the Enterprise Task & Project Manager platform using **4+1 Architectural View Model**.

---

## 1. The "+1" View: Scenarios (Use Cases)
* **Scenarios Documented:**
  1. **User Registration & Secure Login (`UC-IAM-01`, `UC-IAM-02`):** Validates authentication pipelines, password hashing, and token issuance.
  2. **Multi-Tenant Organization Creation (`UC-ORG-01`):** Validates tenant isolation and automated RBAC binding.
  3. **Strict Task State Machine Transition (`UC-TASK-03`):** Validates domain logic boundaries and invariant enforcement.

---

## 2. Logical View (Vista Lógica)
* **Architecture Style:** Clean Architecture & Domain-Driven Design (DDD) Multi-Module.
* **Key Packages / Bounded Contexts:**
  * **`domain` Layer:** Contains pure business logic, Aggregate Roots (`User`, `Organization`, `Project`, `Task`, `Comment`), Value Objects, and Domain Events. *Zero framework dependencies.*
  * **`application` Layer:** Application services orchestrating use cases, transactional boundaries (`@Transactional`), and mappers.
  * **`infrastructure` Layer:** Adapters, Spring Security configurations, Spring Data JPA repositories, REST Controllers, and global RFC 7807 error handling.
* **Key Design Patterns:**
  * *Repository Pattern* for data persistence abstraction.
  * *State Pattern* / *Domain Invariants* for the Task State Machine.
  * *Strategy Pattern* for authentication filters.

---

## 3. Process View (Vista de Procesos)
* **Runtime Behavior & Concurrency:**
  * **Stateless REST API:** Each authenticated HTTP request carries a JWT access token in the `Authorization: Bearer <access-token>` header, verified by the Spring Security filter chain.
  * **Concurrency Control:** High-contention aggregates (such as `Task` during rapid status changes) utilize **Optimistic Locking** (`@Version` annotations) to prevent lost updates in concurrent environments.
  * **Token Rotation Workflow:** Secure state management for refresh tokens, handling invalidation and replay attack detection asynchronously via database tracking.

---

## 4. Development View (Vista de Desarrollo)
* **Build Tool:** Maven multi-module structure targeting Java 21.
* **Module Breakdown:**
  * `core-domain`: Pure business rules library.
  * `app-service`: Application use cases orchestration.
  * `infrastructure-adapter`: Spring Boot web controllers, persistence adapters, and security components.
* **Quality Assurance & Testing Strategy:**
  * **Unit Tests:** JUnit 5 + Mockito for isolated domain and application services (running in milliseconds without loading the Spring container).
  * **Integration/Container Tests:** **Testcontainers** spinning up real PostgreSQL Docker instances to test repository layers and Flyway migrations automatically.

---

## 5. Physical View (Vista Física / Despliegue)
* **Containerization:** Docker & Docker Compose.
* **Infrastructure Topology:**
  * **App Container:** Spring Boot fat-JAR running inside a Java 21 Docker runtime image.
  * **Database Container:** PostgreSQL container initialized with version-controlled schemas managed by **Flyway**.
* **Environment Boundaries:** Isolated network bridges ensuring the database port is only accessible internally within the Docker Compose network, exposing solely the API gateway port to the host/clients.
