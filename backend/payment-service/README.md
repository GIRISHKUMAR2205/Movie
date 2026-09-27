# Payment service

Payment service runs on port `8083` and is exposed through the gateway at
`/api/v1/payments`. The included provider is a sandbox adapter: it records payment
intent state but does not charge real money or accept card numbers.

Required environment:

```bash
export PAYMENT_DB_URL='jdbc:postgresql://localhost:5432/payment_db'
export PAYMENT_DB_USERNAME='postgres'
export PAYMENT_DB_PASSWORD='change-me'
export PAYMENT_WEBHOOK_SECRET='replace-with-at-least-32-random-characters'
export SHOWHUB_JWT_SIGNING_KEY='BASE64_ENCODED_32_BYTE_MINIMUM_KEY'
export SHOWHUB_INTERNAL_API_KEY='same-at-least-32-character-secret-used-by-booking-service'
export RABBITMQ_USERNAME='showhub'
export RABBITMQ_PASSWORD='showhub'
```

Run it with:

```bash
cd backend/payment-service
mvn spring-boot:run
```

API summary:

- `POST /api/v1/payments` — authenticated; accepts `bookingId` and `paymentMethod`, requires
  `Idempotency-Key`, and creates a sandbox checkout from the trusted Booking Service quote.
- `GET /api/v1/payments` and `GET /api/v1/payments/{id}` — authenticated owner-only reads.
- `POST /api/v1/payments/{id}/cancel` — owner can cancel a pending payment.
- `POST /api/v1/payments/{id}/refunds` — `ROLE_ADMIN` only; missing amount means refund all remaining funds.
- `POST /api/v1/payments/webhooks/sandbox` — provider callback secured by timestamped HMAC-SHA256.

Webhook signatures are lowercase hexadecimal HMAC-SHA256 of
`<X-ShowHub-Timestamp>.<raw-json-body>` using `PAYMENT_WEBHOOK_SECRET`. Events are
persistently deduplicated by `eventId`; reusing an ID with different content is rejected.
Every committed payment transition is written to a transactional outbox and delivered to
Booking Service through RabbitMQ with publisher confirms. Booking Service deduplicates those
events before confirming or releasing seats.

Before using this in production, replace `SandboxPaymentProvider` with the selected
provider's hosted-checkout adapter. Amount and currency are already obtained from Booking
Service and cannot be submitted by the browser. Never extend this API to accept PAN, CVV,
or other raw card data.

Build the image from the repository root:

```bash
docker build -f backend/payment-service/Dockerfile -t showhub/payment-service:local .
```
