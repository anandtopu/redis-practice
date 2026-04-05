package com.example.api.semaphore;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SemaphoreService {

  private final StringRedisTemplate redis;

  public SemaphoreService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public SemaphoreLease tryAcquire(String name, int permits, Duration ttl) {
    String semName = Objects.requireNonNull(name, "name");
    Duration leaseTtl = Objects.requireNonNull(ttl, "ttl");
    String token = Objects.requireNonNull(UUID.randomUUID().toString(), "token");

    for (int i = 0; i < permits; i++) {
      String slotKey = slotKey(semName, i);
      Boolean ok = redis.opsForValue().setIfAbsent(slotKey, token, leaseTtl);
      if (Boolean.TRUE.equals(ok)) {
        return new SemaphoreLease(semName, i, token, leaseTtl.toMillis());
      }
    }

    return null;
  }

  public boolean release(SemaphoreLease lease) {
    Objects.requireNonNull(lease, "lease");
    String key = slotKey(lease.name(), lease.slot());
    String current = redis.opsForValue().get(key);
    if (current == null) {
      return false;
    }
    if (!current.equals(lease.token())) {
      return false;
    }
    return Boolean.TRUE.equals(redis.delete(key));
  }

  private static String slotKey(String name, int slot) {
    Objects.requireNonNull(name, "name");
    return "sem:" + name + ":slot:" + slot;
  }
}
