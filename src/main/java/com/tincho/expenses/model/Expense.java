package com.tincho.expenses.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("expense")
public record Expense(
        @Id Long id,
        @Column("tenant_id") AggregateReference<Tenant, Long> tenant,
        @Column("category_id") AggregateReference<Category, Long> category,
        String description,
        BigDecimal amount,
        String currency,
        ExpenseStatus status,
        OffsetDateTime incurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {}
