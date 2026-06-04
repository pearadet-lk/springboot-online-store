package com.onlinestore.inventoryservice;

import com.onlinestore.contracts.Dtos.InventoryItemDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

public final class InventoryOperations {

    private InventoryOperations() {}

    public static Result tryCheck(Map<UUID, InventoryItemDto> items, UUID productId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("invalid");
        }
        var item = items.get(productId);
        if (item == null) {
            return Result.fail("not_found");
        }
        return item.availableQty() >= quantity ? Result.ok() : Result.fail("invalid");
    }

    public static Result tryReserve(Map<UUID, InventoryItemDto> items, UUID productId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("invalid");
        }
        var item = items.get(productId);
        if (item == null) {
            return Result.fail("not_found");
        }
        if (item.availableQty() < quantity) {
            return Result.fail("invalid");
        }
        items.put(
                productId,
                new InventoryItemDto(
                        productId,
                        item.availableQty() - quantity,
                        item.reservedQty() + quantity,
                        OffsetDateTime.now(ZoneOffset.UTC)));
        return Result.ok();
    }

    public static Result tryRelease(Map<UUID, InventoryItemDto> items, UUID productId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("invalid");
        }
        var item = items.get(productId);
        if (item == null) {
            return Result.fail("not_found");
        }
        if (item.reservedQty() < quantity) {
            return Result.fail("invalid");
        }
        items.put(
                productId,
                new InventoryItemDto(
                        productId,
                        item.availableQty() + quantity,
                        item.reservedQty() - quantity,
                        OffsetDateTime.now(ZoneOffset.UTC)));
        return Result.ok();
    }

    public static Result tryCommit(Map<UUID, InventoryItemDto> items, UUID productId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("invalid");
        }
        var item = items.get(productId);
        if (item == null) {
            return Result.fail("not_found");
        }
        if (item.reservedQty() < quantity) {
            return Result.fail("invalid");
        }
        items.put(
                productId,
                new InventoryItemDto(
                        productId,
                        item.availableQty(),
                        item.reservedQty() - quantity,
                        OffsetDateTime.now(ZoneOffset.UTC)));
        return Result.ok();
    }

    public record Result(boolean success, String errorCode) {
        static Result ok() {
            return new Result(true, null);
        }

        static Result fail(String code) {
            return new Result(false, code);
        }
    }
}
