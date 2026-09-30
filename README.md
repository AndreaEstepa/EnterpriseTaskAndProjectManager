# EnterpriseTaskAndProjectManager
A robust, multi-tenant RESTful API built for scalable project management, featuring strict Role-Based Access Control (RBAC), domain-driven state machine task lifecycles, and isolated workspace structures.

---

## Key Features
* **Multi-Tenant Organizations:** Secure tenant containers with isolated data boundaries.
* **Granular RBAC:** Role-based permissions (`ADMIN` vs. `MEMBER`) enforced at the organization and project levels.
* **Workspace Project Management:** Create, update, filter, and archive project spaces.
* **Strict State Machine Tasks:** Controlled lifecycle transitions (`BACKLOG` > `TODO` > `IN_PROGRESS` > `IN_REVIEW` > `DONE`) preventing arbitrary status jumps.
* **Team Collaboration:** Threaded task comments with author-based or admin-level deletion safeguards.
* **Secure IAM:** JWT-based authentication with access token rotation and secure password hashing.

---

## Tech Stack
* **Language/Framework:** *(e.g., Node.js / Express, Spring Boot, or Go / Gin)*
* **Database:** *(e.g., PostgreSQL with Flyway Migrations)*
* **Authentication:** JSON Web Tokens (JWT) & bcrypt

---

## API Endpoints Reference Matrix
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

---

## Getting Started
### Prerequisites
* Ensure you have your database instance running and environment variables configured.
### Environment Variables
Create a `.env` file in the root directory:
```env
PORT=8080
DATABASE_URL=postgres://user:password@localhost:5432/db_name
JWT_SECRET=your_super_secret_key
JWT_REFRESH_SECRET=your_refresh_secret_key
```

## Instalation and execution:
### Clone the repository
git clone [https://github.com/your-username/enterprise-task-api.git](https://github.com/your-username/enterprise-task-api.git)

### Navigate to project directory
cd enterprise-task-api
### Install dependencies
npm install # or maven/gradle/go mod depending on your stack
### Run migrations (if applicable)
npm run migrate
### Start the application
npm run dev


## Documentation
For detailed business logic rules, data contracts, and error code structures, check out the full [Product Requirements Document](docs/PRD.md).

## License
Distributed under the MIT License. See [LICENSE](LICENSE.md) for more information.