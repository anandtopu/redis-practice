package com.example.api.semaphore;

public record SemaphoreLease(String name, int slot, String token, long ttlMs) {
}
