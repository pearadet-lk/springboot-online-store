package com.onlinestore.inventoryservice;

import com.onlinestore.contracts.CatalogSeed;
import com.onlinestore.contracts.Dtos.InventoryItemDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InventoryStore {

    private final Map<UUID, InventoryItemDto> items = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        CatalogSeed.defaultProducts()
                .forEach(p -> items.put(
                        p.productId(),
                        new InventoryItemDto(p.productId(), 100, 0, OffsetDateTime.now(ZoneOffset.UTC))));
    }

    public Map<UUID, InventoryItemDto> items() {
        return items;
    }
}
