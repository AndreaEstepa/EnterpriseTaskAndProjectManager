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
Responsible for user registration, profile management, credential lifecycle, and secure token issuance.

*   **RF-IAM-01: Create User (Registration)**
    *   **Description:** Registers a new user account in the platform with strict password complexity policies, email uniqueness enforcement, and secure password hashing.
    *   **HTTP Method & Endpoint:** `POST /api/v1/auth/register`
    *   **Request Body (JSON):**
        ```json
        {
          "email": "john.doe@enterprise.com",
          "password": "SecurePassword123!",
          "fullName": "John Doe"
        }
        ```
    *   **Validation Rules & Business Logic:**
        *   `email`: Must be a valid email format, unique across the database, and max 255 characters.
        *   `password`: Minimum 8 characters, must contain at least one uppercase letter, one lowercase letter, one number, and one special character.
        *   `fullName`: Between 2 and 100 characters.
        *   **Processing:** The raw password must be securely hashed using `BCryptPasswordEncoder` (with a strength factor of 12) before persisting the `User` entity. The initial account status is set to `ACTIVE`.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "email": "john.doe@enterprise.com",
          "fullName": "John Doe",
          "createdAt": "2026-06-06T10:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure (weak password, invalid email format, missing fields).
        *   `409 Conflict`: The provided email is already registered in the system.




*   **RF-IAM-02: Authenticate User (Login)**
    *   **Description:** Validates user credentials against stored hashes using Spring Security's authentication manager and issues a cryptographically signed JWT Access Token and a persistent Refresh Token.
    *   **HTTP Method & Endpoint:** `POST /api/v1/auth/login`
    *   **Request Body (JSON):**
        ```json
        {
          "email": "john.doe@enterprise.com",
          "password": "SecurePassword123!"
        }
        ```
    *   **Processing Business Logic:**
        *   Authenticates credentials. If authentication fails, increments failed login attempts (or logs warning).
        *   Generates a short-lived **JWT Access Token** (expiration: 15 minutes) containing user claims and roles.
        *   Generates a secure **Refresh Token** (expiration: 7 days), securely hashes or stores it in the database bound to the user session, and returns both to the client.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
          "refreshToken": "d3b07384d113edec49eaa6238ad5ff00...",
          "tokenType": "Bearer"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Malformed or missing request body parameters.
        *   `401 Unauthorized`: Bad credentials (incorrect email/password) or account is deactivated/locked.




*   **RF-IAM-03: Token Rotation & Refresh Mechanism**
    *   **Description:** Generates a new cryptographically signed JWT Access Token and a rotated Refresh Token using a valid, non-expired refresh token. Implements strict token rotation policies to mitigate token theft and replay attacks.
    *   **HTTP Method & Endpoint:** `POST /api/v1/auth/refresh`
    *   **Request Body (JSON):**
        ```json
        {
          "refreshToken": "d3b07384d113edec49eaa6238ad5ff00..."
        }
        ```
    *   **Processing Business Logic & Security Steps:**
        1.  **Validation:** Verify the cryptographic signature and expiration of the incoming refresh token. If expired or malformed, reject immediately.
        2.  **Database Lookup:** Query the persistent storage (`refresh_tokens` table) to check if the token exists, is active, and has not been revoked.
        3.  **Replay Attack Detection (Security Safeguard):** 
            *   If the token presented has *already* been used/rotated (i.e., marked as revoked or missing), this indicates a potential security breach (token theft). 
            *   *Action:* Invalidate all active refresh tokens associated with that user ID immediately, forcing a global logout, and return `401 Unauthorized`.
        4.  **Rotation Execution:** Revoke the currently used refresh token (mark it as `REVOKED` or delete it).
        5.  **Issuance:** Generate a fresh JWT Access Token (short expiration, e.g., 15 minutes) and a brand-new Refresh Token (longer expiration, e.g., 7 days), persisting the new refresh token in the database linked to the user session.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
          "refreshToken": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Missing or malformed request body.
        *   `401 Unauthorized`: Token is expired, invalid, signature verification failed, or a token reuse/replay attack was detected (triggering security revocation).




*   **RF-IAM-04: Read User Profile**
    *   **Description:** Retrieves profile details of the currently authenticated user based on the security context established by the incoming JWT.
    *   **HTTP Method & Endpoint:** `GET /api/v1/users/me`
    *   **Security & Headers:** Requires a valid Bearer token in the `Authorization` header (`Authorization: Bearer <accessToken>`).
    *   **Processing Business Logic:**
        *   Extracts the user identifier (UUID or email) from the validated JWT claims.
        *   Queries the persistence layer to fetch current profile attributes.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "email": "john.doe@enterprise.com",
          "fullName": "John Doe",
          "status": "ACTIVE",
          "createdAt": "2026-06-06T10:00:00Z",
          "updatedAt": "2026-06-06T10:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing, expired, or malformed JWT token.
        *   `404 Not Found`: User entity referenced in token no longer exists in the database.




*   **RF-IAM-05: Update User Profile**
    *   **Description:** Updates modifiable profile attributes (such as full name) for the currently authenticated user.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/users/me`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "fullName": "Jonathan Doe"
        }
        ```
    *   **Processing Business Logic:**
        *   Extracts user context from security filter.
        *   Validates input constraints (e.g., `fullName` length).
        *   Updates entity fields and modifies the `updatedAt` audit timestamp.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "email": "john.doe@enterprise.com",
          "fullName": "Jonathan Doe",
          "status": "ACTIVE",
          "updatedAt": "2026-06-06T12:30:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure on input fields.
        *   `401 Unauthorized`: Missing or invalid authentication token.



        
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
Manages corporate structures, multi-tenant isolation, and fine-grained access control through Role-Based Access Control (RBAC).

*   **RF-ORG-01: Create Organization (Create)**
    *   **Description:** Creates a new corporate tenant container and automatically assigns the creator as an `ADMIN` within the organization's membership mapping table.
    *   **HTTP Method & Endpoint:** `POST /api/v1/organizations`
    *   **Security & Headers:** Requires a valid Bearer token (`Authorization: Bearer <accessToken>`).
    *   **Request Body (JSON):**
        ```json
        {
          "name": "Acme Corporation Tech"
        }
        ```
    *   **Validation Rules & Business Logic:**
        *   `name`: String, required, between 2 and 100 characters. Must be unique for the user or globally unique according to system policy.
        *   **Processing:** Persists the `Organization` entity. Instantiates a mapping entry in the junction table `organization_members` linking the authenticated user ID to the new organization ID with the role `ADMIN`.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "id": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "name": "Acme Corporation Tech",
          "createdAt": "2026-06-06T10:30:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure (missing or empty name).
        *   `401 Unauthorized`: Missing or invalid authentication token.
        *   `409 Conflict`: Organization name already exists.



*   **RF-ORG-02: Read Organizations & Details (Read / List & Detail)**
    *   **Description:** Retrieves a list of all organizations associated with the currently authenticated user, or fetches detailed information and member lists for a specific organization ID.
    *   **HTTP Methods & Endpoints:** 
        *   List user organizations: `GET /api/v1/organizations`
        *   Read specific organization detail: `GET /api/v1/organizations/{orgId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Processing Business Logic:**
        *   For lists: Queries the database using the `organization_members` junction table to return only organizations where the user is an active member.
        *   For details: Validates that the requesting user belongs to the target organization (`orgId`). If not, rejects access.
    *   **Success Response (`200 OK` - List Example):**
        ```json
        [
          {
            "id": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
            "name": "Acme Corporation Tech",
            "userRole": "ADMIN",
            "createdAt": "2026-06-06T10:30:00Z"
          }
        ]
        ```
    *   **Success Response (`200 OK` - Detail Example):**
        ```json
        {
          "id": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "name": "Acme Corporation Tech",
          "createdAt": "2026-06-06T10:30:00Z",
          "members": [
            {
              "userId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
              "email": "john.doe@enterprise.com",
              "fullName": "John Doe",
              "role": "ADMIN"
            }
          ]
        }
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User attempts to access an organization they do not belong to.
        *   `404 Not Found`: Organization ID does not exist.




*   **RF-ORG-03: Update Organization Details (Update)**
    *   **Description:** Modifies general organization metadata (such as its corporate name).
    *   **HTTP Method & Endpoint:** `PUT /api/v1/organizations/{orgId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "name": "Acme Global Technologies"
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **RBAC Enforcement:** The security layer must verify that the authenticated user possesses the `ADMIN` role within the target `{orgId}`. If the user is a regular `MEMBER`, throw an authorization exception.
        *   `name`: Required, 2 to 100 characters.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "name": "Acme Global Technologies",
          "updatedAt": "2026-06-06T14:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User is a standard member and lacks `ADMIN` privileges.
        *   `404 Not Found`: Organization does not exist.



*   **RF-ORG-04: Delete Organization (Delete)**
    *   **Description:** Permanently deletes an organization tenant container, enforcing cascading deletion across all associated projects, tasks, and membership relations.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/organizations/{orgId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Security & Business Logic Checks:**
        *   **RBAC Enforcement:** Restricted strictly to organization `ADMIN`.
        *   **Safety Safeguard:** Triggers database cascade or clean removal of dependent projects, tasks, comments, and membership records to prevent orphaned relational records.
    *   **Success Response (`204 No Content`):** Empty body.
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks `ADMIN` rights in this organization.
        *   `404 Not Found`: Organization does not exist.



*   **RF-ORG-05: Add Organization Member (Membership Create)**
    *   **Description:** Invites or associates an existing platform user to the organization, assigning a specific corporate role.
    *   **HTTP Method & Endpoint:** `POST /api/v1/organizations/{orgId}/members`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "userId": "c333bb22-1111-4aa0-8888-777766665555",
          "role": "MEMBER"
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **RBAC Enforcement:** Caller must have the `ADMIN` role in the target organization.
        *   `role`: Must be either `ADMIN` or `MEMBER`.
        *   Target `userId` must correspond to an active user account in the system.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "organizationId": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "userId": "c333bb22-1111-4aa0-8888-777766665555",
          "role": "MEMBER",
          "joinedAt": "2026-06-06T15:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Invalid role value or user is already a member.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: Caller lacks `ADMIN` rights.
        *   `404 Not Found`: Organization or target user does not exist.


*   **RF-ORG-06: Remove Organization Member (Membership Delete)**
    *   **Description:** Revokes a user's access by removing them from the organization's membership list.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/organizations/{orgId}/members/{userId}`
    *   **Security & Business Logic Checks:**
        *   **RBAC Enforcement:** Caller must have the `ADMIN` role.
        *   **Business Safeguard:** An organization cannot be left with zero administrators. If attempting to remove the last remaining `ADMIN`, throw a domain validation exception (`400 Bad Request`).
    *   **Success Response (`204 No Content`):** Empty body.
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Attempting to remove the sole remaining administrator.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: Caller lacks `ADMIN` permissions.
        *   `404 Not Found`: Organization or target membership link does not exist.


### 3.3 Module 3: Project Management (Project CRUD)
Manages isolated workspaces and collaborative project containers within specific organizations, enforcing tenant boundaries and lifecycle states.

*   **RF-PROJ-01: Create Project (Create)**
    *   **Description:** Creates a new workspace project tied to a specific organization. The project is initialized with an `ACTIVE` status.
    *   **HTTP Method & Endpoint:** `POST /api/v1/organizations/{orgId}/projects`
    *   **Security & Headers:** Requires a valid Bearer token (`Authorization: Bearer <accessToken>`).
    *   **Request Body (JSON):**
        ```json
        {
          "name": "Backend Cloud Migration",
          "description": "Migrating legacy monolithic microservices to AWS cloud infrastructure."
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **Tenant Membership Check:** The authenticated user must be an active member of the target `{orgId}`. If not, throw `403 Forbidden`.
        *   `name`: Required, between 2 and 100 characters. Must be unique within the organization context.
        *   `description`: Optional, maximum 500 characters.
        *   **Processing:** Persists the `Project` entity linking it to the `organizationId` with an initial status value of `ACTIVE`.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "id": "c555bb11-4444-3cc0-9999-111122223333",
          "organizationId": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "name": "Backend Cloud Migration",
          "description": "Migrating legacy monolithic microservices to AWS cloud infrastructure.",
          "status": "ACTIVE",
          "createdAt": "2026-06-06T16:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure (missing name or length constraints violated).
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User does not belong to the parent organization.
        *   `404 Not Found`: Parent organization does not exist.
        *   `409 Conflict`: Project name already exists within this organization.



*   **RF-PROJ-02: Read Projects & Details (Read / List & Detail)**
    *   **Description:** Lists all workspace projects contained inside a specific organization, or fetches detailed information for a single project ID.
    *   **HTTP Methods & Endpoints:**
        *   List organization projects: `GET /api/v1/organizations/{orgId}/projects`
        *   Read specific project detail: `GET /api/v1/projects/{projectId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Processing Business Logic:**
        *   Validates that the user is a member of the organization that owns the project.
        *   Supports basic listing of project metadata, status, and aggregate statistics (e.g., task counts).
    *   **Success Response (`200 OK` - List Example):**
        ```json
        [
          {
            "id": "c555bb11-4444-3cc0-9999-111122223333",
            "name": "Backend Cloud Migration",
            "status": "ACTIVE",
            "createdAt": "2026-06-06T16:00:00Z"
          }
        ]
        ```
    *   **Success Response (`200 OK` - Detail Example):**
        ```json
        {
          "id": "c555bb11-4444-3cc0-9999-111122223333",
          "organizationId": "b1ffc888-8e1a-4de7-99cc-5aa8bc210f88",
          "name": "Backend Cloud Migration",
          "description": "Migrating legacy monolithic microservices to AWS cloud infrastructure.",
          "status": "ACTIVE",
          "createdAt": "2026-06-06T16:00:00Z",
          "updatedAt": "2026-06-06T16:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User attempts to access a project belonging to an organization they are not part of.
        *   `404 Not Found`: Organization or project ID does not exist.



*   **RF-PROJ-03: Update Project Metadata (Update)**
    *   **Description:** Modifies a project's name or description details.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/projects/{projectId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "name": "AWS Cloud Migration & DevOps",
          "description": "Updated scope: migrating microservices and implementing CI/CD pipelines."
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **RBAC / Membership Check:** User must be an active member of the organization owning the project. (Can be further restricted to org `ADMIN` or project managers if business logic demands).
        *   `name`: Required, 2 to 100 characters.
        *   **Business Rule Safeguard:** If the project status is `ARCHIVED`, updates are rejected (`400 Bad Request`), as archived projects are read-only.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "c555bb11-4444-3cc0-9999-111122223333",
          "name": "AWS Cloud Migration & DevOps",
          "description": "Updated scope: migrating microservices and implementing CI/CD pipelines.",
          "status": "ACTIVE",
          "updatedAt": "2026-06-06T17:30:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure or attempting to update an `ARCHIVED` project.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks permissions within the owning organization.
        *   `404 Not Found`: Project does not exist.




*   **RF-PROJ-04: Archive Project (Soft Delete / State Transition)**
    *   **Description:** Transitions a project's operational state to `ARCHIVED`, locking all contained tasks into read-only mode without permanently erasing historical data.
    *   **HTTP Method & Endpoint:** `PATCH /api/v1/projects/{projectId}/archive`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Security & Business Logic Checks:**
        *   **RBAC Enforcement:** Restricted strictly to organization `ADMIN` role.
        *   **State Check:** If already `ARCHIVED`, the operation is idempotent or returns a clean confirmation response.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "c555bb11-4444-3cc0-9999-111122223333",
          "status": "ARCHIVED",
          "updatedAt": "2026-06-06T18:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User is a standard member and lacks `ADMIN` privileges.
        *   `404 Not Found`: Project does not exist.


*   **RF-PROJ-05: Delete Project Permanently (Delete)**
    *   **Description:** Permanently deletes a project container and triggers cascading deletion of all associated tasks and comment threads.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/projects/{projectId}`
    *   **Security & Business Logic Checks:**
        *   **RBAC Enforcement:** Restricted strictly to organization `ADMIN`.
        *   **Safety Safeguard:** Irreversible data removal action.
    *   **Success Response (`204 No Content`):** Empty body.
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks `ADMIN` privileges.
        *   `404 Not Found`: Project does not exist.


### 3.4 Module 4: Task Management & State Machine (Task CRUD & Domain Core)
The operational core handling workloads, task assignments, and strict lifecycle state transitions.

*   **RF-TASK-01: Create Task (Create)**
    *   **Description:** Creates a task within a specific workspace project, initializing it strictly with the `BACKLOG` state.
    *   **HTTP Method & Endpoint:** `POST /api/v1/projects/{projectId}/tasks`
    *   **Security & Headers:** Requires a valid Bearer token (`Authorization: Bearer <accessToken>`).
    *   **Request Body (JSON):**
        ```json
        {
          "title": "Configure Flyway Database Migration Scripts",
          "description": "Write initial V1__init_schema.sql scripts for IAM, organizations, projects, and tasks.",
          "priority": "HIGH",
          "assigneeId": "c333bb22-1111-4aa0-8888-777766665555"
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **Membership Check:** The authenticated user must belong to the organization owning the target project.
        *   `title`: Required, between 2 and 150 characters.
        *   `priority`: Must be one of `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
        *   `assigneeId`: Optional. If provided, the referenced user must be an active member of the parent organization.
        *   **Project State Check:** If the parent project is `ARCHIVED`, task creation is rejected (`400 Bad Request`).
        *   **Processing:** Persists the `Task` entity, setting the initial `status` strictly to `BACKLOG` regardless of any input payload.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "id": "d666aa22-3333-2cc0-7777-222211110000",
          "projectId": "c555bb11-4444-3cc0-9999-111122223333",
          "title": "Configure Flyway Database Migration Scripts",
          "description": "Write initial V1__init_schema.sql scripts for IAM, organizations, projects, and tasks.",
          "status": "BACKLOG",
          "priority": "HIGH",
          "assigneeId": "c333bb22-1111-4aa0-8888-777766665555",
          "createdAt": "2026-06-06T18:30:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure or project is `ARCHIVED`.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks access to the parent project's organization.
        *   `404 Not Found`: Project or assignee ID does not exist.

*   **RF-TASK-02: Read Tasks & Details (Read / List with Filters & Detail)**
    *   **Description:** Lists tasks contained within a project (supporting filtering query parameters) or fetches detailed information for a single task including its metadata and associated comments.
    *   **HTTP Methods & Endpoints:**
        *   List project tasks: `GET /api/v1/projects/{projectId}/tasks?status=BACKLOG&priority=HIGH&assigneeId=c333bb22-...`
        *   Read specific task detail: `GET /api/v1/tasks/{taskId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Processing Business Logic:**
        *   Validates organization membership for the requesting user.
        *   Supports dynamic filtering via optional query parameters (`status`, `priority`, `assigneeId`).
    *   **Success Response (`200 OK` - List Example):**
        ```json
        [
          {
            "id": "d666aa22-3333-2cc0-7777-222211110000",
            "title": "Configure Flyway Database Migration Scripts",
            "status": "BACKLOG",
            "priority": "HIGH",
            "assigneeId": "c333bb22-1111-4aa0-8888-777766665555",
            "createdAt": "2026-06-06T18:30:00Z"
          }
        ]
        ```
    *   **Success Response (`200 OK` - Detail Example):**
        ```json
        {
          "id": "d666aa22-3333-2cc0-7777-222211110000",
          "projectId": "c555bb11-4444-3cc0-9999-111122223333",
          "title": "Configure Flyway Database Migration Scripts",
          "description": "Write initial V1__init_schema.sql scripts for IAM, organizations, projects, and tasks.",
          "status": "BACKLOG",
          "priority": "HIGH",
          "assigneeId": "c333bb22-1111-4aa0-8888-777766665555",
          "createdAt": "2026-06-06T18:30:00Z",
          "updatedAt": "2026-06-06T18:30:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks access to the target project/organization.
        *   `404 Not Found`: Project or task ID does not exist.

*   **RF-TASK-03: Update Task General Fields (Update)**
    *   **Description:** Updates core task descriptive attributes (title, description, priority, or assignee assignment). Note: Status updates are excluded from this endpoint and managed via the dedicated state machine endpoint.
    *   **HTTP Method & Endpoint:** `PUT /api/v1/tasks/{taskId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "title": "Configure Flyway Database Migration Scripts & Seed Data",
          "description": "Write initial V1__init_schema.sql and seed baseline records.",
          "priority": "URGENT",
          "assigneeId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   User must belong to the organization owning the task.
        *   Project state check: Updates are blocked if the parent project is `ARCHIVED`.
        *   `title`: Required, 2 to 150 characters.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "d666aa22-3333-2cc0-7777-222211110000",
          "title": "Configure Flyway Database Migration Scripts & Seed Data",
          "description": "Write initial V1__init_schema.sql and seed baseline records.",
          "status": "BACKLOG",
          "priority": "URGENT",
          "assigneeId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "updatedAt": "2026-06-06T19:00:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure or project is `ARCHIVED`.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks permissions.
        *   `404 Not Found`: Task or assignee ID does not exist.

*   **RF-TASK-04: Strict State Machine Transition (Specialized Update)**
    *   **Description:** Controls the directional workflow of a task's lifecycle to prevent arbitrary or illogical status shifts.
    *   **HTTP Method & Endpoint:** `PATCH /api/v1/tasks/{taskId}/status`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Request Body (JSON):**
        ```json
        {
          "targetStatus": "IN_PROGRESS"
        }
        ```
    *   **Allowed Flow Rules & State Machine Logic:**
        *   Valid states: `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`.
        *   Forward progression path: `BACKLOG` ➔ `TODO` ➔ `IN_PROGRESS` ➔ `IN_REVIEW` ➔ `DONE`.
        *   Backward allowance: Moving back to a previous adjacent phase (e.g., `IN_REVIEW` ➔ `IN_PROGRESS`) is permitted for revision handling. Skipping non-adjacent states arbitrarily (e.g., `BACKLOG` directly to `DONE`) is strictly prohibited by the domain validation engine.
    *   **Success Response (`200 OK`):**
        ```json
        {
          "id": "d666aa22-3333-2cc0-7777-222211110000",
          "status": "IN_PROGRESS",
          "updatedAt": "2026-06-06T19:10:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Invalid transition attempted, resulting in a `DomainValidationException`.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks permission or project is `ARCHIVED`.
        *   `404 Not Found`: Task ID does not exist.

*   **RF-TASK-05: Delete Task (Delete)**
    *   **Description:** Permanently deletes a task workspace entity and triggers cascading deletion of its associated comment threads.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/tasks/{taskId}`
    *   **Security & Business Logic Checks:**
        *   User must be an organization member with sufficient scope (or project participant/admin).
        *   Irreversible deletion safeguard.
    *   **Success Response (`204 No Content`):** Empty body.
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks permission.
        *   `404 Not Found`: Task ID does not exist.


### 3.5 Module 5: Task Comments (Comment CRUD)
Handles internal team collaboration, threaded discussions, and note-taking associated with specific tasks.

*   **RF-COMM-01: Create Comment (Create)**
    *   **Description:** Appends a text comment to a specific task, associating the comment with the currently authenticated user as the author.
    *   **HTTP Method & Endpoint:** `POST /api/v1/tasks/{taskId}/comments`
    *   **Security & Headers:** Requires a valid Bearer token (`Authorization: Bearer <accessToken>`).
    *   **Request Body (JSON):**
        ```json
        {
          "content": "Finished reviewing the database migration scripts. Everything looks solid."
        }
        ```
    *   **Validation Rules & Security Checks:**
        *   **Membership Check:** The authenticated user must be a member of the organization that owns the project and task.
        *   `content`: Required, non-empty, between 1 and 2000 characters.
        *   **Project State Check:** If the parent project is `ARCHIVED`, adding comments is rejected (`400 Bad Request`).
        *   **Processing:** Persists the `Comment` entity with a generated UUID, linking it to the `taskId`, setting `authorId` to the current user, and registering the `createdAt` timestamp.
    *   **Success Response (`201 Created`):**
        ```json
        {
          "id": "e777bb33-2222-1cc0-8888-333344445555",
          "taskId": "d666aa22-3333-2cc0-7777-222211110000",
          "authorId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "authorName": "John Doe",
          "content": "Finished reviewing the database migration scripts. Everything looks solid.",
          "createdAt": "2026-06-06T19:15:00Z"
        }
        ```
    *   **Error Handling Responses:**
        *   `400 Bad Request`: Validation failure (empty content) or project is `ARCHIVED`.
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User does not have access to the parent task/project.
        *   `404 Not Found`: Task ID does not exist.



*   **RF-COMM-02: Read Comments (Read / List)**
    *   **Description:** Retrieves all comments linked to a specific task, returned in chronological order (oldest to newest) to maintain a natural conversation flow.
    *   **HTTP Method & Endpoint:** `GET /api/v1/tasks/{taskId}/comments`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Processing Business Logic:**
        *   Validates that the requesting user belongs to the organization owning the task.
        *   Queries database sorting by `createdAt ASC`.
    *   **Success Response (`200 OK`):**
        ```json
        [
          {
            "id": "e777bb33-2222-1cc0-8888-333344445555",
            "taskId": "d666aa22-3333-2cc0-7777-222211110000",
            "authorId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            "authorName": "John Doe",
            "content": "Finished reviewing the database migration scripts. Everything looks solid.",
            "createdAt": "2026-06-06T19:15:00Z"
          }
        ]
        ```
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User lacks access to the task's organization.
        *   `404 Not Found`: Task ID does not exist.


*   **RF-COMM-03: Delete Comment (Delete)**
    *   **Description:** Permanently deletes a specific task comment.
    *   **HTTP Method & Endpoint:** `DELETE /api/v1/comments/{commentId}`
    *   **Security & Headers:** Requires a valid Bearer token.
    *   **Security & Business Logic Checks:**
        *   **Authorization Rule:** The security policy must verify that either:
            1. The authenticated user is the **original author** of the comment.
            2. The authenticated user holds the **`ADMIN`** role within the parent organization.
        *   If neither condition is met, throw an authorization exception (`403 Forbidden`).
    *   **Success Response (`204 No Content`):** Empty body.
    *   **Error Handling Responses:**
        *   `401 Unauthorized`: Missing or invalid token.
        *   `403 Forbidden`: User is neither the comment author nor an organization administrator.
        *   `404 Not Found`: Comment ID does not exist.
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
| **Org** | `PUT` | `/api/v1/organizations/{id}/members/{uId}` | Update member role | Org `ADMIN` |
| **Org** | `DELETE`| `/api/v1/organizations/{id}/members/{uId}` | Remove organization member | Org `ADMIN` |
| **Proj** | `POST` | `/api/v1/organizations/{id}/projects` | Create workspace project | Org Member |
| **Proj** | `GET` | `/api/v1/organizations/{id}/projects` | List organization projects | Org Member |
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