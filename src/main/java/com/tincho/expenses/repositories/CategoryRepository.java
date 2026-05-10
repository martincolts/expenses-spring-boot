package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends ListCrudRepository<Category, Long> {

    @Query("SELECT * FROM category WHERE tenant_id = :tenantId ORDER BY id")
    List<Category> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM category WHERE id = :id AND tenant_id = :tenantId")
    Optional<Category> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT EXISTS(SELECT 1 FROM category WHERE tenant_id = :tenantId AND name = :name)")
    boolean existsByTenantIdAndName(@Param("tenantId") Long tenantId, @Param("name") String name);
}
