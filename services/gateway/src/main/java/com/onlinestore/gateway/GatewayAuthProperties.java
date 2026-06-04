package com.onlinestore.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record GatewayAuthProperties(String jwtIssuer, String jwtAudience, String jwtSigningKey) {}
