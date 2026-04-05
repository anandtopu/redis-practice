# Demo 02: API rate limiting (Fixed window + Token bucket)

## Problem
Protect APIs and downstream systems from bursts, abuse, and accidental overload.

## Why Redis fits
Rate limiting must be consistent across multiple app instances. Redis provides atomic increments and scripts for correct distributed enforcement.

## Data model (Redis)
### Fixed window
- Key: `rate:fw:{client}`
- Value: counter (Redis String)
- TTL: window length

### Token bucket (Lua)
- Key: `rate:tb:{dimension}:{subject}`
- Value: Redis Hash with fields:
  - `tokens`
  - `ts` (last refill timestamp)
- TTL: bucket key expires after inactivity

## Flow
### Fixed window (`GET /limited`)
- `INCR` per client key
- On first increment, set `EXPIRE`
- Block when counter exceeds limit

### Token bucket (`GET /limited/token`)
- Compute key based on dimension
- Run Lua script to:
  - Refill tokens based on elapsed time
  - Consume a token if available
  - Return allowed/blocked

## Local demo steps
### Fixed window
Run multiple times quickly; after a few requests you should see `429`.
```bash
curl -i http://localhost:8080/limited
```

### Token bucket
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

## Pitfalls
- Fixed windows can be unfair at boundary edges.
- Key design mistakes can accidentally share limits across tenants/users.
- Time skew matters if you compute timestamps outside Redis.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS rate:*
HGETALL rate:tb:ip:*
TTL rate:tb:ip:*
```

## Production notes
- Prefer token bucket (or sliding window) for bursty traffic.
- Add metrics: allowed/blocked, latency, and hot-key monitoring.
- Consider per-route limits and different quotas per customer tier.
