# Deployment View (4+1 Architectural View Model)

This document details the physical and virtual infrastructure topology of the multi-tenant task management system, mapping software components (containers, runtime environments, and databases) to execution nodes.

---

## 1. Introduction and Purpose
The Deployment View defines the hardware and containerized environment required to execute the system securely, scalably, and with high availability. It bridges the gap between software components and physical or cloud infrastructure.

> **Visual Diagrams Reference:** The deployment topology is modeled in StarUML [DP-01-Deployment-Topology](../assets/DP-01-Deployment-Topology.jpg).

---

## 2. Infrastructure Topology and Nodes
The system architecture is distributed across four distinct tiers, containerized using Docker and orchestrated to ensure strict isolation and resource management:

### 2.1. Client Tier
- **Components:** Web Browser, Mobile Application, or API Testing Clients (e.g., Postman).
- **Protocol:** HTTPS / TLS 1.3 for secure encrypted transport over the public internet.

### 2.2. Edge / Reverse Proxy Tier (API Gateway)
- **Component:** Nginx / Spring Cloud Gateway.
- **Responsibilities:**
  - SSL Termination.
  - Rate limiting and basic IP filtering.
  - Reverse proxy routing incoming requests (`/api/v1/*`) to the internal application container cluster.

### 2.3. Application Server Tier (Backend Container)
- **Component:** Docker container running a Java 21 runtime.
- **Internal Stack:** 
  - Embedded Tomcat Servlet Container.
  - Spring Boot Application executing the Hexagonal Architecture core (Domain, Application, and Infrastructure adapters).
- **Responsibilities:** Stateless execution of business use cases, JWT validation, and coordination of multi-tenant transactions.

### 2.4. Persistence Tier (Database Server)
- **Component:** Managed PostgreSQL Database Instance (or containerized PostgreSQL for local/staging environments).
- **Responsibilities:**
  - Relational data storage governed by Spring Data JPA and Hibernate mappings (`TaskJpaEntity`, `OrganizationJpaEntity`, etc.).
  - Multi-tenancy support via tenant discriminators (`organization_id`) or schema-per-tenant isolation strategies.

---

## 3. Containerization and Environment Consistency
- **Multi-Stage Dockerfiles:** The backend is packaged using a multi-stage Maven build (separating the build phase from the lightweight Java 21 runtime image) to minimize vulnerability surface area and image size.
- **Environment Configuration:** Configuration properties (database connection strings, JWT secrets) are injected securely at runtime via environment variables (`.env` / Kubernetes Secrets), maintaining strict compliance with the Twelve-Factor App methodology.