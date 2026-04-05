package com.example.api.pubsub;

import jakarta.validation.constraints.NotBlank;

public record PublishRequest(@NotBlank String message) {
}
