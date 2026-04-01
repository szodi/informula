# Movie API

A Spring Boot REST API that aggregates movie data from [OMDB](http://www.omdbapi.com/) and [TMDB](https://www.themoviedb.org/documentation/api), returning a unified list of matching titles with year and director(s).

## Endpoint

```
GET /movies/{movieTitle}?api={apiName}
```

| Parameter    | Required | Values          | Description                        |
|--------------|----------|-----------------|------------------------------------|
| `movieTitle` | yes      | any string      | Full or partial movie title        |
| `api`        | yes      | `omdb`, `tmdb`  | External data source to query      |

### Example

```bash
curl "http://localhost:8080/movies/Avengers?api=omdb"
```

```json
{
  "movies": [
    {
      "Title": "The Avengers",
      "Year": "2012",
      "Director": ["Joss Whedon"]
    },
    {
      "Title": "Avengers: Endgame",
      "Year": "2019",
      "Director": ["Anthony Russo", "Joe Russo"]
    }
  ]
}
```

## Architecture

```
Controller
  └── MovieService          ← @Cacheable (Redis, 1h TTL)
        └── MovieApiService ← strategy interface
              ├── OmdbApiService  (search → N parallel detail calls)
              └── TmdbApiService  (search → N parallel credits calls)

SearchHistoryService        ← @Async, fire-and-forget MySQL write
```

### Performance

Both providers require two HTTP round-trips per result set: one search call followed by one detail/credits call **per matching movie**. These detail calls are issued concurrently via Reactor's non-blocking `Flux.flatMap` (concurrency = 10), so total latency is bounded by the slowest single call rather than the sum of all calls.

Responses are cached in Redis keyed on `{api}:{normalised_title}`. On a cache hit no external HTTP calls are made.

Search history is written asynchronously on a dedicated thread pool and never adds latency to the response. A failure in the persistence layer is logged and swallowed — it cannot affect the API response.

## Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Framework   | Spring Boot 4.0, Spring MVC         |
| HTTP client | Spring WebFlux `WebClient` (Netty)  |
| Cache       | Redis (Spring Cache, JSON serialised)|
| Database    | MySQL 8 via Spring Data JPA / Hibernate 7 |
| Build       | Maven 3, Java 25                    |
| Tests       | JUnit 5, Mockito, MockWebServer     |

## Prerequisites

### Local development
- Java 25+
- Maven 3.9+
- MySQL 8 database (schema auto-created on first run via `ddl-auto: update`)
- Redis instance

### Docker
- Docker 24+
- Docker Compose v2

## Configuration

All secrets are read from environment variables. No credentials in source.

| Variable        | Default       | Description              |
|-----------------|---------------|--------------------------|
| `OMDB_API_KEY`  | *(required)*  | OMDB API key             |
| `TMDB_API_KEY`  | *(required)*  | TMDB API key             |
| `DB_HOST`       | `localhost`   | MySQL host               |
| `DB_PORT`       | `3306`        | MySQL port               |
| `DB_NAME`       | `movieapi`    | MySQL database name      |
| `DB_USERNAME`   | `root`        | MySQL username           |
| `DB_PASSWORD`   | *(required)*  | MySQL password           |
| `REDIS_HOST`    | `localhost`   | Redis host               |
| `REDIS_PORT`    | `6379`        | Redis port               |
| `REDIS_PASSWORD`| *(empty)*     | Redis password           |

Obtain API keys at:
- OMDB: https://www.omdbapi.com/apikey.aspx
- TMDB: https://www.themoviedb.org/settings/api

## Running

### With Docker Compose (recommended)

```bash
cp .env.example .env
# Edit .env and set OMDB_API_KEY and TMDB_API_KEY
docker compose up --build
```

This starts MySQL 8.4, Redis 7, and the application. The app waits for both dependencies to pass their health checks before starting. MySQL data is persisted in a named volume (`mysql_data`).

To stop and remove containers (data volume is preserved):

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

### Building the image manually

```bash
docker build -t movie-api .
```

Run the image against an existing MySQL and Redis:

```bash
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PASSWORD=secret \
  -e REDIS_HOST=host.docker.internal \
  -e OMDB_API_KEY=your_key \
  -e TMDB_API_KEY=your_key \
  movie-api
```

### Without Docker

```bash
export OMDB_API_KEY=your_omdb_key
export TMDB_API_KEY=your_tmdb_key
export DB_USERNAME=root
export DB_PASSWORD=secret

mvn spring-boot:run
```

The server starts on port `8080` by default.

## Testing

### Manually
Under `/src/main/resources/http` folder you can find a test request file for each API.

### Automatically
```bash
mvn test
```

Unit tests use [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) to simulate external API responses without making real network calls. No running Redis or MySQL instance is required to run the test suite.

## Error Responses

| HTTP Status | Cause                                      |
|-------------|--------------------------------------------|
| `400`       | Missing/blank `api` or `movieTitle` param, or unknown API name |
| `502`       | Upstream OMDB or TMDB API is unavailable   |
| `500`       | Unexpected internal error                  |

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Unknown API 'foo'. Supported values: omdb, tmdb",
  "path": "/movies/Avengers",
  "timestamp": "2026-03-31T12:00:00"
}
```