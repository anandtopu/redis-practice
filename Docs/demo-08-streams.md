# Demo 08: Redis Streams (durable event processing)

## Problem
You need a lightweight durable queue/event log with consumer groups, retries, and replay.

## Why Redis fits
Streams provide append-only events with consumer group semantics, pending entries, and acknowledgements.

## Data model (Redis)
- Stream: `orders.stream`
- Consumer group: created by the worker on startup
- Message fields: order payload (`orderId`, `userId`, etc.)

## Flow
- API appends to stream on `POST /orders`
- Worker reads via consumer group
- On success, worker `XACK`s the message

## Local demo steps
Create an order:
```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: stream-demo-1" \
  -d '{"userId":"u-123","productId":1}'
```
Check worker logs:
```bash
docker compose logs --tail=200 worker
```
Inspect the stream:
```bash
docker exec -it redis-usage-redis redis-cli
XRANGE orders.stream - +
XINFO GROUPS orders.stream
```

## Pitfalls
- If consumers crash before `XACK`, pending entries accumulate.
- Poison messages can be retried forever without a DLQ strategy.
- Stream growth needs trimming/retention.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
XINFO STREAM orders.stream
XPENDING orders.stream orders-group
```

## Production notes
- Plan for retention: `XTRIM` or time-based keys.
- Add metrics: consumer lag, pending count, processing latency.
- For very high scale/long retention, Kafka is usually a better fit.
