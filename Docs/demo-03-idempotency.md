# Demo 03: Idempotency keys for POST endpoints

## Problem
Clients retry POST requests due to timeouts/network issues. Without protection, the same request can create duplicate orders.

## Why Redis fits
Redis can store short-lived idempotency records with TTL and enforce “only-once” processing across instances.

## Data model (Redis)
- Key: `idem:{key}`
- Value: response payload (or a marker + order id)
- TTL: idempotency retention window

## Flow
- Client sends `Idempotency-Key` header.
- API checks Redis for existing record.
  - If present: return stored result.
  - If absent: process request, store result with TTL, return.

## Local demo steps
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

## Pitfalls
- If TTL is too short, duplicates can reappear.
- If you store only a marker (not the response), you may not be able to return the same result.
- If you don’t make the “check + set” atomic, two instances can race.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS idem:*
TTL idem:demo-key-1
GET idem:demo-key-1
```

## Production notes
- Use a consistent key namespace per endpoint (or include request hash) to reduce collisions.
- Decide whether idempotency scope is per-user, per-tenant, or global.
- Consider storing a “processing” state to handle long-running requests.
