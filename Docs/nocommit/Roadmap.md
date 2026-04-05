**Roadmap**

Below is a practical 10-demo roadmap for a “Redis in real-world Java systems” project. It starts with the highest-signal patterns teams actually use, then moves into coordination, messaging, and failure handling so you build both implementation depth and operational understanding.

**Phase 1: Core Caching and API Protection**

1. **Read-through cache for release and policy queries**
   - Why first: easiest entry point and one of the most common Redis uses in production.
   - Best fit in your repo: `decision-service`, `policy-service`.
   - What to demonstrate:
     - Cache hit vs cache miss.
     - TTL-based expiration.
     - Cache invalidation after create/update.
     - Stale reads when invalidation is delayed or missed.
   - Real-world lesson:
     - Redis improves latency, but cache correctness is the hard part.
   - Failure drills:
     - Delete a DB row and show stale cached data.
     - Expire many keys together to show a DB spike.
   - What to document:
     - Cache key naming strategy.
     - TTL selection reasoning.
     - Invalidation strategy and fallback to DB.

2. **API rate limiting at the gateway**
   - Why early: extremely common and easy to visualize.
   - Best fit: `gateway-service`.
   - What to demonstrate:
     - Per-IP limit.
     - Per-user limit.
     - Per-tenant limit.
     - Burst allowance vs steady-state allowance.
   - Real-world lesson:
     - Simple counters are easy; fair limits across multiple app instances require Redis.
   - Failure drills:
     - Two app instances enforcing the same limit.
     - Bad window design causing unfair throttling.
   - What to document:
     - Fixed window vs sliding window vs token bucket.
     - Choosing the limiter dimension: IP, API key, tenant, user.

3. **Idempotency keys for POST endpoints**
   - Why early: crucial in payment/order-style APIs and very relevant to release creation/evaluation requests.
   - Best fit: `decision-service`, `policy-service`.
   - What to demonstrate:
     - Preventing duplicate request processing.
     - Returning the same result for retried requests.
     - TTL for idempotency records.
   - Real-world lesson:
     - Retries are normal in distributed systems; duplicate writes are not.
   - Failure drills:
     - Simulate client retry after timeout.
     - Use too-short TTL and show duplicate writes reappearing.
   - What to document:
     - Idempotency key lifecycle.
     - Difference between deduping requests and deduping events.

**Phase 2: Coordination and Concurrency**

4. **Distributed lock for release evaluation**
   - Why here: teaches Redis beyond caching.
   - Best fit: `decision-service`.
   - What to demonstrate:
     - Only one evaluation per release at a time.
     - Lock expiry.
     - Lock recovery after crash.
   - Real-world lesson:
     - Locks are useful, but easy to misuse if expiration and ownership are not handled carefully.
   - Failure drills:
     - Process crashes while holding lock.
     - Lock expires too early and second worker starts duplicate work.
   - What to document:
     - When to use a lock vs DB constraint vs queue.
     - Risks of relying on locks for correctness.

5. **Distributed semaphore for concurrency control**
   - Why next: builds on locking but models “up to N tasks at once.”
   - Best fit: `agent-service`, `policy-service`.
   - What to demonstrate:
     - Limit simultaneous expensive operations, such as explanations or simulations.
     - Fairness issues and starvation risks.
   - Real-world lesson:
     - Many systems need partial concurrency, not full serialization.
   - Failure drills:
     - Worker dies without releasing a slot.
     - Slot leakage under error conditions.
   - What to document:
     - Difference between lock, semaphore, and rate limiter.
     - How concurrency caps protect downstream systems.

6. **Job status tracking for long-running workflows**
   - Why here: very real pattern for async APIs and pairs nicely with Temporal.
   - Best fit: `agent-service`, `decision-service`.
   - What to demonstrate:
     - `queued`, `running`, `completed`, `failed`.
     - UI polling against Redis instead of the main DB.
     - Expiring old job status entries.
   - Real-world lesson:
     - Redis is excellent for short-lived operational state.
   - Failure drills:
     - Redis restart causing transient status loss.
     - Job completes but status cache is never updated.
   - What to document:
     - Why status belongs in Redis but final business records belong in Postgres.
     - Polling patterns and stale status handling.

**Phase 3: Events, Messaging, and Streaming**

7. **Pub/Sub for lightweight real-time notifications**
   - Why now: introduces messaging with minimal setup.
   - Best fit: `decision-service` to `agent-service` or UI notification layer.
   - What to demonstrate:
     - “Release evaluated” or “policy updated” notifications.
     - Multiple subscribers.
     - Fast fan-out.
   - Real-world lesson:
     - Pub/Sub is simple and low latency, but not durable.
   - Failure drills:
     - Subscriber disconnects and misses a message.
     - Service restart during event publication.
   - What to document:
     - When Pub/Sub is enough.
     - Why Pub/Sub is bad for must-not-lose workflows.

8. **Redis Streams for durable event processing**
   - Why after Pub/Sub: makes the durability tradeoff obvious.
   - Best fit: `ingestion-service`, `normalization-service`.
   - What to demonstrate:
     - Append events to a stream.
     - Consumer groups.
     - Replay.
     - Pending entries and ack flow.
   - Real-world lesson:
     - Streams are much closer to queue semantics than Pub/Sub.
   - Failure drills:
     - Consumer crashes before ack.
     - Pending list grows.
     - Poison message repeatedly fails.
   - What to document:
     - Pub/Sub vs Streams vs Kafka.
     - Why Redis Streams are strong for lightweight durable workflows, but not a Kafka replacement at large scale.

**Phase 4: Advanced Data Modeling**

9. **Sorted-set ranking for release risk dashboards**
   - Why here: demonstrates Redis data structures beyond key-value storage.
   - Best fit: `decision-service`.
   - What to demonstrate:
     - Rank releases by risk score, failed checks, or instability trend.
     - Top-N risky releases.
     - Time-windowed leaderboards.
   - Real-world lesson:
     - Redis shines when the read pattern matches the data structure.
   - Failure drills:
     - Ranking becomes stale when writes don’t update the set.
     - Confusing tie-breaking or score updates.
   - What to document:
     - Why sorted sets are better than recomputing rankings from scratch.
     - How to reconcile Redis rankings with source-of-truth DB records.

10. **Retry scheduler and delayed work queue**
   - Why last: combines coordination, timing, and operational behavior.
   - Best fit: `ingestion-service`, `agent-service`.
   - What to demonstrate:
     - Store failed work for retry at a future time.
     - Exponential backoff.
     - Dead-letter handling after max attempts.
   - Real-world lesson:
     - Retry systems are deceptively complex and a great way to learn timing edge cases.
   - Failure drills:
     - Same task retried too aggressively.
     - Retry storm after service recovery.
     - Lost delayed task after restart if Redis is misused.
   - What to document:
     - Immediate retry vs delayed retry.
     - Why jitter and retry caps matter.

**Recommended Build Order by Learning Outcome**
1. Read cache
2. Rate limiting
3. Idempotency keys
4. Distributed lock
5. Job status tracking
6. Pub/Sub
7. Streams
8. Semaphore
9. Sorted-set ranking
10. Retry scheduler

**How To Package Each Demo**
Use the same structure for every use case so the project feels intentional:

1. Problem statement
   - What real production problem this solves.

2. Why Redis fits
   - Fast lookup, TTL, atomicity, coordination, messaging, ranking, or short-lived state.

3. Suggested service fit
   - Which microservice owns the pattern in your repo.

4. Data model concept
   - Key/value, counter, hash, list, set, sorted set, stream, lock key.

5. Request and response flow
   - What happens on success, failure, retry, and timeout.

6. Local demo scenario
   - How to reproduce behavior live.

7. Pitfalls
   - The specific mistakes teams make.

8. Troubleshooting checklist
   - What to inspect first when behavior is wrong.

9. Production notes
   - Scaling, persistence, eviction, failover, observability.

**Common Pitfalls To Cover Across The Whole Roadmap**
- Using Redis as the only source of truth for critical business data.
- Forgetting TTLs and slowly filling memory.
- Poor key naming and lack of namespacing.
- Relying on non-atomic multi-step logic.
- Assuming Pub/Sub is durable.
- Using locks without safe expiration behavior.
- Ignoring serialization/versioning issues during deployments.
- Not planning for eviction policy effects.
- Missing observability for hit rate, key count, latency, memory, and reconnects.
- Treating local success as proof of production readiness.

**Good Final Deliverable Shape For Your Project**
- `Demo 1-3`: caching and API protection.
- `Demo 4-6`: concurrency and workflow coordination.
- `Demo 7-8`: eventing and stream processing.
- `Demo 9-10`: advanced data structures and delayed execution.
- One “Redis pitfalls and troubleshooting” guide shared across all demos.
- One comparison page: “When to use Redis vs Postgres vs Kafka vs Temporal.”

If you want, I can do the next step and turn this into a portfolio-ready chapter outline for your project docs, with a title, learning goals, demo scenario, and “what to show live” section for each of the 10 demos.