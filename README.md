# Issue Tracker

[![CI](https://github.com/jmeinert/issuetracker/actions/workflows/ci.yaml/badge.svg)](https://github.com/jmeinert/issuetracker/actions/workflows/ci.yaml)

Issue Tracker is a fullstack learning project for managing projects and tracking their associated issues through a defined workflow

I built this project to deepen my practical knowledge of Java and the Spring Boot ecosystem after several years
of professional web development with PHP and TYPO3.

My goal was to go beyond a minimal CRUD demo by adding realistic workflow rules, PostgreSQL persistence,
session-based authentication, role-based authorization and automated integration tests.

## Features

### Project and issue management

* Track issues within projects using defined statuses and priorities
* Query issues with pagination, sorting, combinable filters and case-insensitive text search
* Enforce status transitions and require closed issues to be reopened before editing
* Prevent deletion of projects that still contain issues

### Users and security

* Register users and store passwords as Argon2id hashes
* Provide login and logout endpoints via sessions
* Associate issues with their reporter and optional assignee
* Restrict operations based on roles and issue ownership
* Enable or disable users through an administrator endpoint

### API behavior

* Validate incoming requests with Bean Validation
* Return consistent, structured error responses

### Build and runtime

* Run the frontend, backend and PostgreSQL with Docker Compose
* Serve the frontend and proxy API requests through nginx, with optional local HTTPS
* Build the backend and frontend images using multi-stage Docker builds
* Run backend verification, frontend linting and both image builds with GitHub Actions

## Tech stack

### Backend

* Java 21
* Spring Boot 4.1
* Spring Web MVC and Spring Data JPA
* Spring Security
* PostgreSQL

### Frontend

* React 19
* Vite
* TypeScript

### Infrastructure

* Docker Compose
* nginx
* GitHub Actions

## Getting started

### Prerequisites

* Docker with Docker Compose

Clone the repository and create the local environment file:

```bash
git clone https://github.com/jmeinert/issuetracker.git
cd issuetracker
cp .env.example .env
```

### Configure environment

The environment file configures the following values:

| Variable            | Required | Description                                              |
|---------------------|----------|----------------------------------------------------------|
| `POSTGRES_DB`       | Yes      | Name of the PostgreSQL database                          |
| `POSTGRES_USER`     | Yes      | PostgreSQL user used by the application                  |
| `POSTGRES_PASSWORD` | Yes      | Password for the PostgreSQL user                         |
| `BACKEND_PORT`      | No       | Host port for direct backend access (defaults to `8080`) |
| `FRONTEND_PORT`     | No       | Host port for nginx (defaults to `80` or `443`)          |
| `POSTGRES_PORT`     | No       | Host port for PostgreSQL (defaults to `5432`)            |

### Start the complete application locally

Build the application image and start all services:

```bash
docker compose up -d --build
```

Frontend: http://localhost

#### Optional: local HTTPS

For HTTPS, install [mkcert](https://github.com/filosottile/mkcert) and create a locally trusted certificate for `localhost`.
Save the `.pem` files in the `.certs` directory in the repository root.
On Linux, grant the nginx container's group (`101`) access to the certificate directory and private key.

```bash
mkdir -p .certs
mkcert -install
mkcert -cert-file .certs/localhost.pem -key-file .certs/localhost-key.pem localhost
sudo chgrp 101 .certs .certs/localhost-key.pem
chmod 750 .certs
chmod 640 .certs/localhost-key.pem
```

Then, start the HTTPS variant:

```bash
docker compose -f compose.yaml -f compose.https.yaml up -d --build
```

Frontend: https://localhost

### Run the backend from source

Running the backend outside Docker additionally requires Java 21.
Start only PostgreSQL:

```bash
docker compose up -d postgres
```

#### Command line (Bash)

```bash
set -a
source .env
set +a
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### IntelliJ

Activate the `local` Spring profile and provide the variables from `.env` in the IntelliJ run configuration.
Then run `IssuetrackerApplication`.

### Reset PostgreSQL

To reset the PostgreSQL database, remove its Docker volume:

```bash
docker compose down -v
```

## API documentation

[Scalar](http://localhost:8080/scalar) documents all endpoints, schemas and responses.

The generated [OpenAPI document](http://localhost:8080/v3/api-docs) is also available as JSON.

Protected endpoints can be called after creating a session with `POST /api/auth/login`.
Use the credentials of the [local administrator account](#local-administrator).
Endpoints with unsafe HTTP methods, such as POST, additionally require both the `XSRF-TOKEN` cookie
and its value in the `X-XSRF-TOKEN` header.

## Architecture

```text
backend/           Spring Boot application
frontend/          React application and nginx configuration
compose.yaml       Local HTTP stack
compose.https.yaml Optional HTTPS overrides
```

The backend is organized by feature around the project, issue, user and authentication domains.
Security, configuration and error handling are kept in dedicated packages.

The feature packages separate HTTP handling, business logic and persistence.
The API uses dedicated request and response DTOs rather than exposing JPA entities directly.

## Issue workflow

New issues are automatically created with the `OPEN` status.

The following status transitions are allowed:

```text
OPEN        -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
IN_PROGRESS -> CLOSED
RESOLVED    -> IN_PROGRESS
RESOLVED    -> CLOSED
CLOSED      -> OPEN
```

Invalid status transitions are rejected with an HTTP `409 Conflict` response.
To model a simple issue workflow, closed issues must be reopened before their title, description or priority can be changed.

Issues use one of the following priorities:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

## Authentication and authorization

The API uses session authentication with login and logout endpoints.

Public registration always creates an enabled user with the `USER` role.
Clients cannot select or change their own role.

> [!NOTE]
> Disabling a user blocks future logins but does not automatically invalidate existing sessions.

### Local administrator

For local development, Flyway seeds the following administrator:

Username: `admin` <br>
Password: `testpassword1234`

## Testing

The service layer is covered by unit tests using JUnit and Mockito.
Controller tests use MockMvc to verify request validation, JSON responses, HTTP status codes and business-rule conflicts.
Integration tests run against PostgreSQL using Testcontainers, with Flyway managing the test schema.

GitHub Actions runs on all pull requests and pushes to main or major version branches such as v2.

### Prerequisites

* Java 21
* Docker (required for integration tests)

Run unit and controller tests:

```bash
cd backend
./mvnw test
```

Run the complete test suite:

```bash
cd backend
./mvnw verify
```

## API endpoints

### Authentication

| Method | Endpoint             | Access | Description     |
|--------|----------------------|--------|-----------------|
| `GET`  | `/api/auth/csrf`     | Public | Get CSRF token  |
| `POST` | `/api/auth/register` | Public | Register a user |
| `POST` | `/api/auth/login`    | Public | Log in          |
| `POST` | `/api/auth/logout`   | Public | Log out         |

### Users

| Method  | Endpoint                      | Access | Description              |
|---------|-------------------------------|--------|--------------------------|
| `PATCH` | `/api/users/{userId}/enabled` | Admin  | Enable or disable a user |

### Projects

| Method   | Endpoint                    | Access             | Description              |
|----------|-----------------------------|--------------------|--------------------------|
| `GET`    | `/api/projects`             | Authenticated user | Retrieve all projects    |
| `GET`    | `/api/projects/{projectId}` | Authenticated user | Retrieve a project by ID |
| `POST`   | `/api/projects`             | Admin              | Create a project         |
| `PUT`    | `/api/projects/{projectId}` | Admin              | Update a project         |
| `DELETE` | `/api/projects/{projectId}` | Admin              | Delete a project         |

### Issues

| Method   | Endpoint                           | Access                    | Description                                                      |
|----------|------------------------------------|---------------------------|------------------------------------------------------------------|
| `GET`    | `/api/issues`                      | Authenticated user        | Query issues with pagination, sorting, filtering and text search |
| `GET`    | `/api/projects/{projectId}/issues` | Authenticated user        | Retrieve paginated issues for a project                          |
| `POST`   | `/api/projects/{projectId}/issues` | Authenticated user        | Create an issue within a project                                 |
| `GET`    | `/api/issues/{issueId}`            | Authenticated user        | Retrieve an issue by ID                                          |
| `PUT`    | `/api/issues/{issueId}`            | Admin, Reporter, Assignee | Update an issue                                                  |
| `PATCH`  | `/api/issues/{issueId}/status`     | Admin, Reporter, Assignee | Change the status of an issue                                    |
| `PATCH`  | `/api/issues/{issueId}/assignee`   | Admin                     | Assign or reassign an issue                                      |
| `DELETE` | `/api/issues/{issueId}/assignee`   | Admin                     | Remove the current assignee                                      |
| `DELETE` | `/api/issues/{issueId}`            | Admin                     | Delete an issue                                                  |
