# ShowHub API gateway

The gateway is the only browser-facing backend. It listens on port `8080` and
routes `/api/v1/**` requests to the owning service. It validates bearer JWTs,
sets CORS policy, adds correlation IDs, and applies Resilience4j circuit
breaking, retry, rate limiting, bulkhead limits, and downstream time limits.

## Run locally

```bash
export SHOWHUB_JWT_SIGNING_KEY='same-base64-key-used-by-services'
export GATEWAY_ALLOWED_ORIGINS='http://localhost:5173'
cd backend/apigateway
mvn spring-boot:run
```

By default the gateway expects User, Booking, Payment, Movie, and Theater
services at their local ports. Override a target with `USER_SERVICE_URL`,
`BOOKING_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `MOVIE_SERVICE_URL`, or
`THEATER_SERVICE_URL`. `CATALOG_SERVICE_URL` is reserved for the future concert
catalog route.

## Public routes

| Gateway route | Destination |
| --- | --- |
| `/api/v1/users/**` | User Service |
| `/api/v1/movies/**` | Movie Service |
| `/api/v1/theaters/**`, `/api/v1/shows` | Theater Service |
| `/api/v1/shows/{id}/seats`, `/api/v1/bookings/**` | Booking Service |
| `/api/v1/payments/**` | Payment Service |
| `/api/v1/concerts/**`, `/api/v1/venues/**` | reserved Catalog Service route |

Only safe `GET` and `HEAD` requests are retried. Do not add retries to a
state-changing route unless its end-to-end idempotency contract is explicit.
Gateway rate limits are local to an instance; move the limiter state to a
shared store before horizontally scaling it.

Health and metrics: `GET /actuator/health`, `GET /actuator/prometheus`.

Build the image from the repository root:

```bash
docker build -f backend/apigateway/Dockerfile -t showhub/api-gateway:local .
```
