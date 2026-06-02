package com.tincho.expenses.repositories;

import com.tincho.expenses.model.Expense;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends ListCrudRepository<Expense, Long>, ListPagingAndSortingRepository<Expense, Long> {

    @Query("SELECT * FROM expense WHERE tenant_id = :tenantId ORDER BY incurred_at DESC, id DESC")
    List<Expense> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM expense WHERE tenant_id = :tenantId ORDER BY incurred_at DESC, id DESC LIMIT :size OFFSET :offset")
    List<Expense> findByTenantIdPaged(@Param("tenantId") Long tenantId, @Param("size") int size, @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM expense WHERE tenant_id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM expense WHERE id = :id AND tenant_id = :tenantId")
    Optional<Expense> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT * FROM expense
            WHERE tenant_id = :tenantId
              AND (CAST(:categoryId AS BIGINT)      IS NULL OR category_id   = :categoryId)
              AND (CAST(:status    AS TEXT)         IS NULL OR status::text  = :status)
              AND (CAST(:dateFrom  AS TIMESTAMPTZ)  IS NULL OR incurred_at  >= :dateFrom)
              AND (CAST(:dateTo    AS TIMESTAMPTZ)  IS NULL OR incurred_at  <= :dateTo)
            ORDER BY incurred_at DESC, id DESC
            LIMIT :size OFFSET :offset
            """)
    List<Expense> findByTenantIdFilteredPaged(
            @Param("tenantId")   Long tenantId,
            @Param("categoryId") Long categoryId,
            @Param("status")     String status,
            @Param("dateFrom")   OffsetDateTime dateFrom,
            @Param("dateTo")     OffsetDateTime dateTo,
            @Param("size")       int size,
            @Param("offset")     long offset);

    @Query("""
            SELECT COUNT(*) FROM expense
            WHERE tenant_id = :tenantId
              AND (CAST(:categoryId AS BIGINT)      IS NULL OR category_id   = :categoryId)
              AND (CAST(:status    AS TEXT)         IS NULL OR status::text  = :status)
              AND (CAST(:dateFrom  AS TIMESTAMPTZ)  IS NULL OR incurred_at  >= :dateFrom)
              AND (CAST(:dateTo    AS TIMESTAMPTZ)  IS NULL OR incurred_at  <= :dateTo)
            """)
    long countByTenantIdFiltered(
            @Param("tenantId")   Long tenantId,
            @Param("categoryId") Long categoryId,
            @Param("status")     String status,
            @Param("dateFrom")   OffsetDateTime dateFrom,
            @Param("dateTo")     OffsetDateTime dateTo);

    @Query("""
            SELECT * FROM expense
            WHERE (CAST(:categoryId AS BIGINT)      IS NULL OR category_id   = :categoryId)
              AND (CAST(:status    AS TEXT)         IS NULL OR status::text  = :status)
              AND (CAST(:dateFrom  AS TIMESTAMPTZ)  IS NULL OR incurred_at  >= :dateFrom)
              AND (CAST(:dateTo    AS TIMESTAMPTZ)  IS NULL OR incurred_at  <= :dateTo)
            ORDER BY incurred_at DESC, id DESC
            LIMIT :size OFFSET :offset
            """)
    List<Expense> findAllFilteredPaged(
            @Param("categoryId") Long categoryId,
            @Param("status")     String status,
            @Param("dateFrom")   OffsetDateTime dateFrom,
            @Param("dateTo")     OffsetDateTime dateTo,
            @Param("size")       int size,
            @Param("offset")     long offset);

    @Query("""
            SELECT COUNT(*) FROM expense
            WHERE (CAST(:categoryId AS BIGINT)      IS NULL OR category_id   = :categoryId)
              AND (CAST(:status    AS TEXT)         IS NULL OR status::text  = :status)
              AND (CAST(:dateFrom  AS TIMESTAMPTZ)  IS NULL OR incurred_at  >= :dateFrom)
              AND (CAST(:dateTo    AS TIMESTAMPTZ)  IS NULL OR incurred_at  <= :dateTo)
            """)
    long countAllFiltered(
            @Param("categoryId") Long categoryId,
            @Param("status")     String status,
            @Param("dateFrom")   OffsetDateTime dateFrom,
            @Param("dateTo")     OffsetDateTime dateTo);
}
