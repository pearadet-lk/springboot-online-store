package com.onlinestore.userservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        String jwtIssuer,
        String jwtAudience,
        String jwtSigningKey,
        int accessTokenMinutes,
        int refreshTokenDays) {}
