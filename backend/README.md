# ShowHub backend

This directory contains the gateway and independently deployable Java 21 /
Spring Boot services. It is not a Maven reactor: build, test, and run each
service from its own directory.

| Directory | Port | Owns |
| --- | ---: | --- |
| `apigateway` | 8080 | Public API edge, CORS, authentication enforcement, resilience |
| `user-service` | 8084 | Accounts, roles, OAuth, tokens, verification outbox |
| `mail-service` | 8085 | Email delivery ledger and SMTP delivery |
| `movie-service` | 8086 | Movie catalog |
| `theater-service` | 8087 | Theaters, auditoriums, seat maps, show schedules |
| `booking-service` | 8082 | Seat holds, bookings, expiry, payment state consumption |
| `payment-service` | 8083 | Payment intents, webhooks, refunds, payment outbox |

## Development

Use JDK 21. Maven applies Flyway migrations at startup against the database
configured for the service. Run a service from its directory:

```bash
mvn spring-boot:run
mvn test
mvn -DskipTests package
```

Run dependencies outside the applications: PostgreSQL and RabbitMQ are host
services; Redis and Mailpit have Docker Compose definitions in
[`../infrastructure`](../infrastructure/README.md).

## Communication rules

- Public calls enter only through the API gateway.
- Booking reads show inventory through a protected internal Theater Service
  endpoint; Payment reads booking quotes through a protected internal Booking
  Service endpoint.
- User-to-Mail and Payment-to-Booking notifications are RabbitMQ messages.
  Both publishers use transactional outboxes and consumers deduplicate events.
- No service reads another service's tables or migration files.

