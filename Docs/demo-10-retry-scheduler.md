# Demo 10: Retry scheduler (delayed queue with ZSET)

## Problem
When work fails, immediate retries can cause retry storms. You need delayed retries with exponential backoff, jitter, and a DLQ.

## Why Redis fits
Redis Sorted Sets naturally model “run at time T” using the score as a timestamp.

## Data model (Redis)
- Due queue: `retry:queue` (ZSET)
  - Member: task JSON
  - Score: `runAtEpochMs`
- DLQ: `retry:dlq` (ZSET)

## Flow
- API `POST /retry/enqueue` adds a task to `retry:queue` with an initial delay.
- Worker poller:
  - Finds due tasks (`score <= now`)
  - Atomically claims/removes due tasks (Lua)
  - Executes task
  - If it fails, re-enqueues with backoff + jitter
  - After max attempts, moves to `retry:dlq`

## Local demo steps
Enqueue a task that fails 3 times before succeeding:
```bash
curl -i -X POST http://localhost:8080/retry/enqueue \
  -H "Content-Type: application/json" \
  -d '{"taskId":"t1","failTimes":3,"initialDelayMs":1000}'
```
Watch worker logs:
```bash
docker compose logs --tail=200 worker
```
Inspect queues:
```bash
docker exec -it redis-usage-redis redis-cli
ZRANGE retry:queue 0 -1 WITHSCORES
ZRANGE retry:dlq 0 -1 WITHSCORES
```

## Pitfalls
- Non-atomic claim leads to double-processing.
- Backoff without jitter can synchronize retries and amplify load.
- No max-attempt cap can cause infinite retries.

## Troubleshooting
- If nothing is processed, check worker logs and verify the keys in `worker/application.yml`.
- Inspect ZSET scores vs current time.

## Production notes
- Consider visibility timeouts / processing leases for long-running tasks.
- Emit metrics: retry count, queue depth, DLQ size, processing latency.
- For complex workflows, a dedicated workflow engine may fit better.
