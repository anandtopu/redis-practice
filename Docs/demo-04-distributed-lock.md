# Demo 04: Distributed lock (SET NX PX)

## Problem
You need to ensure only one instance performs a critical section at a time (e.g., a scheduled job or a protected workflow step).

## Why Redis fits
Redis supports atomic `SET key value NX PX` to implement a simple distributed mutex.

## Data model (Redis)
- Key: `lock:{name}`
- Value: random token (owner id)
- TTL: lock expiry to avoid permanent deadlocks

## Flow
- Acquire:
  - `SET lock:{name} {token} NX PX {ttl}`
- Release:
  - Only delete if current value matches `{token}` (ownership check)

## Local demo steps
```bash
curl -i -X POST http://localhost:8080/lock/demo
```
Run it multiple times in parallel to see lock contention.

## Pitfalls
- Releasing without ownership check can delete another owner’s lock.
- TTL too short can cause concurrent execution if work exceeds TTL.
- TTL too long causes slow recovery after crashes.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS lock:*
GET lock:demo
TTL lock:demo
```

## Production notes
- For stronger guarantees across failures, use a proven algorithm/approach and carefully reason about timeouts.
- Emit metrics: lock acquire latency, contention rate, and time spent holding the lock.
