package com.example.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitedController {

  private final RateLimitService rateLimitService;

  public RateLimitedController(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @GetMapping("/limited")
  public ResponseEntity<?> limited(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    String key = "rate:fixed:" + ip;
    boolean allowed = rateLimitService.allowFixedWindow(key, 5, Duration.ofSeconds(30));

    if (!allowed) {
      return ResponseEntity.status(429).body(Map.of("error", "rate_limited"));
    }

    return ResponseEntity.ok(Map.of("ok", true));
  }
}
