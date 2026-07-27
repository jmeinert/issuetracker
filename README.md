# Issue Tracker

Issue Tracker is a REST API for managing projects and tracking their associated issues through a defined workflow.

I built this project to deepen my practical knowledge of Java and the Spring Boot ecosystem after several years
of professional web development with PHP and TYPO3.

My goal was to go beyond a minimal CRUD demo by adding realistic domain rules, clear API boundaries and automated tests.

> [!NOTE]
> **Work in progress:** The implemented scope covers project and issue management using an in-memory H2 database.
> Data is not persisted between application restarts.
> PostgreSQL persistence, query capabilities, security and deployment infrastructure are planned.

## Features

### Project management

* Create, retrieve, update and delete projects
* Input validation using request DTOs
* Automatic creation and last-modified timestamps
* Reject deletion of projects that still contain issues

### Issue management

* Create issues within a project
* Retrieve individual issues
* Retrieve all issues belonging to a project
* Update and delete issues
* Assign priorities to issues
* Change issue statuses through a dedicated endpoint
* Prevent changes to title, description and priority for closed issues

### Validation and error handling

* Bean Validation for incoming requests
* Centralized exception handling
* Structured error responses
* Appropriate HTTP status codes for validation errors, missing resources and business-rule conflicts

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
* Spring Boot 4.1 with Spring Web MVC and Spring Data JPA
* Hibernate and H2
* Jakarta Bean Validation
* Maven
* JUnit 5, Mockito, MockMvc

## Architecture

The codebase is organized by feature, primarily around the `project` and `issue` domains.
Each domain contains its own controller, service, repository, request DTOs and response DTOs.

Business rules are intentionally kept in the service layer, while controllers focus on request validation and
response mapping. Repositories handle data access through Spring Data JPA, while response DTOs ensure that JPA entities
are not exposed through the API.

## Testing

The service layer is covered by unit tests using JUnit 5 and Mockito.
Controller tests use MockMvc to verify request validation, JSON responses, HTTP status codes and business-rule conflicts.
Parameterized service tests cover all allowed and rejected issue status transitions.

```bash
./mvnw test
```

## Getting started

With Java 21 installed, clone the repository and start the application:

```bash
git clone https://github.com/jmeinert/issuetracker.git
cd issuetracker
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

## API endpoints

### Projects

| Method   | Endpoint             | Description              |
| -------- | -------------------- | ------------------------ |
| `GET`    | `/api/projects`      | Retrieve all projects    |
| `GET`    | `/api/projects/{id}` | Retrieve a project by ID |
| `POST`   | `/api/projects`      | Create a project         |
| `PUT`    | `/api/projects/{id}` | Update a project         |
| `DELETE` | `/api/projects/{id}` | Delete a project         |

### Issues

| Method   | Endpoint                           | Description                       |
| -------- | ---------------------------------- | --------------------------------- |
| `GET`    | `/api/projects/{projectId}/issues` | Retrieve all issues for a project |
| `POST`   | `/api/projects/{projectId}/issues` | Create an issue within a project  |
| `GET`    | `/api/issues/{issueId}`            | Retrieve an issue by ID           |
| `PUT`    | `/api/issues/{issueId}`            | Update an issue                   |
| `PATCH`  | `/api/issues/{issueId}/status`     | Change the status of an issue     |
| `DELETE` | `/api/issues/{issueId}`            | Delete an issue                   |

## Example requests

The examples assume a freshly started application and should be run in order.

### Create a project

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Issue Tracker",
    "description": "Development of a project and issue management API"
  }'
```

Example response:

```json
{
  "id": 1,
  "name": "Issue Tracker",
  "description": "Development of a project and issue management API",
  "createdAt": "2026-07-27T12:00:00Z",
  "updatedAt": "2026-07-27T12:00:00Z"
}
```

### Create an issue

```bash
curl -X POST http://localhost:8080/api/projects/1/issues \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Add PostgreSQL support",
    "description": "Replace the in-memory database with PostgreSQL.",
    "priority": "HIGH"
  }'
```

New issues automatically receive the `OPEN` status.

### Change an issue status

```bash
curl -X PATCH http://localhost:8080/api/issues/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

## Roadmap

Planned improvements include:

* [ ] PostgreSQL database
* [ ] Database migrations with Flyway
* [ ] Filtering, sorting and pagination
* [ ] Authentication and authorization with Spring Security
* [ ] Docker and Docker Compose setup
* [ ] Integration tests with Testcontainers
* [ ] Continuous integration with GitHub Actions
* [ ] OpenAPI documentation
