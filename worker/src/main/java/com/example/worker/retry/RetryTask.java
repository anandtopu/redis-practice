package com.example.worker.retry;

public record RetryTask(String taskId, int attempt, int failTimes) {
}
