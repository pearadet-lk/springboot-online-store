package com.onlinestore.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ServiceWebAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter apiVersioningFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                var uri = request.getRequestURI();
                if (uri.startsWith("/api/v1/")) {
                    var rewritten = uri.substring("/api/v1".length());
                    var wrapped = new PathRewriteRequestWrapper(request, rewritten);
                    response.setHeader("api-supported-versions", "v1");
                    filterChain.doFilter(wrapped, response);
                } else {
                    response.setHeader("api-supported-versions", "v1");
                    filterChain.doFilter(request, response);
                }
            }
        };
    }

    @Bean
    public OncePerRequestFilter traceIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                var traceId = request.getHeader("X-Request-Id");
                if (traceId == null || traceId.isBlank()) {
                    traceId = java.util.UUID.randomUUID().toString().replace("-", "");
                }
                response.setHeader("X-Trace-Id", traceId);
                filterChain.doFilter(request, response);
            }
        };
    }
}
