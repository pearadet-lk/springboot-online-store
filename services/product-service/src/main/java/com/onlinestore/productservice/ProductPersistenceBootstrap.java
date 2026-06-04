package com.onlinestore.productservice;

import com.onlinestore.contracts.CatalogSeed;
import com.onlinestore.productservice.persistence.ProductEntity;
import com.onlinestore.productservice.persistence.ProductJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Configuration
public class ProductPersistenceBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ProductPersistenceBootstrap.class);

    @Bean
    ProductPersistenceMode productPersistenceMode(Environment environment) {
        if (environment.acceptsProfiles("postgres")) {
            log.info("Product catalog using PostgreSQL (postgres profile).");
            return ProductPersistenceMode.POSTGRES;
        }
        log.warn("Product catalog using in-memory fallback.");
        return ProductPersistenceMode.MEMORY;
    }

    @Bean
    ApplicationRunner catalogSeeder(
            ProductPersistenceMode mode,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ProductJpaRepository jpaRepo) {
        return args -> {
            if (mode != ProductPersistenceMode.POSTGRES || jpaRepo == null) {
                return;
            }
            for (var p : CatalogSeed.defaultProducts()) {
                if (jpaRepo.existsById(p.productId())) {
                    continue;
                }
                var entity = new ProductEntity();
                entity.setProductId(p.productId());
                entity.setSku("sku-" + p.productId().toString().replace("-", ""));
                entity.setName(p.name());
                entity.setDescription(p.description());
                entity.setPrice(p.price());
                entity.setActive(p.isActive());
                var now = OffsetDateTime.now(ZoneOffset.UTC);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                jpaRepo.save(entity);
            }
        };
    }
}
