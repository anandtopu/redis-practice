package com.example.api.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String userId) {
}
