package com.example.api.retry;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetryController {

  private final RetryQueueService retryQueueService;

  public RetryController(RetryQueueService retryQueueService) {
    this.retryQueueService = retryQueueService;
  }

  @PostMapping("/retry/enqueue")
  public ResponseEntity<?> enqueue(@Valid @RequestBody EnqueueRetryRequest request) {
    retryQueueService.enqueue(request);
    return ResponseEntity.ok(Map.of("enqueued", true));
  }
}
