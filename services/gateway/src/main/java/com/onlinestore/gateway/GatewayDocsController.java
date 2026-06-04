package com.onlinestore.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class GatewayDocsController {

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("service", "gateway", "status", "ok"));
    }

    @GetMapping("/api/docs")
    public Mono<Map<String, Object>> docs() {
        return Mono.just(Map.of(
                "gateway", "/",
                "downstream",
                List.of(
                        Map.of("name", "user-service", "url", "/users"),
                        Map.of("name", "product-service", "url", "/products"),
                        Map.of("name", "order-service", "url", "/orders"))));
    }
}
