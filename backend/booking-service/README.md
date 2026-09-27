# Booking Service

Booking Service owns seat holds, booking totals, expiry, confirmation, cancellation, and the customer booking history.

## Double-booking protection

1. One Redis Lua script checks and creates every requested seat key atomically.
2. The booking and seat snapshots are written in one PostgreSQL transaction.
3. A partial unique PostgreSQL index on `(show_id, seat_id)` prevents more than one `HELD` or `CONFIRMED` reservation even if Redis is unavailable or loses data.
4. Expired/cancelled/failed bookings release both the durable reservation and the matching Redis lease.

Redis is required and the service fails closed when it cannot acquire a hold. Redis is not the source of truth; PostgreSQL is.

## Prerequisites

Create a local PostgreSQL database:

```sql
CREATE DATABASE booking_db;
```

Start Redis from the repository root:

```bash
docker compose -f infrastructure/cache/docker-compose.yml up -d
```

RabbitMQ remains a host service, consistent with the other ShowHub services.

Required shared environment variables:

```bash
export SHOWHUB_JWT_SIGNING_KEY='<same Base64 key used by user-service>'
export SHOWHUB_INTERNAL_API_KEY='<random secret of at least 32 characters>'
export RABBITMQ_USERNAME=showhub
export RABBITMQ_PASSWORD=showhub
```

Use the same `SHOWHUB_INTERNAL_API_KEY` for Theater Service and Payment Service.

Run:

```bash
cd backend/booking-service
mvn spring-boot:run
```

The service listens on port `8082`; browser requests should use the API Gateway on port `8080`.

Build a container image from the repository root with:

```bash
docker build -f backend/booking-service/Dockerfile -t showhub/booking-service:local .
```

Kubernetes runtime settings and probes are centralized in
[`../../infrastructure/kubernetes`](../../infrastructure/kubernetes/README.md).

## API

- `GET /api/v1/shows/{showId}/seats` — public availability overlay
- `POST /api/v1/bookings` — hold seats; requires bearer token and `Idempotency-Key`
- `GET /api/v1/bookings` — current user's bookings
- `GET /api/v1/bookings/{bookingId}` — owned booking
- `POST /api/v1/bookings/{bookingId}/cancel` — cancel an unpaid booking
- `POST /internal/v1/bookings/{bookingId}/payment-intent` — trusted payment quote used only by Payment Service

Payment status events are consumed idempotently from RabbitMQ. Malformed or permanently invalid events go to the configured dead-letter queue.
