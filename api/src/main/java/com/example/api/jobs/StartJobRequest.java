package com.example.api.jobs;

import jakarta.validation.constraints.NotBlank;

public record StartJobRequest(@NotBlank String kind, boolean fail) {
}
