package com.onlinestore.productservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE (:includeInactive = true OR p.active = true)
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.name
            """)
    List<ProductEntity> search(@Param("q") String q, @Param("includeInactive") boolean includeInactive);
}
