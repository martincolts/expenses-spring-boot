package com.tincho.expenses.model;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("category")
public record Category(
        @Id Long id,
        @Column("tenant_id") AggregateReference<Tenant, Long> tenant,
        String name,
        String iconId,
        String colorId,
        OffsetDateTime createdAt
) {}
