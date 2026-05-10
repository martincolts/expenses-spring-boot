package com.tincho.expenses.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("income")
public record Income(
        @Id Long id,
        @Column("tenant_id") AggregateReference<Tenant, Long> tenant,
        BigDecimal amount,
        OffsetDateTime createdAt
) {}
