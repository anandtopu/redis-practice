package com.example.worker.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobsStreamConsumer implements ApplicationRunner {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final JobStatusWriter jobStatusWriter;

  private final Counter processed;
  private final Counter completed;
  private final Counter failed;
  private final Counter parseError;
  private final Counter acked;
  private final Counter pollError;

  @Value("${app.redis.stream.jobs}")
  private String streamKey;

  @Value("${app.redis.stream.jobsConsumerGroup}")
  private String consumerGroup;

  @Value("${app.redis.stream.jobsConsumerName}")
  private String consumerName;

  public JobsStreamConsumer(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      JobStatusWriter jobStatusWriter,
      MeterRegistry meterRegistry
  ) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.jobStatusWriter = jobStatusWriter;

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.processed = registry.counter("demo.worker.stream.jobs", "result", "processed");
    this.completed = registry.counter("demo.worker.stream.jobs", "result", "completed");
    this.failed = registry.counter("demo.worker.stream.jobs", "result", "failed");
    this.parseError = registry.counter("demo.worker.stream.jobs", "result", "parse_error");
    this.acked = registry.counter("demo.worker.stream.jobs", "result", "acked");
    this.pollError = registry.counter("demo.worker.stream.jobs", "result", "poll_error");
  }

  @Override
  public void run(ApplicationArguments args) {
    String key = Objects.requireNonNull(streamKey, "streamKey");
    String group = Objects.requireNonNull(consumerGroup, "consumerGroup");

    try {
      redis.opsForStream().createGroup(key, ReadOffset.from("0-0"), group);
      System.out.println("[jobs] created_consumer_group=" + group);
    } catch (Exception e) {
      System.out.println("[jobs] consumer_group_exists_or_stream_missing");
    }

    new Thread(this::pollLoop, "jobs-stream-consumer").start();
  }

  private void pollLoop() {
    String key = Objects.requireNonNull(streamKey, "streamKey");
    String group = Objects.requireNonNull(consumerGroup, "consumerGroup");
    String name = Objects.requireNonNull(consumerName, "consumerName");
    Consumer consumer = Consumer.from(group, name);

    while (true) {
      try {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
            consumer,
            StreamReadOptions.empty().count(5).block(Duration.ofSeconds(2)),
            StreamOffset.create(key, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
          continue;
        }

        for (MapRecord<String, Object, Object> r : records) {
          Map<Object, Object> map = r.getValue();
          Object payloadObj = map.get("payload");
          if (payloadObj == null) {
            redis.opsForStream().acknowledge(key, group, r.getId());
            acked.increment();
            continue;
          }

          String payloadJson = payloadObj.toString();
          JobPayload payload;
          try {
            payload = objectMapper.readValue(payloadJson, JobPayload.class);
          } catch (Exception ex) {
            parseError.increment();
            redis.opsForStream().acknowledge(key, group, r.getId());
            acked.increment();
            continue;
          }

          String jobId = payload.jobId();
          jobStatusWriter.write(jobStatusWriter.status(jobId, "running", 0, null, null));

          try {
            for (int p = 10; p <= 100; p += 30) {
              Thread.sleep(300);
              jobStatusWriter.write(jobStatusWriter.status(jobId, "running", p, null, null));
            }

            if (payload.fail()) {
              throw new RuntimeException("simulated_failure");
            }

            jobStatusWriter.write(jobStatusWriter.status(jobId, "completed", 100, "ok", null));
            completed.increment();
          } catch (Exception ex) {
            jobStatusWriter.write(jobStatusWriter.status(jobId, "failed", 100, null, ex.getMessage()));
            failed.increment();
          } finally {
            redis.opsForStream().acknowledge(key, group, r.getId());
            acked.increment();
          }

          processed.increment();
        }
      } catch (Exception e) {
        System.out.println("[jobs] poll_error=" + e.getMessage());
        pollError.increment();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }
}
