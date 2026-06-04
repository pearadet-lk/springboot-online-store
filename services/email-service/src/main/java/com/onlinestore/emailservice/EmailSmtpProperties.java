package com.onlinestore.emailservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email")
public record EmailSmtpProperties(
        String fromAddress, String smtpHost, int smtpPort, String smtpUser, String smtpPassword, boolean enableSsl) {}
