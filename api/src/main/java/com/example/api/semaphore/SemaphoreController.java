package com.example.api.semaphore;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SemaphoreController {

  private final SemaphoreService semaphoreService;

  public SemaphoreController(SemaphoreService semaphoreService) {
    this.semaphoreService = semaphoreService;
  }

  @PostMapping("/semaphore/acquire")
  public ResponseEntity<?> acquire(
      @RequestParam String name,
      @RequestParam @Min(1) @Max(50) int permits,
      @RequestParam(defaultValue = "10000") long ttlMs
  ) {
    SemaphoreLease lease = semaphoreService.tryAcquire(name, permits, Duration.ofMillis(ttlMs));
    if (lease == null) {
      return ResponseEntity.status(409).body(Map.of("error", "no_permits"));
    }
    return ResponseEntity.ok(lease);
  }

  @PostMapping("/semaphore/release")
  public ResponseEntity<?> release(
      @RequestParam String name,
      @RequestParam int slot,
      @RequestParam String token
  ) {
    boolean ok = semaphoreService.release(new SemaphoreLease(name, slot, token, 0));
    return ResponseEntity.ok(Map.of("released", ok));
  }
}
