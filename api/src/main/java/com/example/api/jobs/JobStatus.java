package com.example.api.jobs;

import java.time.Instant;

public record JobStatus(String jobId, String status, int progress, Instant updatedAt, String result, String error) {
}
