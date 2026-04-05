# Demo 07: Pub/Sub (real-time notifications)

## Problem
You need low-latency fan-out notifications (e.g., “order created”, “cache warmed”) where durability is not required.

## Why Redis fits
Redis Pub/Sub is simple and fast for real-time messages to multiple subscribers.

## Data model (Redis)
- Channel/topic: configured in the API (`PubSubConfig`)
- Messages: strings/payloads published to that topic

## Flow
- API publishes to the topic
- One or more subscribers receive and log/process the message

## Local demo steps
Publish:
```bash
curl -i -X POST http://localhost:8080/pubsub/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"hello redis pubsub"}'
```
Check logs:
```bash
docker compose logs --tail=50 api
```

## Pitfalls
- Not durable: subscribers that are down will miss messages.
- No replay/history.
- Backpressure handling is limited.

## Troubleshooting
- Verify the API logs show publish + receive.
- If nothing is received, ensure the listener container is configured and connected.

## Production notes
- Use Pub/Sub for notifications, not for workflows that must-not-lose.
- For durable processing, use Redis Streams (next demo).
