package com.example.api.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RetryQueueService {

  private static final String QUEUE_KEY = "retry:queue";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public RetryQueueService(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  public void enqueue(EnqueueRetryRequest request) {
    Objects.requireNonNull(request, "request");

    String taskId = Objects.requireNonNull(request.taskId(), "taskId");

    long runAt = Instant.now().toEpochMilli() + request.initialDelayMs();

    try {
      String payload = objectMapper.writeValueAsString(Map.<String, String>of(
          "taskId", taskId,
          "attempt", "0",
          "failTimes", String.valueOf(request.failTimes())
      ));

      Boolean ok = redis.opsForZSet().add(QUEUE_KEY, payload, runAt);
      if (!Boolean.TRUE.equals(ok)) {
        throw new RuntimeException("failed_to_enqueue_retry");
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
