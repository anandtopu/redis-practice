package com.example.worker.retry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RetryScheduler implements ApplicationRunner {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final DefaultRedisScript<List> claimDueScript;
  private final Random random;

  private final Counter claimed;
  private final Counter processedSuccess;
  private final Counter scheduledRetry;
  private final Counter movedToDlq;
  private final Counter invalidPayload;
  private final Counter loopError;

  @Value("${app.redis.retry.queue}")
  private String queueKey;

  @Value("${app.redis.retry.dlq}")
  private String dlqKey;

  public RetryScheduler(StringRedisTemplate redis, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.random = new Random();

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.claimed = registry.counter("demo.worker.retry", "result", "claimed");
    this.processedSuccess = registry.counter("demo.worker.retry", "result", "processed_success");
    this.scheduledRetry = registry.counter("demo.worker.retry", "result", "scheduled_retry");
    this.movedToDlq = registry.counter("demo.worker.retry", "result", "moved_to_dlq");
    this.invalidPayload = registry.counter("demo.worker.retry", "result", "invalid_payload");
    this.loopError = registry.counter("demo.worker.retry", "result", "loop_error");

    String lua = "local key = KEYS[1]\n" +
        "local now = tonumber(ARGV[1])\n" +
        "local count = tonumber(ARGV[2])\n" +
        "local items = redis.call('ZRANGEBYSCORE', key, '-inf', now, 'LIMIT', 0, count)\n" +
        "for i=1,#items do\n" +
        "  redis.call('ZREM', key, items[i])\n" +
        "end\n" +
        "return items\n";

    DefaultRedisScript<List> script = new DefaultRedisScript<>();
    script.setScriptText(lua);
    script.setResultType(List.class);
    this.claimDueScript = script;
  }

  @Override
  public void run(ApplicationArguments args) {
    new Thread(this::loop, "retry-scheduler").start();
  }

  private void loop() {
    String qKey = Objects.requireNonNull(queueKey, "queueKey");
    String dKey = Objects.requireNonNull(dlqKey, "dlqKey");

    while (true) {
      try {
        long now = System.currentTimeMillis();

        List<String> payloads = claimDue(qKey, now, 10);
        if (payloads.isEmpty()) {
          Thread.sleep(300);
          continue;
        }

        for (String payload : payloads) {
          handlePayload(qKey, dKey, payload);
        }
      } catch (Exception e) {
        System.out.println("[retry] loop_error=" + e.getMessage());
        loopError.increment();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  private List<String> claimDue(String queueKey, long nowMs, int count) {
    List res = redis.execute(
        claimDueScript,
        List.of(queueKey),
        String.valueOf(nowMs),
        String.valueOf(count)
    );

    if (res == null || res.isEmpty()) {
      return List.of();
    }

    List<String> out = new ArrayList<>();
    for (Object o : res) {
      if (o != null) {
        out.add(o.toString());
      }
    }

    if (!out.isEmpty()) {
      claimed.increment(out.size());
    }
    return out;
  }

  private void handlePayload(String queueKey, String dlqKey, String payloadJson) throws Exception {
    Map<String, Object> map = objectMapper.readValue(payloadJson, new TypeReference<>() {
    });

    String taskId = Objects.toString(map.get("taskId"), "");
    int attempt = Integer.parseInt(Objects.toString(map.get("attempt"), "0"));
    int failTimes = Integer.parseInt(Objects.toString(map.get("failTimes"), "0"));

    if (taskId.isBlank()) {
      redis.opsForZSet().add(dlqKey, payloadJson, System.currentTimeMillis());
      invalidPayload.increment();
      return;
    }

    boolean shouldFail = attempt < failTimes;

    if (!shouldFail) {
      System.out.println("[retry] processed taskId=" + taskId + " attempt=" + attempt + " result=success");
      processedSuccess.increment();
      return;
    }

    int nextAttempt = attempt + 1;
    long baseDelayMs = backoffMs(nextAttempt);
    long jitterMs = random.nextInt(250);
    long nextRunAt = System.currentTimeMillis() + baseDelayMs + jitterMs;

    map.put("attempt", String.valueOf(nextAttempt));
    String nextPayload = objectMapper.writeValueAsString(map);

    if (nextAttempt >= 10) {
      redis.opsForZSet().add(dlqKey, nextPayload, System.currentTimeMillis());
      System.out.println("[retry] taskId=" + taskId + " moved_to_dlq attempt=" + nextAttempt);
      movedToDlq.increment();
      return;
    }

    redis.opsForZSet().add(queueKey, nextPayload, nextRunAt);
    System.out.println("[retry] taskId=" + taskId + " scheduled attempt=" + nextAttempt + " runAtMs=" + nextRunAt);
    scheduledRetry.increment();
  }

  private static long backoffMs(int attempt) {
    long base = 500L;
    long max = Duration.ofSeconds(30).toMillis();
    long delay = base * (1L << Math.min(attempt, 10));
    return Math.min(delay, max);
  }
}
