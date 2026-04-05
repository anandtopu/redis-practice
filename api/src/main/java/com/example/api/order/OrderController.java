package com.example.api.order;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping("/orders")
  public ResponseEntity<?> create(
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request
  ) {
    String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? "no-key" : idempotencyKey;
    OrderEntity order = orderService.createOrderIdempotent(key, request);
    return ResponseEntity.ok(Map.of("orderId", order.getId()));
  }
}
