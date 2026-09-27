# User Service

User Service owns local identity, username/password login, Google OAuth,
verified-email state, role-request audit history, and stateful refresh-token
rotation. It runs on port `8084` with context path `/users`; callers should use
the gateway prefix `/api/v1/users`.

## Required configuration

```bash
export USER_DB_URL='jdbc:postgresql://localhost:5432/user_db'
export USER_DB_USERNAME='postgres'
export USER_DB_PASSWORD='change-me'
export SHOWHUB_JWT_SIGNING_KEY='base64-encoded-key-at-least-32-bytes'
export GOOGLE_CLIENT_ID='google-client-id'
export GOOGLE_CLIENT_SECRET='google-client-secret'
export RABBITMQ_USERNAME='showhub'
export RABBITMQ_PASSWORD='showhub'
```

For a distinct web site or subdomain, configure
`SHOWHUB_REFRESH_COOKIE_DOMAIN`, `SHOWHUB_REFRESH_COOKIE_SECURE=true`, and
`SHOWHUB_REFRESH_COOKIE_SAME_SITE=None`; also set
`FRONTEND_APP_BASE_URL`, `FRONTEND_ALLOWED_ORIGINS`, and
`GATEWAY_ALLOWED_ORIGINS` to the exact HTTPS frontend origin. Local HTTP
development may set `SHOWHUB_REFRESH_COOKIE_SECURE=false`.

Run:

```bash
cd backend/user-service
mvn spring-boot:run
```

## Authentication model

- Login and signup return a short-lived access JWT in the response envelope and
  set an opaque refresh-token cookie.
- Refresh tokens are stored as hashes, rotated on use, and revocable. The
  frontend sends `X-Requested-With: XMLHttpRequest` when calling `POST
  /refresh`; allowed-origin CORS and that non-simple header protect this cookie
  endpoint. There is no general browser CSRF-token endpoint.
- OAuth authorization requests use signed state. The Google callback finishes
  on User Service and redirects the browser to the SPA with an access token in
  the fragment, which the SPA removes immediately.
- Signup and OAuth registration queue an email-verification command in the
  transactional outbox. Mail Service delivers it through SMTP.

## Main API

`POST /login`, `POST /signup`, `POST /refresh`, `GET /me`, `POST /signout`,
`POST /requestAdminPrivilege`, `GET /role-requests`, `GET
/email-verifications/verify`, and `POST /email-verifications/resend` are
available under `/users`. `ROLE_SUPERADMIN` review endpoints are under
`/users/super/role-requests`.

The OpenAPI UI is `http://localhost:8084/users/swagger.html`. Health and
metrics are at `/users/actuator/health` and `/users/actuator/prometheus`.

Build the image from the repository root:

```bash
docker build -f backend/user-service/Dockerfile -t showhub/user-service:local .
```
