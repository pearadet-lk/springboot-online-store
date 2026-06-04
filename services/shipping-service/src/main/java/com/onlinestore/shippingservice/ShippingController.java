package com.onlinestore.shippingservice;

import com.onlinestore.contracts.Dtos.ShipmentDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ShippingController {

    private final Map<UUID, ShipmentDto> shipments = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "shipping-service", "status", "ok");
    }

    @PostMapping("/shipments/{orderId}")
    public ResponseEntity<ShipmentDto> create(@PathVariable UUID orderId) {
        if (shipments.containsKey(orderId)) {
            return ResponseEntity.ok(shipments.get(orderId));
        }
        var shipment = new ShipmentDto(
                UUID.randomUUID(),
                orderId,
                "DemoCarrier",
                "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Created",
                OffsetDateTime.now(ZoneOffset.UTC));
        shipments.put(orderId, shipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @GetMapping("/shipments/{orderId}")
    public ShipmentDto get(@PathVariable UUID orderId) {
        var shipment = shipments.get(orderId);
        if (shipment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return shipment;
    }
}
