package com.example.api.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotBlank String userId, @NotNull Long productId) {
}
