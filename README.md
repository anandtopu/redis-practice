# redis-practice

Local **Redis + Postgres + Spring Boot** (API + Worker) playground that demonstrates common Redis-backed system design patterns end-to-end.

This repo is intentionally practical:

- **API service** exposes HTTP endpoints that implement patterns like cache-aside, rate limiting, locks, idempotency, streams, pub/sub, sorted-set leaderboards, delayed retries, and job status tracking.
- **Worker service** consumes Redis Streams / scheduled work to show background processing.
- **Postgres** stores the “source of truth” entities (e.g., products/orders), while Redis is used for performance + distributed coordination.

The fastest way to use this project is with **Docker Compose** (Redis, RedisInsight, Postgres, API, Worker).

## Project layout

- **`api/`**
  - Spring Boot HTTP API (port `8080`)
- **`worker/`**
  - Spring Boot background worker + actuator (port `8081`)
- **`common/`**
  - Shared classes/config used by both services
- **`Docs/`**
  - Step-by-step docs per demo/pattern
- **`docker-compose.yml`**
  - Local stack (Redis + Postgres + API + Worker + RedisInsight)
- **`Redis_knowledge.md`**
  - Notes/learning material about Redis concepts

## Docs

- [`Docs/demo-01-cache-aside.md`](Docs/demo-01-cache-aside.md)
- [`Docs/demo-02-rate-limiting.md`](Docs/demo-02-rate-limiting.md)
- [`Docs/demo-03-idempotency.md`](Docs/demo-03-idempotency.md)
- [`Docs/demo-04-distributed-lock.md`](Docs/demo-04-distributed-lock.md)
- [`Docs/demo-05-distributed-semaphore.md`](Docs/demo-05-distributed-semaphore.md)
- [`Docs/demo-06-job-status-tracking.md`](Docs/demo-06-job-status-tracking.md)
- [`Docs/demo-07-pubsub.md`](Docs/demo-07-pubsub.md)
- [`Docs/demo-08-streams.md`](Docs/demo-08-streams.md)
- [`Docs/demo-09-sorted-sets-leaderboard.md`](Docs/demo-09-sorted-sets-leaderboard.md)
- [`Docs/demo-10-retry-scheduler.md`](Docs/demo-10-retry-scheduler.md)
- [`Docs/troubleshooting-redis-cli.md`](Docs/troubleshooting-redis-cli.md)

## What you’ll learn here (patterns)

- **Caching**: cache-aside, TTLs, invalidation on write
- **Rate limiting**: fixed window, token bucket (per IP/user/tenant)
- **Distributed coordination**: locks and semaphores
- **Messaging**: Pub/Sub and Streams
- **Reliability**: idempotency keys, retries with delayed scheduling
- **Async workflows**: job status tracking + polling
- **Ranking/leaderboards**: Sorted Sets

## Prereqs

- Docker Desktop

Optional (only if you want to run services outside Docker):

- Java `21`
- Maven `3.9+`

## Quickstart (recommended): Docker Compose

Start everything:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

- API: http://localhost:8080
- Worker (actuator metrics): http://localhost:8081
- RedisInsight UI: http://localhost:5540
  - Inside the docker network, Redis host is `redis` and port is `6379`
  - From your host machine, you can connect to `localhost:6379`

### Containers and ports

- **Redis**: `6379`
- **Postgres**: `5432` (db/user/password all `app` in docker compose)
- **API**: `8080`
- **Worker**: `8081`
- **RedisInsight**: `5540`

## Configuration

When running in Docker, the services are configured via environment variables in `docker-compose.yml`.

- **`SPRING_PROFILES_ACTIVE`**: `docker`
- **`DB_HOST`**: `postgres`
- **`DB_PORT`**: `5432`
- **`DB_NAME`**: `app`
- **`DB_USER`**: `app`
- **`DB_PASSWORD`**: `app`
- **`REDIS_HOST`**: `redis`
- **`REDIS_PORT`**: `6379`

## Running without Docker (optional)

If you prefer to run the Spring Boot apps on your host machine:

1. Start Redis and Postgres yourself (or start only infra from compose).
2. Export the same env vars shown above, but use `localhost` for hosts.
3. Run the services via Maven.

### Option A: Start only Redis + Postgres via Docker

```bash
docker compose up redis postgres redisinsight
```

Then run the apps locally (in separate terminals):

```bash
mvn -pl api spring-boot:run
```

```bash
mvn -pl worker spring-boot:run
```

### Option B: Build jars

```bash
mvn -DskipTests package
```

Then run:

```bash
java -jar api/target/api-*.jar
```

```bash
java -jar worker/target/worker-*.jar
```

## Handy commands

Tail logs:

```bash
docker compose logs --tail=200 api
docker compose logs --tail=200 worker
```

Open a Redis CLI inside the Redis container:

```bash
docker exec -it redis-usage-redis redis-cli
```

Reset local state (removes Redis/Postgres volumes):

```bash
docker compose down -v
```

## Runbook (verify each Redis pattern)

### 1) Seed Postgres

```bash
curl -X POST http://localhost:8080/seed
```

### 2) Cache-aside + TTL (Redis as cache)

First call should come from Postgres (and populate Redis):

```bash
curl http://localhost:8080/products/1
```

Second call should come from Redis:

```bash
curl http://localhost:8080/products/1
```

### 3) Cache invalidation on write

```bash
curl -X POST http://localhost:8080/products/1/price \
  -H "Content-Type: application/json" \
  -d '{"price": 99.99}'

curl http://localhost:8080/products/1
```

### 4) Session store (Redis-backed HTTP sessions)

```bash
curl -i -c cookies.txt \
  -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"u-123"}'

curl -i -b cookies.txt http://localhost:8080/me
```

### 5) Rate limiting (fixed window)

Run multiple times quickly; after a few requests you should see `429`:

```bash
curl -i http://localhost:8080/limited
```

### 6) Distributed lock demo (SET NX PX)

```bash
curl -i -X POST http://localhost:8080/lock/demo
```

### 7) Pub/Sub

Publish a message (API also subscribes and logs it):

```bash
curl -i -X POST http://localhost:8080/pubsub/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"hello redis pubsub"}'
```

Then check logs:

```bash
docker compose logs --tail=50 api
```

### 8) Idempotency key + Streams (API produces, worker consumes)

Create an order twice with the same `Idempotency-Key`:

```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-key-1" \
  -d '{"userId":"u-123","productId":1}'

curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-key-1" \
  -d '{"userId":"u-123","productId":1}'
```

Verify worker consumed the stream message:

```bash
docker compose logs --tail=100 worker
```

### 9) Leaderboard (Sorted Sets)

```bash
curl -i -X POST http://localhost:8080/leaderboard/incr \
  -H "Content-Type: application/json" \
  -d '{"board":"game-1","member":"alice","delta":10}'

curl -i -X POST http://localhost:8080/leaderboard/incr \
  -H "Content-Type: application/json" \
  -d '{"board":"game-1","member":"bob","delta":25}'

curl -i "http://localhost:8080/leaderboard/top?board=game-1&n=10"
```

### 10) Rate limiting (token bucket)

Per-IP:

```bash
curl -i "http://localhost:8080/limited/token?dimension=ip"
```

Per-user:

```bash
curl -i "http://localhost:8080/limited/token?dimension=user&userId=u-123"
```

Per-tenant:

```bash
curl -i "http://localhost:8080/limited/token?dimension=tenant&tenantId=t-1"
```

### 11) Distributed semaphore (limit concurrency)

Acquire (run multiple times; once permits are exhausted you should see `409`):

```bash
curl -i -X POST "http://localhost:8080/semaphore/acquire?name=explain&permits=2&ttlMs=15000"
```

Release (use the `slot` + `token` returned by acquire):

```bash
curl -i -X POST "http://localhost:8080/semaphore/release?name=explain&slot=0&token=REPLACE_ME"
```

### 12) Job status tracking (async job + polling)

Start a job:

```bash
curl -i -X POST http://localhost:8080/jobs/start \
  -H "Content-Type: application/json" \
  -d '{"kind":"demo","fail":false}'
```

Poll status:

```bash
curl -i http://localhost:8080/jobs/REPLACE_JOB_ID
```

Watch worker processing:

```bash
docker compose logs --tail=200 worker
```

### 13) Retry scheduler (delayed queue with ZSET)

Enqueue a task:

```bash
curl -i -X POST http://localhost:8080/retry/enqueue \
  -H "Content-Type: application/json" \
  -d '{"taskId":"t1","failTimes":3,"initialDelayMs":1000}'
```

Watch worker logs:

```bash
docker compose logs --tail=200 worker
```

## Observability (Actuator + Micrometer)

### API metrics

Actuator metrics endpoint:

```bash
curl "http://localhost:8080/actuator/metrics"
```

Demo metrics:

```bash
curl "http://localhost:8080/actuator/metrics/demo.cache.product"
curl "http://localhost:8080/actuator/metrics/demo.cache.product?tag=result:hit"

curl "http://localhost:8080/actuator/metrics/demo.ratelimit.fixed_window"
curl "http://localhost:8080/actuator/metrics/demo.ratelimit.token_bucket"
```

### Worker metrics

The worker exposes metrics on port `8081`:

```bash
curl "http://localhost:8081/actuator/metrics"
```

Demo metrics:

```bash
curl "http://localhost:8081/actuator/metrics/demo.worker.stream.orders"
curl "http://localhost:8081/actuator/metrics/demo.worker.stream.jobs"
curl "http://localhost:8081/actuator/metrics/demo.worker.retry"

curl "http://localhost:8081/actuator/metrics/demo.worker.retry?tag=result:processed_success"
curl "http://localhost:8081/actuator/metrics/demo.worker.stream.jobs?tag=result:failed"
```

## Redis CLI troubleshooting

See `Docs/troubleshooting-redis-cli.md`.

## Handy Redis commands

```bash
docker exec -it redis-usage-redis redis-cli
```

Inside `redis-cli`:

```text
KEYS *
TTL product:1
XRANGE orders.stream - +
XINFO GROUPS orders.stream
```
