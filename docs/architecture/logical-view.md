# Logical View (4+1 Architectural View Model)

This document details the static structure of the multi-tenant task management backend system, covering both the Domain-Driven Design (DDD) domain model and the technical layer structuring through Clean Architecture.

---

## 1. Introduction and Purpose
The Logical View focuses on the key code abstractions and how the system is modularized to ensure loose coupling, high cohesion, and absolute testability. The design completely decouples business logic from infrastructure details (frameworks, persistence engines, and network protocols).

> **Visual Diagrams Reference:** The corresponding diagrams for this view are modeled and structured in StarUML LD-01-Domain-Entities-Class-Diagram[docs/assets/LD-01-Domain-Entities-Class-Diagram].

---

## 2. Domain Model and Aggregates
The core of the system models actual business rules through **Aggregate Roots** and **Value Objects**, applying strict lifecycle relationship rules:

- **Main Entities and Aggregates:**
  - `Organization`: Tenant root, defining corporate boundaries.
  - `Project`: Isolated workspace confined within an organization.
  - `Task`: Central operational unit endowed with a strict state machine (`BACKLOG` > `DONE`).
  - `User`: Global identity of the authentication system.
  - `Comment`: Contextual collaboration thread associated with a task.

- **Relationship and Composition Patterns:**
  - **Strict Composition (Filled black diamond):** Used where the lifecycle of the parts entirely depends on the container (`Organization` > `Project`, `Project` > `Task`, `Task` > `Comment`). If the container is deleted, its children are removed via cascade.
  - **Loose Association:** Used for cross-references and authorship without strict cascade deletion (`User` > `Comment`, `User` > `Task` as an optional assignment).

---

## 3. Clean Architecture & Package Organization
The source code base and logical components are distributed across three concentric layers, strictly applying the **Dependency Inversion Principle (DIP)** (outer layers know about inner layers, but the core completely ignores the outside):

### 3.1. Core: `domain` (Zero Dependencies)
Contains pure business logic and invariant system rules, free of any external frameworks.
- **`model` / `entity`**: Pure entities (`Task`, `Project`, `Organization`, `User`).
- **`valueobject`**: Immutable objects encapsulating validations and typed identity (`TaskId`, `Email`, `TaskStatus`).
- **`repository`**: Ports or pure persistence interfaces (`TaskRepository`, `OrganizationRepository`).
- **`event`**: Asynchronous domain signals or events (`TaskStatusChangedEvent`).
- **`exception`**: Pure business exceptions (`DomainValidationException`).

### 3.2. Intermediate Layer: `application` (Use Cases)
Orchestrates application flows and translates adapter requests into operations on the domain.
- **`usecases`**: Business interactors (`CreateTaskUseCase`, `UpdateTaskStatusUseCase`, `CreateOrganizationUseCase`).
- **`dto`**: Input and output data transfer objects (`TaskRequestDto`, `TaskResponseDto`).
- **`mapper`**: Transformers adapting domain models to DTOs and vice versa (`TaskDomainMapper`).

### 3.3. Outer Layer: `infrastructure` (Adapters and Frameworks)
Implements concrete technological details to connect the system with the outside world.
- **`persistence`**: Database entities with Hibernate/JPA annotations (`TaskJpaEntity`), repository adapters, and Spring Data JPA connections (`TaskRepositoryAdapter`).
- **`web` / `rest`**: Exposed HTTP controllers and centralized error handling under the RFC 7807 standard (`TaskController`, `GlobalExceptionHandler`).
- **`security`**: Security layer based on Spring Security and JWT token validation filters (`JwtTokenFilter`, `SecurityConfig`).

---

## 4. Canonical Pattern and Modular Scalability
The architecture presented is designed around a **canonical pattern**. This means that the layer structure, adapters, and ports developed for the Tasks and Organizations subsystem are replicated symmetrically and strictly for the rest of the system's aggregates (`User`, `Organization`, `Project`, and `Comment`), ensuring a consistent development experience and predictable maintenance as the platform grows.
The corresponding diagrams for this view are modeled and structured in StarUML LD-02-Clean-Architecture-Layers[docs/assets/LD-02-Clean-Architecture-Layers].