# Process View (4+1 Architectural View Model)

This document details the dynamic behavior, concurrency, thread synchronization, and runtime message flows of the multi-tenant task management backend system.

---

## 1. Introduction and Purpose
While the Logical View addresses the static structure of the code, the Process View focuses on runtime execution. It illustrates how components collaborate to handle critical operations, ensuring transactional integrity, strict state machine enforcement, and robust security validation across boundaries.

> **Visual Diagrams Reference:** The dynamic sequence diagrams for this process are modeled in StarUML (see `docs/assets/PR-01-Task-State-Transition`).

---

## 2. Critical Scenario: Task State Machine Transition
The primary core workflow of the platform involves modifying a task's lifecycle stage (e.g., transitioning from `BACKLOG` to `IN_PROGRESS` or `DONE`). This process traverses all three layers of the architecture to guarantee business invariant protection.

### 2.1. Step-by-Step Runtime Execution Flow
1. **HTTP Request & Security Interception:**
   - The `Client` issues a `PATCH /api/v1/tasks/{id}/status` HTTP request carrying a Bearer JWT.
   - The `JwtTokenFilter` (Infrastructure) intercepts the request, validates the cryptographic signature, and populates the Security Context.
2. **Web Controller Delegation:**
   - The request reaches the `TaskController`, which maps the incoming payload into an application-level command/request object and invokes the interactor.
3. **Application Orchestration & Retrieval:**
   - The `UpdateTaskStatusUseCase` coordinates the transaction. It calls the persistence port (`TaskRepository`) to fetch the target `Task` aggregate root by its identifier.
4. **Domain Invariant & State Machine Evaluation:**
   - Once the pure `Task` entity is loaded into memory, the use case invokes its domain method (`task.changeStatus(newStatus)`).
   - The `Task` entity interacts with its `TaskStatus` value object to validate whether the requested transition complies with the business rules of the state machine. If an invalid transition is attempted, a `DomainValidationException` is thrown immediately, bypassing persistence.
5. **Transactional Persistence:**
   - If validation succeeds, the use case delegates the persistence operation to the `TaskRepositoryAdapter`.
   - The adapter translates the pure domain model into a `TaskJpaEntity` and executes the update command via Spring Data JPA against the relational database.
6. **Response Mapping:**
   - The database acknowledges the commit, the transaction finalizes successfully, and a `TaskResponseDto` is returned through the layers back to the HTTP client.

---

## 3. Canonical Process Replication for Other Aggregates
The execution flow modeled for the task state transition acts as the **canonical process pattern** for the system. The same multi-layered lifecycle—comprising security filtering, REST controllers, application use cases, pure domain logic execution, and repository adapters—applies symmetrically and identically to other system aggregates and operations, such as organization provisioning (`Organization`), workspace creation (`Project`), and user authentication (`User`).

---

## 4. Concurrency and Exception Handling in Runtime
- **Thread Isolation:** Each incoming HTTP request runs within an isolated thread managed by the embedded servlet container (Tomcat), ensuring thread-safe processing of concurrent modifications.
- **Uniform Error Propagation:** Any exception triggered during the process (such as a domain rule violation or concurrent modification conflict) is caught by the `GlobalExceptionHandler`, translating it deterministically into an RFC 7807 problem details response format.