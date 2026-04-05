package com.example.api.leaderboard;

import jakarta.validation.constraints.NotBlank;

public record IncrementScoreRequest(@NotBlank String board, @NotBlank String member, double delta) {
}
