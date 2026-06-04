package com.onlinestore.productservice;

import com.onlinestore.contracts.CatalogSeed;
import com.onlinestore.contracts.Dtos.ProductDto;
import com.onlinestore.productservice.persistence.ProductEntity;
import com.onlinestore.productservice.persistence.ProductJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductCatalogService {

    private final ProductPersistenceMode mode;
    private final Map<UUID, ProductDto> memory;
    private final ProductJpaRepository jpaRepo;

    public ProductCatalogService(
            ProductPersistenceMode mode,
            @Autowired(required = false) ProductJpaRepository jpaRepo) {
        this.mode = mode;
        this.jpaRepo = jpaRepo;
        this.memory = new ConcurrentHashMap<>();
        if (mode == ProductPersistenceMode.MEMORY) {
            CatalogSeed.defaultProducts().forEach(p -> memory.put(p.productId(), p));
        }
    }

    public boolean isPostgres() {
        return mode == ProductPersistenceMode.POSTGRES;
    }

    public List<ProductDto> list(String search, boolean includeInactive) {
        if (mode == ProductPersistenceMode.POSTGRES && jpaRepo != null) {
            var q = (search == null || search.isBlank()) ? null : search;
            return jpaRepo.search(q, includeInactive).stream().map(this::toDto).toList();
        }
        var stream = memory.values().stream();
        if (!includeInactive) {
            stream = stream.filter(ProductDto::isActive);
        }
        if (search != null && !search.isBlank()) {
            var needle = search.toLowerCase(Locale.ROOT);
            stream = stream.filter(p -> p.name().toLowerCase(Locale.ROOT).contains(needle)
                    || p.description().toLowerCase(Locale.ROOT).contains(needle));
        }
        return stream.sorted(Comparator.comparing(ProductDto::name)).toList();
    }

    public Optional<ProductDto> get(UUID id, boolean includeInactive) {
        if (mode == ProductPersistenceMode.POSTGRES && jpaRepo != null) {
            return jpaRepo.findById(id)
                    .filter(e -> includeInactive || e.isActive())
                    .map(this::toDto);
        }
        var product = memory.get(id);
        if (product == null || (!includeInactive && !product.isActive())) {
            return Optional.empty();
        }
        return Optional.of(product);
    }

    public ProductDto create(ProductDto request) {
        var product = new ProductDto(
                UUID.randomUUID(),
                request.name(),
                request.description(),
                request.price(),
                true);
        if (mode == ProductPersistenceMode.POSTGRES && jpaRepo != null) {
            jpaRepo.save(toEntity(product));
            return product;
        }
        memory.put(product.productId(), product);
        return product;
    }

    public Optional<ProductDto> update(UUID id, ProductDto request) {
        if (mode == ProductPersistenceMode.POSTGRES && jpaRepo != null) {
            if (!jpaRepo.existsById(id)) {
                return Optional.empty();
            }
            var updated = new ProductDto(id, request.name(), request.description(), request.price(), request.isActive());
            jpaRepo.save(toEntity(updated));
            return Optional.of(updated);
        }
        if (!memory.containsKey(id)) {
            return Optional.empty();
        }
        var updated = new ProductDto(id, request.name(), request.description(), request.price(), request.isActive());
        memory.put(id, updated);
        return Optional.of(updated);
    }

    public boolean deactivate(UUID id) {
        if (mode == ProductPersistenceMode.POSTGRES && jpaRepo != null) {
            return jpaRepo.findById(id).map(entity -> {
                entity.setActive(false);
                entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                jpaRepo.save(entity);
                return true;
            }).orElse(false);
        }
        var existing = memory.get(id);
        if (existing == null) {
            return false;
        }
        memory.put(id, new ProductDto(id, existing.name(), existing.description(), existing.price(), false));
        return true;
    }

    private ProductDto toDto(ProductEntity entity) {
        return new ProductDto(
                entity.getProductId(),
                entity.getName(),
                entity.getDescription() != null ? entity.getDescription() : "",
                entity.getPrice(),
                entity.isActive());
    }

    private ProductEntity toEntity(ProductDto dto) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var entity = new ProductEntity();
        entity.setProductId(dto.productId());
        entity.setSku("sku-" + dto.productId().toString().replace("-", ""));
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setActive(dto.isActive());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
