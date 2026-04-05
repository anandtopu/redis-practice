package com.example.worker.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobStatusWriter {

  private static final Duration JOB_TTL = Duration.ofMinutes(30);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public JobStatusWriter(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  public void write(JobStatus status) {
    Objects.requireNonNull(status, "status");
    try {
      String json = objectMapper.writeValueAsString(status);
      redis.opsForValue().set(jobKey(status.jobId()), json, JOB_TTL);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public JobStatus status(String jobId, String status, int progress, String result, String error) {
    return new JobStatus(jobId, status, progress, Instant.now(), result, error);
  }

  private static String jobKey(String jobId) {
    String id = Objects.requireNonNull(jobId, "jobId");
    return "job:" + id;
  }
}
