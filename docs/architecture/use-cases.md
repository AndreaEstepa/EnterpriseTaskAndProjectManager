# Use Cases Documentation (The "+1" View)
This document details the functional use cases, actor interactions, and structural inheritance rules for the multi-tenant task management platform. It serves as the primary scenario driver for the 4+1 Architectural View Model.

> **Visual Diagrams Reference:** The dynamic sequence diagrams for this process are modeled in StarUML (see `docs/assets/UC-01-IAM-Security`), (see `docs/assets/UC-02-Organizations-RBAC`), (see `docs/assets/UC-03-Projects-Tasks-Domain`)

---

## 1. System Actors & Generalization Hierarchy

- **Unauthenticated User (`Visitor`):** An unverified user interacting with public endpoints (registration and login).
- **Authenticated User:** Any user with a valid JWT session capable of managing profile data and viewing personal organizations.
- **Organization Member (`MEMBER`):** Inherits from *Authenticated User*. A workspace participant capable of creating projects, writing tasks, executing state machine transitions, and collaborating via comments.
- **Organization Admin (`ADMIN`):** Inherits from *Organization Member*. A tenant administrator with full control over corporate boundaries, role assignments, member management, project archives, and administrative deletions.

---

## 2. Use Case Modules Overview

> **Visual Diagrams Reference:** The architectural visual models for these use cases are structured, modeled, and exported from StarUML (see `docs/assets/use-cases-*.png`).

### 2.1 IAM & Security Module (`UC-01-IAM-Security`)
- **`UC-IAM-01`**: Register User Account (`POST /api/v1/auth/register`) — Validates strict password complexity and email uniqueness, applying BCrypt hashing.
- **`UC-IAM-02`**: Authenticate User & Issue Tokens (`POST /api/v1/auth/login`) — Validates credentials, issuing short-lived JWT Access Tokens and persistent Refresh Tokens.
- **`UC-IAM-03`**: Token Rotation (`POST /api/v1/auth/refresh`) — Rotates refresh tokens and triggers global security revocation if replay/theft attacks are detected.
- **`UC-IAM-04`**: Read/Update Profile (`GET/PUT /api/v1/users/me`) — Manages authenticated user details securely via security context extraction.
- **`UC-IAM-05`**: Deactivate Account (`DELETE /api/v1/users/me`) — Soft deletes the user account and globally revokes active sessions.

### 2.2 Organizations & Memberships Module (`UC-02-Organizations-RBAC`)
- **`UC-ORG-01`**: Create Organization (`POST /api/v1/organizations`) — Instantiates a corporate tenant and automatically maps the creator as an `ADMIN`.
- **`UC-ORG-02`**: List & Read Organizations (`GET /api/v1/organizations/{id}`) — Retrieves tenant data enforcing membership authorization checks.
- **`UC-ORG-03`**: Update Organization Metadata (`PUT /api/v1/organizations/{id}`) — *Includes* `Verify Admin Permissions`. Restricted strictly to organization `ADMIN`.
- **`UC-ORG-04`**: Delete Organization (`DELETE /api/v1/organizations/{id}`) — *Includes* `Verify Admin Permissions` and *Extends* `Trigger Cascading Deletion` to child workspaces.
- **`UC-ORG-05`**: Manage Organization Members (`POST/PUT/DELETE /api/v1/organizations/{id}/members`) — *Includes* `Verify Admin Permissions`. Handles membership invitations, role updates (`ADMIN`/`MEMBER`), and removals with safeguard checks.

### 2.3 Projects & Task Management Core (`UC-03-Projects-Tasks-Domain`)
- **`UC-PROJ-01`**: Create & Manage Projects (`POST/GET/PUT /api/v1/projects`) — Establishes isolated workspaces inside tenant boundaries.
- **`UC-PROJ-02`**: Archive Project (`PATCH /api/v1/projects/{id}/archive`) — Transitions workspace to read-only mode (`ADMIN` only).
- **`UC-TASK-01`**: Create Task (`POST /api/v1/projects/{id}/tasks`) — *Includes* `Verify Project Is Active`. Initializes tasks strictly in the `BACKLOG` state.
- **`UC-TASK-02`**: Task State Machine Transition (`PATCH /api/v1/tasks/{id}/status`) — *Includes* `Evaluate State Invariants`. Enforces directional workflow rules (`BACKLOG` ➔ `TODO` ➔ `IN_PROGRESS` ➔ `IN_REVIEW` ➔ `DONE`) and blocks illegal jumps.
- **`UC-COMM-01`**: Task Collaboration (`POST/GET/DELETE /api/v1/tasks/{id}/comments`) — Manages team discussion threads with author/admin deletion privileges.

---

## 3. Deep-Dive Use Case Specifications

### 3.1 Strict State Machine Transition (Task Lifecycle)
- **ID:** `UC-TASK-04`
- **Actor Principal:** Organization Member (`MEMBER` or `ADMIN`).
- **Preconditions:**
  * The user must be authenticated with a valid JWT Bearer token.
  * The parent project must be in an `ACTIVE` state (archived projects block modifications).
  * The target task must exist within the database.
- **Main Scenario (Normal Flow):**
  1. The user sends a `PATCH /api/v1/tasks/{taskId}/status` request with the `targetStatus` payload (e.g., `IN_PROGRESS`).
  2. The Spring Security filter chain validates the JWT token and populates the Security Context.
  3. The application service verifies that the user belongs to the organization owning the task.
  4. The **Domain Layer State Machine Engine** evaluates the current task status (e.g., `TODO`) against the requested `targetStatus` (invoking `Evaluate State Invariants`).
  5. The engine validates that the transition satisfies the strict directional flow: `BACKLOG` ➔ `TODO` ➔ `IN_PROGRESS` ➔ `IN_REVIEW` ➔ `DONE` (allowing adjacent backward movement for reviews).
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
  * Valid authentication session (Bearer token).
- **Main Scenario (Normal Flow):**
  1. The user submits a `POST /api/v1/organizations` request containing the corporate name.
  2. The system validates payload constraints (name length and uniqueness).
  3. A database transaction begins:
     - Persists the new `Organization` tenant entity.
     - Automatically creates a relational record in the `organization_members` junction table linking the authenticated user ID to the new organization ID with the role `ADMIN`.
  4. The transaction commits successfully, returning a `201 Created` status with the organization details and ID.
- **Alternative Flows & Exceptions:**
  * **`401 Unauthorized`**: Missing or invalid token.
  * **`400 Bad Request`**: Validation failure on the organization name.
  * **`409 Conflict`**: Organization name already exists.