package com.example.api.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
  private final StringRedisTemplate redis;

  private final Counter allowed;
  private final Counter blocked;

  public RateLimitService(StringRedisTemplate redis, MeterRegistry meterRegistry) {
    this.redis = redis;

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.allowed = registry.counter("demo.ratelimit.fixed_window", "result", "allowed");
    this.blocked = registry.counter("demo.ratelimit.fixed_window", "result", "blocked");
  }

  public boolean allowFixedWindow(String key, int limit, Duration window) {
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, window);
    }
    boolean ok = count != null && count <= limit;
    if (ok) {
      allowed.increment();
    } else {
      blocked.increment();
    }
    return ok;
  }
}
