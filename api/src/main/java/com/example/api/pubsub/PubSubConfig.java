package com.example.api.pubsub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class PubSubConfig {

  @Bean
  public ChannelTopic demoTopic() {
    return new ChannelTopic("demo.topic");
  }

  @Bean
  public MessageListenerAdapter messageListenerAdapter(PubSubListener listener) {
    return new MessageListenerAdapter(listener, "handleMessage");
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory redisConnectionFactory,
      MessageListenerAdapter messageListenerAdapter,
      ChannelTopic demoTopic
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);
    container.addMessageListener(messageListenerAdapter, demoTopic);
    return container;
  }
}
