package com.example.api.pubsub;

import org.springframework.stereotype.Component;

@Component
public class PubSubListener {

  public void handleMessage(String message) {
    System.out.println("[pubsub] received=" + message);
  }
}
