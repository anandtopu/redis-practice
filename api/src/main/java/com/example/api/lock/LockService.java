package com.example.api.lock;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LockService {
  private final StringRedisTemplate redis;

  public LockService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public String tryAcquire(String lockKey, Duration ttl) {
    String token = UUID.randomUUID().toString();
    Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, ttl);
    return Boolean.TRUE.equals(ok) ? token : null;
  }

  public boolean release(String lockKey, String token) {
    String current = redis.opsForValue().get(lockKey);
    if (current == null) {
      return false;
    }
    if (!current.equals(token)) {
      return false;
    }
    return Boolean.TRUE.equals(redis.delete(lockKey));
  }
}
