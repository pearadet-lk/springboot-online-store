package com.onlinestore.cartservice;

import com.onlinestore.contracts.Dtos.CartDto;
import com.onlinestore.contracts.Dtos.UpsertCartRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CartController {

    private final Map<UUID, CartDto> carts = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "cart-service",
                "status", "ok",
                "message", "Cart storage is in-memory; Redis is recommended for production.");
    }

    @GetMapping("/carts/{userId}")
    public CartDto get(@PathVariable UUID userId) {
        return carts.getOrDefault(
                userId,
                new CartDto(UUID.randomUUID(), userId, java.util.List.of(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @PutMapping("/carts/{userId}")
    public CartDto upsert(@PathVariable UUID userId, @RequestBody UpsertCartRequest request) {
        var cart = new CartDto(UUID.randomUUID(), userId, request.items(), OffsetDateTime.now(ZoneOffset.UTC));
        carts.put(userId, cart);
        return cart;
    }

    @DeleteMapping("/carts/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        carts.remove(userId);
    }
}
