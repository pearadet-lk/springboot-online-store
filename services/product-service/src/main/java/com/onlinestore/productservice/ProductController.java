package com.onlinestore.productservice;

import com.onlinestore.contracts.Dtos.ProductDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProductController {

    private final ProductCatalogService catalog;

    public ProductController(ProductCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "product-service",
                "status", "ok",
                "persistence", catalog.isPostgres() ? "postgres" : "memory");
    }

    @GetMapping("/products")
    public List<ProductDto> list(@RequestParam(required = false) String search) {
        return catalog.list(search, false);
    }

    @GetMapping("/products/{id}")
    public ProductDto get(@PathVariable UUID id) {
        return catalog.get(id, false).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/admin/products")
    public List<ProductDto> adminList() {
        return catalog.list(null, true);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto request) {
        var product = catalog.create(request);
        return ResponseEntity.created(java.net.URI.create("/products/" + product.productId()))
                .body(product);
    }

    @PutMapping("/products/{id}")
    public ProductDto update(@PathVariable UUID id, @RequestBody ProductDto request) {
        return catalog.update(id, request).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        if (!catalog.deactivate(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
