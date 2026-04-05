# Demo 05: Distributed semaphore (limit concurrency)

## Problem
You need to cap concurrent executions of an expensive operation (e.g., generating explanations) to protect downstream dependencies.

## Why Redis fits
Redis can coordinate concurrency limits across many app instances using atomic operations and TTL.

## Data model (Redis)
- Base name: `semaphore:{name}`
- Slot keys: `semaphore:{name}:slot:{i}` (for `i = 0..permits-1`)
- Value: random token (lease owner)
- TTL: lease timeout to avoid permanent slot leaks

## Flow
- Acquire:
  - Try to `SET NX PX` on each slot key until one succeeds
  - Return `{slot, token}` as the lease
- Release:
  - Delete only if value matches `{token}` (ownership)

## Local demo steps
Acquire permits:
```bash
curl -s -X POST "http://localhost:8080/semaphore/acquire?name=explain&permits=2&ttlMs=15000"
curl -s -X POST "http://localhost:8080/semaphore/acquire?name=explain&permits=2&ttlMs=15000"
```
A third acquire should fail (no permits):
```bash
curl -i -X POST "http://localhost:8080/semaphore/acquire?name=explain&permits=2&ttlMs=15000"
```
Release (use the `slot` + `token` from the acquire response):
```bash
curl -i -X POST "http://localhost:8080/semaphore/release?name=explain&slot=0&token=REPLACE_ME"
```

## Pitfalls
- Slot leakage if TTL is too large and clients crash.
- Premature expiry if TTL is too short and work exceeds TTL.
- Fairness/starvation: first-available slot strategies can be unfair under load.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS semaphore:explain:slot:*
GET semaphore:explain:slot:0
TTL semaphore:explain:slot:0
```

## Production notes
- Consider renewal/heartbeat if work can exceed TTL.
- Expose metrics: permits in use, acquire wait time, failures.
- Prefer a queue for strict fairness.
