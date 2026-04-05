package com.example.worker.order;

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
public class OrderStreamConsumer implements ApplicationRunner {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  private final Counter processed;
  private final Counter parseError;
  private final Counter acked;
  private final Counter pollError;

  @Value("${app.redis.stream.orders}")
  private String streamKey;

  @Value("${app.redis.stream.consumerGroup}")
  private String consumerGroup;

  @Value("${app.redis.stream.consumerName}")
  private String consumerName;

  public OrderStreamConsumer(StringRedisTemplate redis, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
    this.redis = redis;
    this.objectMapper = objectMapper;

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.processed = registry.counter("demo.worker.stream.orders", "result", "processed");
    this.parseError = registry.counter("demo.worker.stream.orders", "result", "parse_error");
    this.acked = registry.counter("demo.worker.stream.orders", "result", "acked");
    this.pollError = registry.counter("demo.worker.stream.orders", "result", "poll_error");
  }

  @Override
  public void run(ApplicationArguments args) {
    String key = Objects.requireNonNull(streamKey, "streamKey");
    String group = Objects.requireNonNull(consumerGroup, "consumerGroup");

    try {
      redis.opsForStream().createGroup(key, ReadOffset.from("0-0"), group);
      System.out.println("[worker] created_consumer_group=" + group);
    } catch (Exception e) {
      System.out.println("[worker] consumer_group_exists_or_stream_missing");
    }

    new Thread(this::pollLoop, "order-stream-consumer").start();
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
            StreamReadOptions.empty().count(10).block(Objects.requireNonNull(Duration.ofSeconds(2), "block")),
            StreamOffset.create(key, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
          continue;
        }

        for (MapRecord<String, Object, Object> r : records) {
          Map<Object, Object> map = r.getValue();
          Object payloadObj = map.get("payload");
          if (payloadObj != null) {
            String payload = payloadObj.toString();
            try {
              OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
              System.out.println("[worker] order_created orderId=" + event.orderId() + " userId=" + event.userId());
              processed.increment();
            } catch (Exception ex) {
              System.out.println("[worker] failed_to_parse payload=" + payload);
              parseError.increment();
            }
          }

          redis.opsForStream().acknowledge(key, group, r.getId());
          acked.increment();
        }
      } catch (Exception e) {
        System.out.println("[worker] poll_error=" + e.getMessage());
        pollError.increment();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }
}
