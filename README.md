# ShowHub

ShowHub is a movie and concert ticketing platform composed of independently
deployable Spring Boot services, a React/TypeScript web client, and shared
runtime infrastructure. All browser API traffic enters through the API gateway.

## Architecture

```text
React web client ──> API gateway ──> User / Movie / Theater / Booking / Payment
                                         │                         │
                                         └── RabbitMQ ──> Mail      └── Redis
                                              │
                                         PostgreSQL database per service
```

| Project | Port | Responsibility |
| --- | ---: | --- |
| `backend/apigateway` | 8080 | CORS, bearer-token validation, routing, resilience, metrics |
| `backend/booking-service` | 8082 | Seat holds, bookings, expiry, booking history |
| `backend/payment-service` | 8083 | Sandbox payment workflow, refunds, payment events |
| `backend/user-service` | 8084 | Identity, OAuth, verified email, refresh-token rotation |
| `backend/mail-service` | 8085 | Idempotent delivery of verification email commands |
| `backend/movie-service` | 8086 | Movie catalog administration and public movie lookup |
| `backend/theater-service` | 8087 | Theater setup, seats, prices, and show schedules |
| `frontend` | 5173 | Browser application |

Each service owns its database schema and Flyway migrations. Services never
read or write another service's database. Synchronous service calls use
protected internal HTTP endpoints; asynchronous state changes use RabbitMQ
events and transactional outboxes.

## Prerequisites

- JDK 21 and Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16+ running locally
- RabbitMQ running locally on `localhost:5672`
- Docker (for local Redis, Mailpit, and observability)

Create the local databases once:

```sql
CREATE DATABASE user_db;
CREATE DATABASE mail_db;
CREATE DATABASE movie_db;
CREATE DATABASE theater_db;
CREATE DATABASE booking_db;
CREATE DATABASE payment_db;
```

## Local quick start

1. Start the containerized local dependencies:

   ```bash
   docker compose -f infrastructure/cache/docker-compose.yml up -d
   docker compose -f infrastructure/messaging/docker-compose.yml up -d
   docker compose -f infrastructure/observability/docker-compose.yml up -d
   ```

2. Configure secrets in your shell or an untracked environment file. Use the
   same shared values in every service that consumes them:

   ```bash
   export SHOWHUB_JWT_SIGNING_KEY='base64-encoded-key-at-least-32-bytes'
   export SHOWHUB_INTERNAL_API_KEY='random-secret-at-least-32-characters'
   export RABBITMQ_USERNAME='showhub'
   export RABBITMQ_PASSWORD='showhub'
   export USER_DB_URL='jdbc:postgresql://localhost:5432/user_db'
   export USER_DB_USERNAME='postgres'
   export USER_DB_PASSWORD='change-me'
   export GOOGLE_CLIENT_ID='replace-me'
   export GOOGLE_CLIENT_SECRET='replace-me'
   export PAYMENT_WEBHOOK_SECRET='random-secret-at-least-32-characters'
   ```

   Set the equivalent `*_DB_URL`, `*_DB_USERNAME`, and `*_DB_PASSWORD` values
   for the other five services. See each service README for its complete list.

3. In separate terminals, start the services in this order:

   ```bash
   (cd backend/user-service && mvn spring-boot:run)
   (cd backend/mail-service && mvn spring-boot:run)
   (cd backend/movie-service && mvn spring-boot:run)
   (cd backend/theater-service && mvn spring-boot:run)
   (cd backend/booking-service && mvn spring-boot:run)
   (cd backend/payment-service && mvn spring-boot:run)
   (cd backend/apigateway && mvn spring-boot:run)
   ```

4. Configure and run the web client:

   ```bash
   cd frontend
   cp env.example .env
   npm install
   npm run dev
   ```

   Set `VITE_GATEWAY_URL=http://localhost:8080` in `frontend/.env` and include
   `http://localhost:5173` in `GATEWAY_ALLOWED_ORIGINS`.

The user-service OpenAPI UI is at `http://localhost:8084/users/swagger.html`.
Mailpit is at `http://localhost:8025`, Prometheus at `http://localhost:9090`,
and Grafana at `http://localhost:3001`.

## Build and test

Run Maven commands from an individual service directory:

```bash
mvn test
mvn -DskipTests package
```

Run web-client checks from `frontend`:

```bash
npm test
npm run build
```

## Deployment and quality gates

Every deployable application has a Dockerfile. Kubernetes manifests are kept
centralized in [`infrastructure/kubernetes`](infrastructure/kubernetes/README.md)
so the deployment topology remains visible in one place. GitHub Actions runs
regression tests, builds images, and—when configured with protected secrets—
deploys and publishes a health-check report. A release must meet the configured
health success-rate threshold after rollout.

## Further documentation

- [`backend/README.md`](backend/README.md) — backend conventions and service map
- [`frontend/README.md`](frontend/README.md) — browser application and auth model
- [`infrastructure/README.md`](infrastructure/README.md) — local dependencies,
  monitoring, deployment, and health reporting
