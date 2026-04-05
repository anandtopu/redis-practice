# Demo 09: Sorted sets (leaderboards / ranking)

## Problem
You need top-N queries and ranking by score efficiently (e.g., leaderboards, risk ranking, trending items).

## Why Redis fits
Redis Sorted Sets (`ZSET`) are purpose-built for ranked queries with efficient updates and top-N reads.

## Data model (Redis)
- Key: `leaderboard:{board}` (ZSET)
- Member: entity id (e.g., user)
- Score: numeric ranking value

## Flow
- Increment: `ZINCRBY leaderboard:{board} {delta} {member}`
- Top-N: `ZREVRANGE leaderboard:{board} 0 {n-1} WITHSCORES`

## Local demo steps
```bash
curl -i -X POST http://localhost:8080/leaderboard/incr \
  -H "Content-Type: application/json" \
  -d '{"board":"game-1","member":"alice","delta":10}'

curl -i -X POST http://localhost:8080/leaderboard/incr \
  -H "Content-Type: application/json" \
  -d '{"board":"game-1","member":"bob","delta":25}'

curl -i "http://localhost:8080/leaderboard/top?board=game-1&n=10"
```

## Pitfalls
- Score semantics: higher-is-better vs lower-is-better needs consistency.
- Tie-breaking: Redis uses lexicographic member ordering for equal scores.
- Stale rankings if you don’t update on write.

## Troubleshooting
```bash
docker exec -it redis-usage-redis redis-cli
KEYS leaderboard:*
ZREVRANGE leaderboard:game-1 0 10 WITHSCORES
```

## Production notes
- Consider time-windowed boards (daily/weekly) using separate keys.
- Reconcile Redis ranking with source-of-truth if needed.
