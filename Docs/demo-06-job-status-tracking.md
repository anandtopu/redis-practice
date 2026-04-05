# Demo 06: Job status tracking (async workflows)

## Problem
Async workflows need a fast “status by job id” lookup for UI polling (queued/running/completed/failed) without hammering Postgres.

## Why Redis fits
Redis is ideal for short-lived operational state with TTL and extremely fast reads.

## Data model (Redis)
- Status key: `job:{jobId}` (Redis Hash)
  - `state` (queued|running|completed|failed)
  - `message` (optional)
  - `updatedAtMs`
- Stream: `jobs.stream` (Redis Stream)
  - Job payload fields: `jobId`, `kind`, etc.
- TTL: status key expires after completion window

## Flow
- API `POST /jobs/start`
  - Create `jobId`
  - Write status `queued` with TTL
  - Append payload to `jobs.stream`
- Worker consumes `jobs.stream`
  - Update status to `running`
  - Process
  - Update to `completed` or `failed`
- API `GET /jobs/{jobId}` reads the status key

## Local demo steps
Start a job:
```bash
curl -s -X POST http://localhost:8080/jobs/start \
  -H "Content-Type: application/json" \
  -d '{"kind":"demo","fail":false}'
```
Poll status:
```bash
curl -s http://localhost:8080/jobs/JOB_ID
```
Force a failure:
```bash
curl -s -X POST http://localhost:8080/jobs/start \
  -H "Content-Type: application/json" \
  -d '{"kind":"demo","fail":true}'
```
Watch worker logs:
```bash
docker compose logs --tail=200 worker
```

## Pitfalls
- Status in Redis is not a permanent system of record.
- Missing TTL can slowly leak memory.
- “Job finished but status not updated” requires defensive timeout handling.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS job:*
HGETALL job:JOB_ID
TTL job:JOB_ID
XRANGE jobs.stream - +
XINFO GROUPS jobs.stream
```

## Production notes
- Consider a terminal status + immutable audit record in Postgres.
- Add a watchdog for stuck jobs.
- Use consistent job id generation and namespace by environment.
