package com.example.api.retry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record EnqueueRetryRequest(
    @NotBlank String taskId,
    @PositiveOrZero int failTimes,
    @PositiveOrZero long initialDelayMs
) {
}
