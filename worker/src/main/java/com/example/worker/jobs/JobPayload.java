package com.example.worker.jobs;

public record JobPayload(String jobId, String kind, boolean fail) {
}
