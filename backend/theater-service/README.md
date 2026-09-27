# Theater service

The service runs on port `8087` with context path `/theaters`. Through the API gateway,
its API is available at `/api/v1/theaters`.

Required environment variables:

```bash
export THEATER_DB_URL='jdbc:postgresql://localhost:5432/theater_db'
export THEATER_DB_USERNAME='postgres'
export THEATER_DB_PASSWORD='change-me'
export SHOWHUB_JWT_SIGNING_KEY='BASE64_ENCODED_32_BYTE_MINIMUM_KEY'
export SHOWHUB_INTERNAL_API_KEY='same-at-least-32-character-secret-used-by-booking-service'
```

Run it with:

```bash
cd backend/theater-service
mvn spring-boot:run
```

Only a fresh access token with `ROLE_THEATER_ADMIN` can create or modify theater data.
The role is requested through user-service, which requires verified email, and must then be
approved by a super-admin. Refresh or sign in again after approval to obtain the new role claim.

Main admin endpoints:

- `POST /api/v1/theaters` — register a theater.
- `POST /api/v1/theaters/{theaterId}/auditoriums` — add an auditorium.
- `POST /api/v1/theaters/{theaterId}/auditoriums/{auditoriumId}/seat-types` — define a category and default price.
- `POST /api/v1/theaters/{theaterId}/auditoriums/{auditoriumId}/seats` — add individual seats to categories.
- `POST /api/v1/theaters/{theaterId}/auditoriums/{auditoriumId}/show-schedules` — create one daily show per date in the inclusive range.
- `GET /api/v1/theaters/{theaterId}/schedule?from=2026-10-01&to=2026-10-31` — public schedule lookup.
- `GET /api/v1/shows?contentType=MOVIE&city=Bengaluru&date=2026-10-01` — public show discovery through the gateway.

Each scheduled show records optional intermission start/duration, post-show break, and a price
snapshot for every seat type. The auditorium is unavailable until the post-show break ends.
Schedule requests include `contentType` (`MOVIE` or `CONCERT`) so discovery can filter them.

Build the image from the repository root:

```bash
docker build -f backend/theater-service/Dockerfile -t showhub/theater-service:local .
```
