package com.onlinestore.inventoryservice;

import com.onlinestore.contracts.Dtos.InventoryItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
public class InventoryController {

    private final InventoryStore store;

    public InventoryController(InventoryStore store) {
        this.store = store;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "inventory-service", "status", "ok");
    }

    @GetMapping("/inventory/{productId}")
    public InventoryItemDto get(@PathVariable UUID productId) {
        var item = store.items().get(productId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return item;
    }

    @PutMapping("/inventory/{productId}")
    public InventoryItemDto put(@PathVariable UUID productId, @RequestBody InventoryItemDto request) {
        store.items().put(productId, request);
        return request;
    }

    @PostMapping("/inventory/{productId}/reserve")
    public InventoryItemDto reserve(@PathVariable UUID productId, @RequestParam int quantity) {
        var result = InventoryOperations.tryReserve(store.items(), productId, quantity);
        if (!result.success()) {
            throw mapError(result);
        }
        return store.items().get(productId);
    }

    @PostMapping("/inventory/{productId}/release")
    public InventoryItemDto release(@PathVariable UUID productId, @RequestParam int quantity) {
        var result = InventoryOperations.tryRelease(store.items(), productId, quantity);
        if (!result.success()) {
            throw mapError(result);
        }
        return store.items().get(productId);
    }

    @PostMapping("/inventory/{productId}/commit")
    public InventoryItemDto commit(@PathVariable UUID productId, @RequestParam int quantity) {
        var result = InventoryOperations.tryCommit(store.items(), productId, quantity);
        if (!result.success()) {
            throw mapError(result);
        }
        return store.items().get(productId);
    }

    private static ResponseStatusException mapError(InventoryOperations.Result result) {
        if ("not_found".equals(result.errorCode())) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "Inventory operation failed.");
    }
}
