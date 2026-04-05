package com.example.api.order;

import java.time.Instant;

public record OrderCreatedEvent(Long orderId, String userId, Long productId, Instant createdAt) {
}
