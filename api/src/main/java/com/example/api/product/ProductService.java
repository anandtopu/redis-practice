package com.example.api.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private static final Duration PRODUCT_TTL = Duration.ofSeconds(60);

  private final ProductRepository productRepository;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  private final Counter cacheHit;
  private final Counter cacheMiss;
  private final Counter cacheParseError;

  public ProductService(
      ProductRepository productRepository,
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry
  ) {
    this.productRepository = productRepository;
    this.redis = redis;
    this.objectMapper = objectMapper;

    MeterRegistry registry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.cacheHit = registry.counter("demo.cache.product", "result", "hit");
    this.cacheMiss = registry.counter("demo.cache.product", "result", "miss");
    this.cacheParseError = registry.counter("demo.cache.product", "result", "parse_error");
  }

  public ProductDto getProduct(Long id) {
    String key = productKey(id);

    String cached = redis.opsForValue().get(key);
    if (cached != null) {
      try {
        Product p = objectMapper.readValue(cached, Product.class);
        cacheHit.increment();
        return new ProductDto(p.getId(), p.getName(), p.getPrice(), "redis");
      } catch (JsonProcessingException e) {
        cacheParseError.increment();
        redis.delete(key);
      }
    }

    cacheMiss.increment();

    Optional<Product> db = productRepository.findById(id);
    if (db.isEmpty()) {
      return null;
    }

    Product p = db.get();
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(p), PRODUCT_TTL);
    } catch (JsonProcessingException ignored) {
    }

    return new ProductDto(p.getId(), p.getName(), p.getPrice(), "postgres");
  }

  @Transactional
  public ProductDto updatePrice(Long id, UpdatePriceRequest request) {
    Product p = productRepository.findById(id).orElseThrow();
    p.setPrice(request.price());

    redis.delete(productKey(id));

    return new ProductDto(p.getId(), p.getName(), p.getPrice(), "postgres");
  }

  private static String productKey(Long id) {
    return "product:" + id;
  }
}
