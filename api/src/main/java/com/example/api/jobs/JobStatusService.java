package com.example.api.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobStatusService {

  private static final Duration JOB_TTL = Duration.ofMinutes(30);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Value("${app.redis.stream.jobs:jobs.stream}")
  private String jobsStream;

  public JobStatusService(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  public JobStatus startJob(StartJobRequest request) {
    Objects.requireNonNull(request, "request");

    String jobId = UUID.randomUUID().toString();
    JobStatus status = new JobStatus(jobId, "queued", 0, Instant.now(), null, null);
    writeStatus(status);

    try {
      String payload = objectMapper.writeValueAsString(Map.<String, String>of(
          "jobId", jobId,
          "kind", Objects.toString(request.kind(), ""),
          "fail", String.valueOf(request.fail())
      ));

      MapRecord<String, String, String> record = StreamRecords.newRecord()
          .ofMap(Map.<String, String>of("type", "job", "payload", payload))
          .withStreamKey(Objects.requireNonNull(jobsStream, "jobsStream"));

      RecordId id = redis.opsForStream().add(record);
      if (id == null) {
        throw new RuntimeException("failed_to_enqueue_job");
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return status;
  }

  public JobStatus getJob(String jobId) {
    String key = jobKey(jobId);
    String json = redis.opsForValue().get(key);
    if (json == null) {
      return null;
    }
    try {
      return objectMapper.readValue(json, JobStatus.class);
    } catch (JsonProcessingException e) {
      redis.delete(key);
      return null;
    }
  }

  public void writeStatus(JobStatus status) {
    Objects.requireNonNull(status, "status");
    try {
      String json = objectMapper.writeValueAsString(status);
      redis.opsForValue().set(jobKey(status.jobId()), json, JOB_TTL);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static String jobKey(String jobId) {
    String id = Objects.requireNonNull(jobId, "jobId");
    return "job:" + id;
  }
}
