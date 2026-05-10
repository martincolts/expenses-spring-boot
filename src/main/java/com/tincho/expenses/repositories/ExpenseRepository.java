package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Expense;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends ListCrudRepository<Expense, Long> {

    @Query("SELECT * FROM expense WHERE tenant_id = :tenantId ORDER BY incurred_at DESC, id DESC")
    List<Expense> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM expense WHERE id = :id AND tenant_id = :tenantId")
    Optional<Expense> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
