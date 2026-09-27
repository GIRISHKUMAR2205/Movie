# Movie service

The movie catalog runs on port `8086` with context path `/movies`, and is exposed by the
gateway as `/api/v1/movies`.

```bash
export MOVIE_DB_URL='jdbc:postgresql://localhost:5432/movies'
export MOVIE_DB_USERNAME='postgres'
export MOVIE_DB_PASSWORD='change-me'
export SHOWHUB_JWT_SIGNING_KEY='BASE64_ENCODED_32_BYTE_MINIMUM_KEY'
cd backend/movie-service
mvn spring-boot:run
```

`GET /api/v1/movies` and `GET /api/v1/movies/{id}` expose published movies publicly.
`POST /api/v1/movies` and `PUT /api/v1/movies/{id}` require `ROLE_ADMIN`.

`presentationFormats` accepts `2D`, `3D`, `IMAX`, `IMAX_3D`, `4DX`, `DOLBY_CINEMA`,
`SCREENX`, `MX4D`, `VIP`, or a custom format prefixed with `CUSTOM:`. When
`hasSubtitles` is `true`, at least one `subtitleLanguages` entry is required.

Build the image from the repository root:

```bash
docker build -f backend/movie-service/Dockerfile -t showhub/movie-service:local .
```
