package com.example.api.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketRateLimiter {

  private final StringRedisTemplate redis;
  private final DefaultRedisScript<Long> tokenBucketScript;

  private final Counter allowed;
  private final Counter blocked;

  public TokenBucketRateLimiter(StringRedisTemplate redis, MeterRegistry meterRegistry) {
    this.redis = redis;

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.allowed = registry.counter("demo.ratelimit.token_bucket", "result", "allowed");
    this.blocked = registry.counter("demo.ratelimit.token_bucket", "result", "blocked");

    String lua = "local key = KEYS[1]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refill_per_sec = tonumber(ARGV[2])\n" +
        "local now_ms = tonumber(ARGV[3])\n" +
        "local requested = tonumber(ARGV[4])\n" +
        "\n" +
        "local data = redis.call('HMGET', key, 'tokens', 'ts')\n" +
        "local tokens = tonumber(data[1])\n" +
        "local ts = tonumber(data[2])\n" +
        "\n" +
        "if tokens == nil then tokens = capacity end\n" +
        "if ts == nil then ts = now_ms end\n" +
        "\n" +
        "local delta_ms = now_ms - ts\n" +
        "if delta_ms < 0 then delta_ms = 0 end\n" +
        "local refill = (delta_ms / 1000.0) * refill_per_sec\n" +
        "tokens = math.min(capacity, tokens + refill)\n" +
        "\n" +
        "local allowed = 0\n" +
        "if tokens >= requested then\n" +
        "  tokens = tokens - requested\n" +
        "  allowed = 1\n" +
        "end\n" +
        "\n" +
        "redis.call('HSET', key, 'tokens', tokens, 'ts', now_ms)\n" +
        "local ttl_ms = math.floor((capacity / refill_per_sec) * 2000)\n" +
        "if ttl_ms < 1000 then ttl_ms = 1000 end\n" +
        "redis.call('PEXPIRE', key, ttl_ms)\n" +
        "return allowed\n";

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(lua);
    script.setResultType(Long.class);
    this.tokenBucketScript = script;
  }

  public boolean allow(String key, long capacity, double refillPerSecond, long requested) {
    String redisKey = Objects.requireNonNull(key, "key");
    long nowMs = System.currentTimeMillis();
    Long res = redis.execute(
        Objects.requireNonNull(tokenBucketScript, "tokenBucketScript"),
        Objects.requireNonNull(List.of(redisKey), "keys"),
        String.valueOf(capacity),
        String.valueOf(refillPerSecond),
        String.valueOf(nowMs),
        String.valueOf(requested)
    );
    boolean ok = res != null && res == 1L;
    if (ok) {
      allowed.increment();
    } else {
      blocked.increment();
    }
    return ok;
  }

  public static Duration recommendedTtl(long capacity, double refillPerSecond) {
    double seconds = capacity / refillPerSecond;
    return Duration.ofMillis((long) (seconds * 2000));
  }
}
