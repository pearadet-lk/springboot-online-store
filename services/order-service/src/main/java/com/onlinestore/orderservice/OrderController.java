package com.onlinestore.orderservice;

import com.onlinestore.contracts.Dtos.CreateOrderRequest;
import com.onlinestore.contracts.Dtos.OrderDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OrderController {

    private final Map<UUID, OrderDto> orders = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "order-service", "status", "ok");
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> healthV1() {
        return health();
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> create(@RequestBody CreateOrderRequest request) {
        var total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var order = new OrderDto(
                UUID.randomUUID(),
                request.userId(),
                total,
                request.currency().toUpperCase(),
                "Pending",
                OffsetDateTime.now(ZoneOffset.UTC));
        orders.put(order.orderId(), order);
        return ResponseEntity.created(java.net.URI.create("/orders/" + order.orderId())).body(order);
    }

    @GetMapping("/orders/{orderId}")
    public OrderDto get(@PathVariable UUID orderId) {
        var order = orders.get(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return order;
    }

    @PostMapping("/orders/{orderId}/complete")
    public OrderDto complete(@PathVariable UUID orderId) {
        return updateStatus(orderId, "Completed");
    }

    @PostMapping("/orders/{orderId}/fail")
    public OrderDto fail(@PathVariable UUID orderId) {
        return updateStatus(orderId, "Failed");
    }

    private OrderDto updateStatus(UUID orderId, String status) {
        var existing = orders.get(orderId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var updated = new OrderDto(
                existing.orderId(),
                existing.userId(),
                existing.totalAmount(),
                existing.currency(),
                status,
                existing.createdAt());
        orders.put(orderId, updated);
        return updated;
    }
}
