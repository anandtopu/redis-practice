package com.example.api.pubsub;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PubSubController {

  private final StringRedisTemplate redis;
  private final ChannelTopic demoTopic;

  public PubSubController(StringRedisTemplate redis, ChannelTopic demoTopic) {
    this.redis = redis;
    this.demoTopic = demoTopic;
  }

  @PostMapping("/pubsub/publish")
  public ResponseEntity<?> publish(@Valid @RequestBody PublishRequest request) {
    Long receivers = redis.convertAndSend(demoTopic.getTopic(), request.message());
    return ResponseEntity.ok(Map.of("published", true, "receivers", receivers));
  }
}
