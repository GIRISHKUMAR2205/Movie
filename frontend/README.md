# ShowHub frontend

React, TypeScript, Redux Toolkit, and RTK Query frontend for the ShowHub movie and concert ticket platform.

## API boundary

The browser talks only to the API gateway. Configure its public origin in `.env`:

```env
VITE_GATEWAY_URL=http://localhost:8080
```

Do not point the frontend at an individual service. RTK Query sends the
in-memory access token as a bearer token and keeps the opaque refresh token in
its HTTP-only cookie. Refresh sends `X-Requested-With: XMLHttpRequest`; the
gateway's exact-origin CORS policy protects that cookie endpoint. There is no
general CSRF-token endpoint.

For Google login through the gateway, register and configure this callback:

```text
http://localhost:8080/api/v1/users/login/oauth2/code/google
```

Set `GOOGLE_REDIRECT_URI` to the same value in the user service.

## Run

```bash
npm install
npm run dev
```

The default Vite address is `http://localhost:5173`. The gateway must include that origin in `GATEWAY_ALLOWED_ORIGINS`.

## Application areas

- Public movie/concert discovery and show filtering
- Authenticated seat selection, booking, checkout, and payment tracking
- Account verification and role-request audit progress
- Movie and concert catalog administration
- Theater, auditorium, seat type, seat layout, pricing, and recurring schedule administration
- Super-admin role approval, rejection, revocation, reactivation, and audit views

User, movie, theater, show-discovery, seat-inventory, booking, and payment
screens target implemented gateway routes. Concert and venue discovery use a
reserved Catalog Service route and show a recoverable unavailable state until
that service is introduced.

## Container image

Build the production image from the repository root. The gateway URL is a Vite
build-time value, so provide the public URL that browsers will use:

```bash
docker build -f frontend/Dockerfile \
  --build-arg VITE_GATEWAY_URL=https://showhub.example.com \
  -t showhub/frontend:local frontend
```

The image serves the single-page application through Nginx and includes an SPA
fallback for client-side routes. Kubernetes deployment configuration is in
[`../infrastructure/kubernetes`](../infrastructure/kubernetes/README.md).

## Verification

```bash
npm test
npm run build
```
