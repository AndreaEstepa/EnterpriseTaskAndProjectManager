# Functional Specification & Product Requirements Document (PRD)
# Enterprise Task & Project Manager (B2B SaaS Platform)

## 1. Executive Summary & System Overview
The **Enterprise Task & Project Manager** is a robust, multi-tenant B2B SaaS platform engineered to streamline internal workflows, project tracking, and cross-functional team collaboration. 

This document serves as the single source of truth for the system's architecture, functional workflows, and complete CRUD data contracts. The project implements strict **Domain-Driven Design (DDD)**, **Clean Architecture**, secure **JWT-based authentication**, **Role-Based Access Control (RBAC)**, and automated containerized integration testing using **Testcontainers**.

---

## 2. System Architecture & Technical Boundaries

The application is structured into a multi-layer **Clean Architecture** layout divided into distinct Maven/Gradle modules:
*   **`domain`**: Pure business logic, aggregate roots, value objects, and domain events. Completely decoupled from frameworks (no Spring or JPA annotations).
*   **`application`**: Use cases (application services) orchestrating business workflows and transaction boundaries.
*   **`infrastructure`**: Framework-dependent layer housing Spring Boot components, Spring Data JPA repositories, REST controllers, security filters, and database configuration.

---

## 3. Comprehensive CRUD & Functional Requirements

### 3.1 Module 1: Authentication & User Management (IAM CRUD)
Responsible for user registration, profile management, and credential lifecycle.

*   **RF-IAM-01: Create User (Registration)**
    *   **Description:** Registers a new user account in the platform.
    *   **HTTP Method & Endpoint:** `POST /api/v1/auth/register`
    *   **Request Body:** `email` (String, unique, valid email), `password` (String, min 8 chars), `fullName` (String).
    *   **Success Response:** `201 Created` returning the user ID and email.
    *   **Error Handling:** `409 Conflict` if the email already exists.
*   **RF-IAM-02: Authenticate User (Login)**
    *   **Description:** Validates credentials and generates a signed JWT Access Token and Refresh Token.
    *   **HTTP Method & Endpoint:** `POST /api/v1/auth/login`
    *   **Request Body:** `email`, `password`.
    *   **Success Response:** `200 OK` with `accessToken` and `refreshToken`.
    *   **Error Handling:** `401 Unauthorized` for bad credentials.
*   **RF-IAM-03: Read User Profile (Read)**
    *   **Description:** Retrieves profile details of the currently authenticated user.
    *   **HTTP Method & Endpoint:** `GET /api/v1/users/me`
    *   **Success Response:** `200 OK` with user details (`id`, `email`, `fullName`, `createdAt`).
*   **RF-IAM-04: Update User Profile (Update)**
    *   **Description:** Updates the profile information of the authenticated user.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/users/me`
    *   **Request Body:** `fullName` (String).
    *   **Success Response:** `200 OK` with updated user profile.
*   **RF-IAM-05: Delete User Account (Delete)**
    *   **Description:** Soft deletes or deactivates the user account globally.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/users/me`
    *   **Success Response:** `204 No Content`.

    *   **RF-IAM-06: Token Rotation & Refresh Mechanism** (`POST /api/v1/auth/refresh`)
    *   **Description:** Generates a new cryptographically signed JWT Access Token and a rotated Refresh Token using a valid, non-expired refresh token. Implements strict token rotation policies to mitigate token theft and replay attacks.
    *   **Input Data (JSON Request Body):**
        *   `refreshToken`: String (JWT or opaque secure random token securely stored/hashed in the database).
    *   **Processing Business Logic & Security Steps:**
        1.  **Validation:** Verify the cryptographic signature and expiration of the incoming refresh token. If expired or malformed, reject immediately.
        2.  **Database Lookup:** Query the persistent storage (`refresh_tokens` table) to check if the token exists, is active, and has not been revoked.
        3.  **Replay Attack Detection (Security Safeguard):** 
            *   If the token presented has *already* been used/rotated (i.e., marked as revoked or missing), this indicates a potential security breach (token theft). 
            *   *Action:* Invalidate all active refresh tokens associated with that user ID immediately, forcing a global logout, and return `401 Unauthorized`.
        4.  **Rotation Execution:** Revoke the currently used refresh token (mark it as `REVOKED` or delete it).
        5.  **Issuance:** Generate a fresh JWT Access Token (short expiration, e.g., 15 minutes) and a brand-new Refresh Token (longer expiration, e.g., 7 days), persisting the new refresh token in the database linked to the user session.
    *   **Success Response (`200 OK`):**
    

### 3.2 Module 2: Organizations & Memberships (Organization CRUD & RBAC)
Manages corporate structures and tenant-level access control.

*   **RF-ORG-01: Create Organization (Create)**
    *   **Description:** Creates a new corporate tenant container.
    *   **HTTP Method & Endpoint:** `POST /api/v1/organizations`
    *   **Request Body:** `name` (String).
    *   **Success Response:** `201 Created` with organization ID. Creator is automatically mapped as an `ADMIN` in `organization_members`.
*   **RF-ORG-02: Read Organizations (Read / List & Detail)**
    *   **Description:** Lists all organizations the user belongs to, or fetches details of a specific organization.
    *   **HTTP Method & Endpoint:** `GET /api/v1/organizations` or `GET /api/v1/organizations/{orgId}`
    *   **Success Response:** `200 OK` with organization details and associated members list.
*   **RF-ORG-03: Update Organization Details (Update)**
    *   **Description:** Modifies general organization metadata.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/organizations/{orgId}`
    *   **Request Body:** `name` (String).
    *   **Security Check:** Restricted strictly to organization `ADMIN` role (`403 Forbidden` otherwise).
*   **RF-ORG-04: Delete Organization (Delete)**
    *   **Description:** Permanently deletes an organization and cascades deletion to projects and tasks.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/organizations/{orgId}`
    *   **Security Check:** Restricted strictly to organization `ADMIN`.
*   **RF-ORG-05: Manage Organization Members (Membership CRUD)**
    *   **Add Member:** `POST /api/v1/organizations/{orgId}/members` (Body: `userId`, `role` [`ADMIN`, `MEMBER`]).
    *   **Remove Member:** `DELETE /api/v1/organizations/{orgId}/members/{userId}`.

### 3.3 Module 3: Project Management (Project CRUD)
Manages isolated workspaces within an organization.

*   **RF-PROJ-01: Create Project (Create)**
    *   **Description:** Creates a workspace project tied to a specific organization.
    *   **HTTP Method & Endpoint:** `POST /api/v1/organizations/{orgId}/projects`
    *   **Request Body:** `name` (String), `description` (String).
    *   **Success Response:** `201 Created` with project ID and default status `ACTIVE`.
*   **RF-PROJ-02: Read Projects (Read / List & Detail)**
    *   **Description:** Lists all projects inside an organization or fetches details of a single project.
    *   **HTTP Method & Endpoint:** `GET /api/v1/organizations/{orgId}/projects` or `GET /api/v1/projects/{projectId}`
    *   **Success Response:** `200 OK` with project data.
*   **RF-PROJ-03: Update Project (Update)**
    *   **Description:** Modifies project name or description.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/projects/{projectId}`
    *   **Request Body:** `name`, `description`.
    *   **Success Response:** `200 OK`.
*   **RF-PROJ-04: Delete / Archive Project (Delete / Soft Delete)**
    *   **Description:** Archives a project (`PATCH /api/v1/projects/{projectId}/archive`) or deletes it permanently (`DELETE /api/v1/projects/{projectId}`).

### 3.4 Module 4: Task Management & State Machine (Task CRUD & Domain Core)
The operational core handling workloads and strict lifecycle transitions.

*   **RF-TASK-01: Create Task (Create)**
    *   **Description:** Creates a task within a specific project.
    *   **HTTP Method & Endpoint:** `POST /api/v1/projects/{projectId}/tasks`
    *   **Request Body:** `title` (String), `description` (String), `priority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), `assigneeId` (UUID, optional).
    *   **Success Response:** `201 Created`. Task is initialized strictly with status `BACKLOG`.
*   **RF-TASK-02: Read Tasks (Read / List with Filters & Detail)**
    *   **Description:** Lists tasks in a project (supports filtering parameters like `status`, `priority`, `assigneeId`) or fetches a single task.
    *   **HTTP Method & Endpoint:** `GET /api/v1/projects/{projectId}/tasks` or `GET /api/v1/tasks/{taskId}`
    *   **Success Response:** `200 OK` with task list or task detail including comments.
*   **RF-TASK-03: Update Task General Fields (Update)**
    *   **Description:** Updates core task attributes (title, description, priority, assignee).
    *   **HTTP Method & Endpoint:** `PUT /api/v1/tasks/{taskId}`
    *   **Request Body:** `title`, `description`, `priority`, `assigneeId`.
    *   **Success Response:** `200 OK`.
*   **RF-TASK-04: Strict State Machine Transition (Specialized Update)**
    *   **Description:** Controls directional flow of a task's lifecycle to prevent arbitrary status shifts.
    *   **HTTP Method & Endpoint:** `PATCH /api/v1/tasks/{taskId}/status`
    *   **Request Body:** `targetStatus` (`BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`).
    *   **Allowed Flow Rules:** `BACKLOG` ➔ `TODO` ➔ `IN_PROGRESS` ➔ `IN_REVIEW` ➔ `DONE` (with backward allowance for previous active phases).
    *   **Error Handling:** Invalid transitions throw a `DomainValidationException` resulting in a `400 Bad Request`.
*   **RF-TASK-05: Delete Task (Delete)**
    *   **Description:** Permanently deletes a task and associated comments.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/tasks/{taskId}`
    *   **Success Response:** `204 No Content`.

### 3.5 Module 5: Task Comments (Comment CRUD)
Handles internal team discussion threads on tasks.

*   **RF-COMM-01: Create Comment (Create)**
    *   **Description:** Appends a text comment to a task.
    *   **HTTP Method & Endpoint:** `POST /api/v1/tasks/{taskId}/comments`
    *   **Request Body:** `content` (String, non-empty).
    *   **Success Response:** `201 Created`.
*   **RF-COMM-02: Read Comments (Read / List)**
    *   **Description:** Lists all comments linked to a task in chronological order.
    *   **HTTP Method & Endpoint:** `GET /api/v1/tasks/{taskId}/comments`
    *   **Success Response:** `200 OK`.
*   **RF-COMM-03: Delete Comment (Delete)**
    *   **Description:** Deletes a specific comment.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/comments/{commentId}`
    *   **Security Check:** Only the comment author or organization `ADMIN` can delete it (`403 Forbidden` otherwise).

---

## 4. Non-Functional Requirements (NFR)

1.  **NFR-01: Domain Isolation:** Zero framework coupling in the `domain` module. Unit tests for domain logic must run without initializing the Spring IoC container.
2.  **NFR-02: Database Version Control:** Schema evolution must be managed deterministically using **Flyway** migration scripts (`V1__init_schema.sql`, etc.) on top of **PostgreSQL**.
3.  **NFR-03: Test Automation Standard:** Comprehensive testing strategy including isolated Unit Tests (JUnit 5 + Mockito) and Integration Tests running against real containers via **Testcontainers**, aiming for $>70\%$ overall code coverage.
4.  **NFR-04: Standardized Error Responses:** All API errors must adhere to the **RFC 7807 (Problem Details for HTTP APIs)** specification, providing uniform JSON structures containing `type`, `title`, `status`, `detail`, and `timestamp`.

5.  **NFR-05: Auditability & Concurrency Control:** Optimistic locking (`@Version` in entities) will be implemented on high-contention aggregates (like `Task`) to prevent lost updates in concurrent environments.

---

## 5. Complete API Endpoints Reference Matrix

| Module | Method | Endpoint | Description | Auth / Security Rule |
| :--- | :--- | :--- | :--- | :--- |
| **IAM** | `POST` | `/api/v1/auth/register` | Register new user account | Public |
| **IAM** | `POST` | `/api/v1/auth/login` | Authenticate & issue JWT | Public |
| **IAM** | `POST` | `/api/v1/auth/refresh` | Rotate access tokens | Public (Valid Refresh Token) |
| **IAM** | `GET` | `/api/v1/users/me` | Read authenticated profile | Authenticated |
| **IAM** | `PUT` | `/api/v1/users/me` | Update authenticated profile | Authenticated |
| **IAM** | `DELETE`| `/api/v1/users/me` | Delete account | Authenticated |
| **Org** | `POST` | `/api/v1/organizations` | Create tenant organization | Authenticated |
| **Org** | `GET` | `/api/v1/organizations` | List user organizations | Authenticated |
| **Org** | `GET` | `/api/v1/organizations/{id}`| Read organization details | Org Member |
| **Org** | `PUT` | `/api/v1/organizations/{id}`| Update organization meta | Org `ADMIN` |
| **Org** | `DELETE`| `/api/v1/organizations/{id}`| Delete organization container| Org `ADMIN` |
| **Org** | `POST` | `/api/v1/organizations/{id}/members` | Add organization member | Org `ADMIN` |
| **Org** | `DELETE`| `/api/v1/organizations/{id}/members/{uId}` | Remove organization member | Org `ADMIN` |
| **Proj** | `POST` | `/api/v1/organizations/{id}/projects` | Create workspace project | Org Member |
| **Proj** | `GET` | `/api/v1/organizations/{id}/organizations/{id}/projects` | List organization projects | Org Member |
| **Proj** | `GET` | `/api/v1/projects/{id}` | Read project details | Org Member |
| **Proj** | `PUT` | `/api/v1/projects/{id}` | Update project metadata | Org Member |
| **Proj** | `PATCH`| `/api/v1/projects/{id}/archive` | Archive workspace project | Org `ADMIN` |
| **Proj** | `DELETE`| `/api/v1/projects/{id}` | Delete project permanently | Org `ADMIN` |
| **Task** | `POST` | `/api/v1/projects/{id}/tasks` | Create project task | Org Member |
| **Task** | `GET` | `/api/v1/projects/{id}/tasks` | List/Filter project tasks | Org Member |
| **Task** | `GET` | `/api/v1/tasks/{id}` | Read specific task details | Org Member |
| **Task** | `PUT` | `/api/v1/tasks/{id}` | Update general task fields | Org Member |
| **Task** | `PATCH`| `/api/v1/tasks/{id}/status` | Execute state machine shift | Org Member |
| **Task** | `DELETE`| `/api/v1/tasks/{id}` | Delete task permanently | Org `ADMIN` or Author |
| **Comm** | `POST` | `/api/v1/tasks/{id}/comments` | Post task comment | Org Member |
| **Comm** | `GET` | `/api/v1/tasks/{id}/comments` | List task comments | Org Member |
| **Comm** | `DELETE`| `/api/v1/comments/{id}` | Delete task comment | Comment Author / Org `ADMIN` |