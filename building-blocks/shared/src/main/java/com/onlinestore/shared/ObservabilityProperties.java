package com.onlinestore.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "observability")
public record ObservabilityProperties(String otlpEndpoint, String zipkinEndpoint, String elasticsearchUrl) {}
