# Mail service

The service consumes `EmailVerificationRequested` commands from RabbitMQ and
delivers them through SMTP. It is intentionally not called by user-service.

## Delivery model

1. User-service creates the verification token and an `outbox_event` row in one
   PostgreSQL transaction.
2. User-service's outbox relay claims pending rows, publishes a persistent
   RabbitMQ message to `showhub.events` with routing key
   `email.verification.requested`, and marks a row published only after the
   broker confirms it. Expired relay leases are retried.
3. This service reserves `event_id` in `mail_delivery` with `ON CONFLICT DO
   NOTHING`, sends the email, and records it as delivered. Replayed broker
   messages therefore do not send a second email.

RabbitMQ retries SMTP failures five times before moving the message to
`showhub.mail.email.dlq`. Monitor and replay that queue after correcting the
underlying SMTP issue.

SMTP does not offer a transaction that can atomically commit with PostgreSQL,
so literal exactly-once transmission cannot be guaranteed across a process
crash after SMTP accepts a message. This implementation provides at-least-once
delivery plus idempotent consumer processing, and uses the event UUID as a
stable `Message-ID` to let compatible providers collapse that narrow retry
window.

## Local infrastructure

From the `Projects` directory, start Mailpit:

```bash
docker compose --env-file infrastructure/messaging/.env \
  -f infrastructure/messaging/docker-compose.yml up -d
```

Create a local PostgreSQL database named `mail_db`, then run mail-service with
`MAIL_DB_URL=jdbc:postgresql://localhost:5432/mail_db`,
`MAIL_DB_USERNAME`, and `MAIL_DB_PASSWORD`. Run user-service and mail-service
with the same RabbitMQ host, username, and password. RabbitMQ is a host
service on `localhost:5672`; use `MAIL_HOST=localhost` for Mailpit. Mailpit's
inbox is available at `http://localhost:8025`.

Build the image from the repository root:

```bash
docker build -f backend/mail-service/Dockerfile -t showhub/mail-service:local .
```
