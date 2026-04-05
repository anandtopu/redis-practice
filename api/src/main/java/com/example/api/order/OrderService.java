package com.example.api.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

  private final OrderRepository orderRepository;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Value("${app.redis.stream.orders}")
  private String ordersStreamKey;

  public OrderService(OrderRepository orderRepository, StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public OrderEntity createOrderIdempotent(String idempotencyKey, CreateOrderRequest request) {
    String key = "idem:order:" + idempotencyKey;

    String existing = redis.opsForValue().get(key);
    if (existing != null) {
      try {
        long orderId = Long.parseLong(existing);
        return orderRepository.findById(orderId).orElseThrow();
      } catch (Exception ignored) {
        redis.delete(key);
      }
    }

    OrderEntity saved = orderRepository.save(new OrderEntity(request.userId(), request.productId(), Instant.now()));

    redis.opsForValue().set(key, String.valueOf(saved.getId()), IDEMPOTENCY_TTL);

    OrderCreatedEvent event = new OrderCreatedEvent(saved.getId(), saved.getUserId(), saved.getProductId(), saved.getCreatedAt());
    try {
      String payload = objectMapper.writeValueAsString(event);
      MapRecord<String, String, String> record = StreamRecords.newRecord()
          .ofMap(Map.of("type", "order_created", "payload", payload))
          .withStreamKey(ordersStreamKey);
      RecordId recordId = redis.opsForStream().add(record);
      if (recordId == null) {
        System.out.println("[stream] failed_to_add");
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return saved;
  }
}
