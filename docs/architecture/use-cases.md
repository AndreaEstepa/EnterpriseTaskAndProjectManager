# Use Cases Documentation (The "+1" View)
This document details the functional use cases, actor interactions, and structural inheritance rules for the multi-tenant task management platform. It serves as the primary scenario driver for the 4+1 Architectural View Model.
---

## 1. System Actors & Generalization Hierarchy

- **Unauthenticated User (`Visitor`):** An unverified user interacting with public endpoints (registration and login).
- **Authenticated User:** Any user with a valid JWT session capable of managing profile data and viewing personal organizations.
- **Organization Member (`MEMBER`):** Inherits from *Authenticated User*. A workspace participant capable of creating projects, writing tasks, executing state machine transitions, and collaborating via comments.
- **Organization Admin (`ADMIN`):** Inherits from *Organization Member*. A tenant administrator with full control over corporate boundaries, role assignments, member management, project archives, and administrative deletions.

---

## 2. Use Case Modules Overview

> **Visual Diagrams Reference:** The dynamic sequence diagrams for this process are modeled in StarUML [UC-01-IAM-Security](../assets/UC-01-IAM-Security.jpg), [UC-02-Organizations-RBAC](../assets/UC-02-Organizations-RBAC.jpg), [UC-03-Projects-Tasks-Domain](../assets/UC-03-Projects-Tasks-Domain.jpg).

### 2.1 IAM & Security Module (`UC-01-IAM-Security`)
- **`UC-IAM-01`**: Register User Account (`POST /api/v1/auth/register`) — Validates strict password complexity and email uniqueness, applying BCrypt hashing.
- **`UC-IAM-02`**: Authenticate User & Issue Tokens (`POST /api/v1/auth/login`) — Validates credentials, issuing short-lived JWT Access Tokens and persistent Refresh Tokens.
- **`UC-IAM-03`**: Rotate Refresh Token (`POST /api/v1/auth/refresh`) — Rotates the refresh token and revokes the previous token when replay detection is triggered.
- **`UC-IAM-04`**: Read User Profile (`GET /api/v1/users/me`) — Returns the authenticated user's profile.
- **`UC-IAM-05`**: Update User Profile (`PUT /api/v1/users/me`) — Updates the authenticated user's profile attributes.
- **`UC-IAM-06`**: Deactivate User Account (`DELETE /api/v1/users/me`) — Deactivates the account and revokes all active sessions.

### 2.2 Organizations & Memberships Module (`UC-02-Organizations-RBAC`)
- **`UC-ORG-01`**: Create Organization (`POST /api/v1/organizations`) — Instantiates a corporate tenant and automatically maps the creator as an `ADMIN`.
- **`UC-ORG-02`**: List & Read Organizations (`GET /api/v1/organizations/{organizationId}`) — Retrieves tenant data enforcing membership authorization checks.
- **`UC-ORG-03`**: Update Organization Metadata (`PUT /api/v1/organizations/{organizationId}`) — *Includes* `Verify Admin Permissions`. Restricted strictly to organization `ADMIN`.
- **`UC-ORG-04`**: Delete Organization (`DELETE /api/v1/organizations/{organizationId}`) — *Includes* `Verify Admin Permissions` and *Extends* `Trigger Cascading Deletion` to child workspaces.
- **`UC-ORG-05`**: Add Organization Member (`POST /api/v1/organizations/{organizationId}/members`) — Creates a membership and assigns an `ADMIN` or `MEMBER` role.
- **`UC-ORG-06`**: Update Organization Member Role (`PUT /api/v1/organizations/{organizationId}/members/{userId}`) — Changes an active membership role while preserving the last-admin safeguard.
- **`UC-ORG-07`**: Remove Organization Member (`DELETE /api/v1/organizations/{organizationId}/members/{userId}`) — Removes a membership and prevents removal of the last administrator.


### 2.3 Projects & Task Management Core (`UC-03-Projects-Tasks-Domain`)
- **`UC-PROJ-01`**: Create & Manage Projects (`POST /api/v1/projects`, `GET /api/v1/projects?organizationId={organizationId}`, `GET/PUT /api/v1/projects/{projectId}`) — Creates and manages workspaces inside tenant boundaries. Only organization members can list, read, create, or update projects in their organization.
- **`UC-PROJ-02`**: Archive Project (`PATCH /api/v1/projects/{projectId}/archive`) — Transitions workspace to read-only mode; restricted to organization `ADMIN`.
- **`UC-TASK-01`**: Create Task (`POST /api/v1/projects/{projectId}/tasks`) — *Includes* `Verify Project Is Active`. Initializes tasks strictly in the `BACKLOG` state.
- **`UC-TASK-02`**: Assign & Update Task Details (`PUT /api/v1/tasks/{taskId}`) — Manages task assignees, descriptions, and metadata updates within project boundaries.
- **`UC-TASK-03`**: Task State Machine Transition (`PATCH /api/v1/tasks/{taskId}/status`) — *Includes* `Evaluate State Invariants`. Enforces directional workflow rules (`BACKLOG` > `TODO` > `IN_PROGRESS` > `IN_REVIEW` > `DONE`) and blocks illegal jumps.
- **`UC-COMM-01`**: Task Collaboration (`POST/GET/DELETE /api/v1/tasks/{taskId}/comments`) — Manages team discussion threads with author/admin deletion privileges.

---

## 3. Deep-Dive Use Case Specifications

### 3.1 Strict State Machine Transition (Task Lifecycle)
- **ID:** `UC-TASK-03`
- **Actor Principal:** Organization Member (`MEMBER` or `ADMIN`).
- **Preconditions:**
  * The user must be authenticated with a valid JWT access token.
  * The parent project must be in an `ACTIVE` state (archived projects block modifications).
  * The target task must exist within the database.
- **Main Scenario (Normal Flow):**
  1. The user sends a `PATCH /api/v1/tasks/{taskId}/status` request with the `targetStatus` payload (e.g., `IN_PROGRESS`).
  2. The Spring Security filter chain validates the JWT token and populates the Security Context.
  3. The application service verifies that the user belongs to the organization owning the task.
  4. The **Domain Layer State Machine Engine** evaluates the current task status (e.g., `TODO`) against the requested `targetStatus` (invoking `Evaluate State Invariants`).
  5. The engine validates that the transition satisfies the strict directional flow: `BACKLOG` > `TODO` > `IN_PROGRESS` > `IN_REVIEW` > `DONE` (allowing adjacent backward movement for reviews).
  6. The system persists the status change, updates the `updatedAt` audit timestamp, and returns `200 OK` with the updated task model.
- **Alternative Flows & Exceptions:**
  * **`401 Unauthorized`**: If the JWT token is expired, malformed, or missing.
  * **`403 Forbidden`**: If the user lacks access to the parent organization or if the project is `ARCHIVED`.
  * **`400 Bad Request / DomainValidationException`**: If an illegal state jump is attempted (e.g., jumping straight from `BACKLOG` to `DONE`), the domain engine blocks execution and returns a structured RFC 7807 error.

---

### 3.2 Organization Creation & Automated RBAC Mapping
- **ID:** `UC-ORG-01`
- **Actor Principal:** Authenticated User.
- **Preconditions:**
  * Valid authentication session using a JWT access token.
- **Main Scenario (Normal Flow):**
  1. The user submits a `POST /api/v1/organizations` request containing the corporate name.
  2. The system validates payload constraints (name length and global uniqueness after normalization).
  3. A database transaction begins:
     - Persists the new `Organization` tenant entity.
     - Automatically creates a relational record in the `organization_members` junction table linking the authenticated user ID to the new organization ID with the role `ADMIN`.
  4. The transaction commits successfully, returning a `201 Created` status with the organization details and ID.
- **Alternative Flows & Exceptions:**
  * **`401 Unauthorized`**: Missing or invalid token.
  * **`400 Bad Request`**: Validation failure on the organization name.
  * **`409 Conflict`**: The normalized organization name is already used by another organization.
