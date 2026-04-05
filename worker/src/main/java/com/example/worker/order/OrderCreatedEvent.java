package com.example.worker.order;

import java.time.Instant;

public record OrderCreatedEvent(Long orderId, String userId, Long productId, Instant createdAt) {
}
