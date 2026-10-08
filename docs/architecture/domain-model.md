# Domain Model & Core Business Rules

This document establishes the conceptual domain model for the multi-tenant task management platform. It defines the core business entities, attributes, identifiers, cardinalities, validation rules, lifecycle states, and strict deletion policies, completely isolated from infrastructure or framework concerns.

---

## 1. Entity Responsibilities & Attributes

The domain is structured around core bounded contexts governing tenants, workspaces, operational units, and identity access.

### 1.1. User
- **Responsibility:** Represents the physical identity and profile credentials of a human actor interacting with the system.
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `email`: Unique string identifier used for authentication.
  - `passwordHash`: Encrypted credential string (never exposed to external layers).
  - `createdAt`: Timestamp marking registration time.

### 1.2. Organization
- **Responsibility:** Acts as the multi-tenant corporate boundary isolating workspaces, projects, and memberships.
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `name`: Corporate or team name (unique within global scope).
  - `createdAt`: Timestamp marking tenant provisioning.

### 1.3. Membership (`OrganizationMember`)
- **Responsibility:** Manages the relational bridge between a `User` and an `Organization`, encapsulating the access level and operational rights within that specific tenant via a `RoleEnum` (`ADMIN` or `MEMBER`).
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `role`: Enumerated security level (`ADMIN`, `MEMBER`).
  - `joinedAt`: Timestamp marking association time.

### 1.4. Project
- **Responsibility:** Represents an isolated workspace or product container confined strictly within a single organization's boundaries.
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `name`: Workspace title.
  - `description`: Scope or objectives summary.
  - `status`: Enumerated workspace lifecycle (`ACTIVE`, `ARCHIVED`).

### 1.5. Task
- **Responsibility:** Represents the atomic operational unit or ticket assigned to a project, governed by a strict state machine workflow.
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `title`: Summary of the task objective.
  - `description`: Detailed technical or operational notes.
  - `status`: Current lifecycle phase (`TaskStatusEnum`).
  - `updatedAt`: Audit timestamp of the last modification.

### 1.6. Comment
- **Responsibility:** Represents a contextual text-based collaboration thread linked to a specific task for team communication.
- **Attributes:**
  - `id`: Unique identifier (UUID).
  - `content`: Message body.
  - `createdAt`: Timestamp of publication.

---

## 2. Relationships and Cardinality

- **Organization (1) ➔ (0..*) Project:** An organization can own multiple projects. A project belongs to exactly one parent organization (Strict Composition).
- **Organization (1) ➔ (1..*) Membership:** An organization contains multiple member profiles.
- **User (1) ➔ (0..*) Membership:** A user profile can participate in multiple organizations through distinct memberships.
- **Project (1) ➔ (1..*) Task:** A project contains multiple tasks. A task belongs to one parent project (Strict Composition).
- **Task (1) ➔ (0..*) Comment:** A task can host multiple discussion comments. A comment belongs to a single task.
- **User (1) ➔ (0..*) Task:** A user can be assigned to multiple tasks (optional assignment).
- **User (1) ➔ (0..*) Comment:** A user can author multiple comments.

---

## 3. Validation Rules & Business Invariants

- **Email Uniqueness:** Every `User` registration must enforce strict global email uniqueness.
- **Tenant Name Integrity:** Organization names must be non-empty, trimmed, and unique across the platform.
- **Project Active Constraint:** No modifications, task creations, or state transitions can occur inside a project if its status is set to `ARCHIVED`.
- **Membership Association:** A user cannot interact with a project or task unless an active `Membership` links their user ID to the parent `Organization`.

---

## 4. Lifecycle States & State Machine Rules

### 4.1. Project Status (`ProjectStatusEnum`)
- `ACTIVE`: Normal operational mode allowing read/write actions on child tasks.
- `ARCHIVED`: Read-only mode protecting historical data; blocks new tasks and state modifications.

### 4.2. Task Status (`TaskStatusEnum`) & Directional Flow
Tasks follow a strict, linear progression with controlled backward allowance for peer reviews:
- **Allowed States:** `BACKLOG` ➔ `TODO` ➔ `IN_PROGRESS` ➔ `IN_REVIEW` ➔ `DONE`.
- **Transition Rules:**
  - Initial creation always forces the task into the `BACKLOG` state.
  - Forward movement is sequential (e.g., `BACKLOG` to `TODO`, `TODO` to `IN_PROGRESS`). Skipping states directly (e.g., `BACKLOG` to `DONE`) is strictly prohibited and triggers a `DomainValidationException`.
  - Backward movement is permitted exclusively for review cycles (e.g., `IN_REVIEW` back to `IN_PROGRESS`).

---

## 5. Deletion & Cascading Rules

To preserve audit integrity and prevent accidental data loss across multi-tenant boundaries, cascading rules are defined as follows:

- **Organization Deletion:** Deleting an organization triggers a cascading cascade drop of all child `Memberships`, `Projects`, `Tasks`, and `Comments`. (Restricted exclusively to users holding the `ADMIN` role).
- **Project Deletion:** Deleting a project cascades down to delete all associated `Tasks` and their respective `Comments`.
- **Task Deletion:** Deleting a task cascades down to remove all associated `Comments`.
- **User Account Deletion:** Implements a *Soft Delete* strategy on the identity profile, while memberships and authored comments remain preserved for project historical auditing unless explicitly purged by a tenant `ADMIN`.