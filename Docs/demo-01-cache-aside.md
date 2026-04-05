# Demo 01: Cache-aside + TTL (Redis as cache)

## Problem
Read-heavy endpoints (e.g., product catalog) put unnecessary load on Postgres and add latency.

## Why Redis fits
Redis provides low-latency reads and TTL-based expiry for data that can be slightly stale.

## Data model (Redis)
- Key: `product:{id}`
- Value: JSON (serialized `ProductDto`)
- TTL: configured in `ProductService`

## Flow
- **Read** `GET /products/{id}`
  - Try Redis `GET product:{id}`
  - If hit: return cached value
  - If miss: load from Postgres, write to Redis with TTL, return

## Local demo steps
1. Seed DB
```bash
curl -X POST http://localhost:8080/seed
```
2. First read (miss -> DB -> Redis)
```bash
curl http://localhost:8080/products/1
```
3. Second read (hit -> Redis)
```bash
curl http://localhost:8080/products/1
```

## Pitfalls
- Caching nulls incorrectly (or not caching them at all) can cause DB hot-spotting.
- Large values or too many keys can pressure memory and eviction.
- Cache stampede when many requests miss simultaneously.

## Troubleshooting
- Check if keys exist:
```bash
docker exec -it redis-usage-redis redis-cli
KEYS product:*
TTL product:1
GET product:1
```
- If always missing, verify Redis connectivity and TTL configuration.

## Production notes
- Prefer bounded TTLs with jitter to avoid synchronized expirations.
- Use key namespacing (`product:`) consistently.
- Consider request coalescing / single-flight for hot keys.
