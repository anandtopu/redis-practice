package com.example.api.lock;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LockController {

  private final LockService lockService;

  public LockController(LockService lockService) {
    this.lockService = lockService;
  }

  @PostMapping("/lock/demo")
  public ResponseEntity<?> demo() throws InterruptedException {
    String key = "lock:demo";

    String token = lockService.tryAcquire(key, Duration.ofSeconds(10));
    if (token == null) {
      return ResponseEntity.status(409).body(Map.of("error", "lock_busy"));
    }

    try {
      Thread.sleep(500);
      return ResponseEntity.ok(Map.of("acquired", true, "token", token));
    } finally {
      lockService.release(key, token);
    }
  }
}
