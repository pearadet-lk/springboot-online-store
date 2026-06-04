package com.onlinestore.emailservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "messaging.kafka")
public record KafkaMessagingProperties(String bootstrapServers, String topic, String groupId) {}
