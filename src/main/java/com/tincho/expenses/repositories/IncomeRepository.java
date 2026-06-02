package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Income;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface IncomeRepository extends ListCrudRepository<Income, Long>, ListPagingAndSortingRepository<Income, Long> {

    @Query("SELECT * FROM income WHERE tenant_id = :tenantId ORDER BY created_at DESC, id DESC")
    List<Income> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM income WHERE tenant_id = :tenantId ORDER BY created_at DESC, id DESC LIMIT :size OFFSET :offset")
    List<Income> findByTenantIdPaged(@Param("tenantId") Long tenantId, @Param("size") int size, @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM income WHERE tenant_id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM income WHERE id = :id AND tenant_id = :tenantId")
    Optional<Income> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
