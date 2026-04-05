package com.example.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenBucketController {

  private final TokenBucketRateLimiter limiter;

  public TokenBucketController(TokenBucketRateLimiter limiter) {
    this.limiter = limiter;
  }

  @GetMapping("/limited/token")
  public ResponseEntity<?> tokenBucket(
      HttpServletRequest request,
      @RequestParam(name = "dimension", defaultValue = "ip") String dimension,
      @RequestParam(name = "userId", required = false) String userId,
      @RequestParam(name = "tenantId", required = false) String tenantId
  ) {
    String subject;
    if ("user".equalsIgnoreCase(dimension)) {
      subject = (userId == null || userId.isBlank()) ? "anon" : userId;
    } else if ("tenant".equalsIgnoreCase(dimension)) {
      subject = (tenantId == null || tenantId.isBlank()) ? "unknown" : tenantId;
    } else {
      subject = request.getRemoteAddr();
    }

    String key = "rate:tb:" + dimension.toLowerCase() + ":" + subject;

    boolean allowed = limiter.allow(key, 10, 1.0, 1);
    if (!allowed) {
      return ResponseEntity.status(429).body(Map.of("error", "rate_limited", "dimension", dimension, "subject", subject));
    }

    return ResponseEntity.ok(Map.of("ok", true, "dimension", dimension, "subject", subject));
  }
}
