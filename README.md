# Email Service

A Spring Boot 3 microservice for sending transactional HTML emails via SMTP. Email sending is protected by a Resilience4j retry policy and templates are rendered with Thymeleaf.

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| Email | Spring Mail + JavaMailSender |
| Templates | Thymeleaf (`src/main/resources/templates/emails/`) |
| Resilience | Resilience4j `@Retry` — 3 attempts, 2 s back-off |
| API docs | springdoc-openapi / Swagger UI |
| Observability | Spring Boot Actuator |
| Build | Maven 3 |
| Container | Docker (`eclipse-temurin:21-jre-alpine`) |

---

## Project structure

```
src/
├── main/
│   ├── java/com/example/emailservice/
│   │   ├── EmailServiceApplication.java
│   │   ├── config/         OpenApiConfig.java
│   │   ├── controller/     EmailController.java
│   │   ├── dto/            EmailRequest.java · EmailResponse.java · ErrorResponse.java
│   │   ├── exception/      EmailSendException.java · GlobalExceptionHandler.java
│   │   └── service/        EmailService.java (interface) · SmtpEmailService.java
│   └── resources/
│       ├── application.yml
│       └── templates/emails/welcome.html
└── test/
    ├── java/com/example/emailservice/
    │   ├── EmailIntegrationTest.java          (GreenMail)
    │   └── service/SmtpEmailServiceTest.java  (Mockito)
    └── resources/
```

---

## Quick start

### Option A — Docker Compose (recommended)

Starts MailHog as a local SMTP sink and builds + runs the service. No external services required.

**Prerequisites:** Docker, Docker Compose, Maven

```bash
# 1. Build the JAR
mvn package -DskipTests

# 2. Start everything
docker-compose up --build
```

| URL | What |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8025` | MailHog — view captured emails |

Tear down:

```bash
docker-compose down -v
```

---

### Option B — Run locally

**Prerequisites:** Java 21, Maven, an SMTP server (real or MailHog)

#### 1. (Optional) Start MailHog as a local SMTP server

```bash
# macOS
brew install mailhog && mailhog

# Docker
docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

#### 2. Run the application

```bash
mvn spring-boot:run
```

Or pass SMTP credentials directly as environment variables:

```bash
MAIL_HOST=smtp.example.com \
MAIL_PORT=587 \
MAIL_USERNAME=user@example.com \
MAIL_PASSWORD=secret \
  mvn spring-boot:run
```

---

## REST API

### Send an email

```
POST /api/v1/emails
Content-Type: application/json
```

**Request body**

```json
{
  "to": "alice@example.com",
  "subject": "Welcome aboard",
  "templateName": "welcome",
  "templateVariables": {
    "name": "Alice",
    "message": "Thanks for signing up!"
  }
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `to` | string | yes | Must be a valid email address |
| `subject` | string | yes | Cannot be blank |
| `templateName` | string | yes | Filename without `.html` under `templates/emails/` |
| `templateVariables` | object | no | Key/value pairs injected into the Thymeleaf template |

**Responses**

| Status | Meaning |
|---|---|
| `200 OK` | Email delivered |
| `400 Bad Request` | Validation failure — response body lists each invalid field |
| `500 Internal Server Error` | SMTP delivery failed after all retry attempts |

**200 response**

```json
{
  "message": "Email sent successfully",
  "to": "alice@example.com",
  "subject": "Welcome aboard"
}
```

**400 response**

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": {
    "to": "Must be a valid email address"
  },
  "timestamp": "2026-04-25T10:00:00Z"
}
```

---

### Health check

```
GET /api/v1/emails/health
```

```json
{ "status": "UP", "service": "email-service" }
```

---

## Email templates

Templates live in `src/main/resources/templates/emails/`. Reference them by filename without the `.html` extension.

### Built-in template: `welcome`

Variables: `name` (string), `message` (string).

To add a new template, create `templates/emails/my-template.html` and call the API with `"templateName": "my-template"`.

---

## Configuration reference

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `MAIL_HOST` | `localhost` | SMTP hostname |
| `MAIL_PORT` | `1025` | SMTP port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password |

### `application.yml`

| Key | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `resilience4j.retry.instances.emailRetry.max-attempts` | `3` | Max send attempts |
| `resilience4j.retry.instances.emailRetry.wait-duration` | `2s` | Delay between attempts |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI URL |
| `springdoc.api-docs.path` | `/v3/api-docs` | Raw OpenAPI spec URL |

---

## Running tests

```bash
# Unit + integration tests (no SMTP server needed)
mvn test
```

Tests use:
- **Mockito** — `SmtpEmailServiceTest` unit-tests the service in isolation
- **GreenMail** — `EmailIntegrationTest` spins up an in-memory SMTP server on port 3025 and verifies end-to-end delivery through the REST API

---

## Actuator endpoints

| Endpoint | URL |
|---|---|
| Health | `GET /actuator/health` |
| Info | `GET /actuator/info` |
| Metrics | `GET /actuator/metrics` |
