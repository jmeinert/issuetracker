# Issue Tracker

Issue Tracker is a REST API for managing projects and tracking their associated issues through a defined workflow.

I built this project to deepen my practical knowledge of Java and the Spring Boot ecosystem after several years
of professional web development with PHP and TYPO3.

My goal was to go beyond a minimal CRUD demo by adding realistic workflow rules, PostgreSQL persistence,
JWT-based authentication, role-based authorization and automated integration tests.

## Features

### Project and issue management

* Track issues within projects using defined statuses and priorities
* Query issues with pagination, sorting, combinable filters and case-insensitive text search
* Enforce status transitions and require closed issues to be reopened before editing
* Prevent deletion of projects that still contain issues

### Users and security

* Register users and store passwords as Argon2id hashes
* Issue short-lived JWTs for valid credentials
* Associate issues with their reporter and optional assignee
* Restrict operations based on roles and issue ownership
* Enable or disable users through an administrator endpoint

### API behavior

* Validate incoming requests with Bean Validation
* Return consistent, structured error responses

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

## Tech stack

* Java 21
* Spring Boot 4.1
* Spring Web MVC and Spring Data JPA
* Spring Security and OAuth2 Resource Server
* Hibernate
* PostgreSQL 18
* Flyway
* Docker Compose
* Jakarta Bean Validation
* Maven
* JUnit 5, Mockito and MockMvc
* Testcontainers
* GitHub Actions

## Architecture

The codebase is organized by feature around the project, issue, user and authentication domains.
Security, configuration and error handling are kept in dedicated packages.

The feature packages separate HTTP handling, business logic and persistence.
The API uses dedicated request and response DTOs rather than exposing JPA entities directly.

## Authentication and authorization

The API uses stateless bearer authentication with signed JWTs. Access tokens expire after 15 minutes.

Public registration always creates an enabled user with the `USER` role.
Clients cannot select or change their own role.

> [!NOTE]
> Disabling a user blocks future logins but does not revoke existing access tokens.
> They remain valid for up to 15 minutes.

### Local administrator

For local development, Flyway seeds the following administrator:

Username: `admin` <br>
Password: `testpassword1234`

## Testing

The service layer is covered by unit tests using JUnit 5 and Mockito.
Controller tests use MockMvc to verify request validation, JSON responses, HTTP status codes and business-rule conflicts.
Integration tests run against PostgreSQL using Testcontainers, with Flyway managing the test schema.

GitHub Actions runs `./mvnw verify` on pushes to `main` and pull requests targeting `main`.

Run the complete test suite:

```bash
./mvnw test
```

Run the complete build verification:

```bash
./mvnw verify
```

## Getting started

### Prerequisites

* Java 21
* Docker with Docker Compose

Clone the repository and create the local environment file:

```bash
git clone https://github.com/jmeinert/issuetracker.git
cd issuetracker
cp .env.example .env
```

### Generate JWT secret

Generate a Base64-encoded JWT signing secret containing at least 32 bytes:

```bash
openssl rand -base64 32
```

Store the generated value as `JWT_SECRET` in the `.env` file.

### Start PostgreSQL container

```bash
docker compose up -d postgres
```

### Start application from CLI (Bash)

```bash
set -a
source .env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Start application from IntelliJ

Activate the `local` Spring profile and provide the variables from `.env` in the run configuration.
Then run `IssuetrackerApplication`.

The API is available at `http://localhost:8080`.

### Stop or reset PostgreSQL container

Stop PostgreSQL while retaining its data:

```bash
docker compose down
```

Stop PostgreSQL and delete its data:

```bash
docker compose down -v
```

## API endpoints

### Authentication

| Method | Endpoint             | Access | Description     |
|--------|----------------------|--------|-----------------|
| `POST` | `/api/auth/register` | Public | Register a user |
| `POST` | `/api/auth/login`    | Public | Obtain a JWT    |

### Users

| Method  | Endpoint                      | Access | Description              |
|---------|-------------------------------|--------|--------------------------|
| `PATCH` | `/api/users/{userId}/enabled` | Admin  | Enable or disable a user |

### Projects

| Method   | Endpoint             | Access             | Description              |
|----------|----------------------|--------------------|--------------------------|
| `GET`    | `/api/projects`      | Authenticated user | Retrieve all projects    |
| `GET`    | `/api/projects/{id}` | Authenticated user | Retrieve a project by ID |
| `POST`   | `/api/projects`      | Admin              | Create a project         |
| `PUT`    | `/api/projects/{id}` | Admin              | Update a project         |
| `DELETE` | `/api/projects/{id}` | Admin              | Delete a project         |

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

## Example requests

The examples assume a freshly initialized local database.
The seeded local administrator is used for admin-only operations.

### Register a regular user

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "testpassword1234"
  }'
```

### Log in as the local administrator

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "testpassword1234"
  }'
```

Example response:

```json
{
  "token": "<signed-jwt>"
}
```

Use the returned token for every protected request.

### Create a project

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer <signed-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Portal",
    "description": "Improvements to the customer self-service portal"
  }'
```

Example response:

```json
{
  "id": 1,
  "name": "Customer Portal",
  "description": "Improvements to the customer self-service portal",
  "createdAt": "2026-07-27T12:00:00Z",
  "updatedAt": "2026-07-27T12:00:00Z"
}
```

### Create an issue

```bash
curl -X POST http://localhost:8080/api/projects/1/issues \
  -H "Authorization: Bearer <signed-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Fix mobile navigation",
    "description": "The navigation menu does not close after selecting a link on small screens.",
    "priority": "HIGH"
  }'
```

New issues automatically receive the `OPEN` status.

### Change an issue status

```bash
curl -X PATCH http://localhost:8080/api/issues/1/status \
  -H "Authorization: Bearer <signed-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

## Roadmap

Planned improvements include:

* [ ] OpenAPI documentation
* [ ] Containerized application deployment
