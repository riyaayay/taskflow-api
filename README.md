# TaskFlow API

A RESTful task management backend built with **Spring Boot 3**, **Spring Security (JWT)**, and **MySQL**, written as practice for backend/API interview rounds (Gracenote/Nielsen-style stack: Java, Spring Boot, MySQL, REST, unit testing).

Runs out of the box against an in-memory H2 database (zero setup), and switches to MySQL with one flag for anything closer to production.

## Features

- JWT-based registration/login (`/api/auth/register`, `/api/auth/login`)
- Full CRUD on tasks, scoped to the authenticated user (`/api/tasks`)
- Pagination and filtering by status (`GET /api/tasks?status=IN_PROGRESS&page=0&size=10`)
- Request validation with meaningful field-level error messages
- Centralized exception handling (`GlobalExceptionHandler`)
- Password hashing with BCrypt
- Unit tests (JUnit 5 + Mockito) for the service layer, and MockMvc tests for the controller layer

## Tech Stack

| Layer          | Technology                          |
|----------------|--------------------------------------|
| Language       | Java 17                              |
| Framework      | Spring Boot 3.3 (Web, Data JPA, Security, Validation) |
| Database       | MySQL (H2 for local dev/tests)       |
| Auth           | JWT (jjwt)                           |
| Build          | Maven                                |
| Testing        | JUnit 5, Mockito, AssertJ, MockMvc   |

## Project Structure

```
src/main/java/com/riya/taskflow/
├── config/         # Security, JWT filter/service, UserDetailsService
├── controller/      # REST controllers (Auth, Task)
├── dto/              # Request/response payloads
├── exception/        # Custom exceptions + global handler
├── model/             # JPA entities (Task, User) and enums
├── repository/        # Spring Data JPA repositories
└── service/            # Business logic
```

## Getting Started

### Option 1 — run instantly with H2 (no DB setup needed)

```bash
git clone <your-repo-url>
cd taskflow-api
mvn spring-boot:run
```

(Requires Maven 3.9+ and JDK 17. If you'd rather not install Maven, run `mvn -N wrapper:wrapper` once to generate `./mvnw`.)

The API comes up on `http://localhost:8080`. An H2 console is available at `/h2-console` for inspecting data while developing.

### Option 2 — run against MySQL

1. Create a database (or let the app create it for you):
   ```sql
   CREATE DATABASE taskflow_db;
   ```
2. Set environment variables (or edit `application-mysql.properties`):
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=yourpassword
   export DB_HOST=localhost
   export DB_NAME=taskflow_db
   ```
3. Run with the `mysql` profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

## API Reference

### Auth

| Method | Endpoint             | Body                                      |
|--------|-----------------------|--------------------------------------------|
| POST   | `/api/auth/register`  | `{ "name", "email", "password" }`          |
| POST   | `/api/auth/login`     | `{ "email", "password" }`                  |

Both return `{ "token", "email", "role" }`. Pass the token on every subsequent request:

```
Authorization: Bearer <token>
```

### Tasks (all require auth)

| Method | Endpoint          | Description                       |
|--------|-------------------|-------------------------------------|
| POST   | `/api/tasks`       | Create a task                      |
| GET    | `/api/tasks`        | List your tasks (paginated, filterable by `status`) |
| GET    | `/api/tasks/{id}`    | Get a single task                 |
| PUT    | `/api/tasks/{id}`     | Update a task                    |
| DELETE | `/api/tasks/{id}`      | Delete a task                   |

Example:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Riya","email":"riya@example.com","password":"secret123"}'

curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactor task service","priority":"HIGH","dueDate":"2026-08-10"}'
```

## Running Tests

```bash
mvn test
```

Covers:
- `TaskServiceTest` — business logic, ownership checks, partial updates
- `TaskControllerTest` — HTTP status codes, validation errors, auth enforcement

## Notes on Design Choices

- **Ownership scoping**: tasks are always filtered by the authenticated user's id at the repository/service layer rather than trusting a client-supplied user id — prevents IDOR-style access to someone else's tasks.
- **DTOs over exposing entities**: request/response objects are kept separate from JPA entities so the API contract doesn't leak persistence details.
- **Stateless JWT auth**: no server-side session storage, so the API scales horizontally without sticky sessions.
