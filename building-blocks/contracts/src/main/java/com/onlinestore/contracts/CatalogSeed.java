package com.onlinestore.contracts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.onlinestore.contracts.Dtos.ProductDto;

public final class CatalogSeed {

    public static final int DEFAULT_PRODUCT_COUNT = 100;

    private static final List<ProductDto> PRODUCTS = buildProducts();

    private CatalogSeed() {}

    public static List<ProductDto> defaultProducts() {
        return PRODUCTS;
    }

    private static List<ProductDto> buildProducts() {
        var list = new ArrayList<ProductDto>(DEFAULT_PRODUCT_COUNT);

        list.add(new ProductDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Starter Keyboard",
                "Entry-level keyboard",
                new BigDecimal("39.99"),
                true));

        list.add(new ProductDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Gaming Mouse",
                "RGB gaming mouse",
                new BigDecimal("59.99"),
                true));

        String[] departments = {
            "Electronics", "Home", "Sports", "Office", "Garden", "Books", "Music", "Toys"
        };
        String[] kinds = {
            "Adapter", "Cable", "Stand", "Kit", "Pack", "Set", "Mat", "Lamp", "Holder", "Case"
        };

        for (int i = 3; i <= DEFAULT_PRODUCT_COUNT; i++) {
            var id = UUID.fromString(String.format("00000000-0000-4000-8000-%012x", i));
            var dept = departments[(i - 3) % departments.length];
            var kind = kinds[(i * 7) % kinds.length];
            var name = dept + " " + kind + " " + String.format("%03d", i);
            var description = "Seeded demo product " + i + " for catalog browsing and checkout testing.";
            var price = BigDecimal.valueOf(4.99 + (i * 37 % 450) * 0.15).setScale(2, RoundingMode.HALF_UP);
            list.add(new ProductDto(id, name, description, price, true));
        }

        return List.copyOf(list);
    }
}
