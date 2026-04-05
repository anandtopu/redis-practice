# Redis CLI troubleshooting guide

This project is designed so you can debug every demo with a small set of Redis commands.

## Connect

```bash
docker exec -it redis-usage-redis redis-cli
```

## Key inspection

```text
DBSIZE
KEYS *
SCAN 0 MATCH product:* COUNT 100
TYPE product:1
TTL product:1
```

## Demo 01/02: Cache-aside (`product:{id}`)

```text
GET product:1
TTL product:1
DEL product:1
```

## Demo 02: Rate limiting

Fixed window:

```text
KEYS rate:fw:*
GET rate:fw:ip:127.0.0.1
TTL rate:fw:ip:127.0.0.1
```

Token bucket:

```text
KEYS rate:tb:*
HGETALL rate:tb:ip:127.0.0.1
TTL rate:tb:ip:127.0.0.1
```

## Demo 04: Distributed lock

```text
KEYS lock:*
GET lock:demo
TTL lock:demo
```

## Demo 05: Distributed semaphore

```text
KEYS sem:*
GET sem:explain:slot:0
TTL sem:explain:slot:0
```

## Demo 07: Pub/Sub

In one terminal:

```text
SUBSCRIBE demo.topic
```

In another terminal:

```text
PUBLISH demo.topic "hello"
```

## Demo 08: Streams (`orders.stream`, `jobs.stream`)

```text
XRANGE orders.stream - +
XINFO STREAM orders.stream
XINFO GROUPS orders.stream

XRANGE jobs.stream - +
XINFO STREAM jobs.stream
XINFO GROUPS jobs.stream
```

Pending entries (if consumers crash before ACK):

```text
XPENDING orders.stream orders.cg
XPENDING jobs.stream jobs.cg
```

## Demo 09: Sorted sets

```text
KEYS lb:*
ZREVRANGE lb:game-1 0 10 WITHSCORES
```

## Demo 10: Retry scheduler (ZSET delayed queue)

```text
ZRANGE retry:queue 0 -1 WITHSCORES
ZRANGE retry:dlq 0 -1 WITHSCORES
ZCARD retry:queue
ZCARD retry:dlq
```
