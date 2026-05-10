package com.tincho.expenses.model;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("tenant")
public record Tenant(
        @Id Long id,
        String name,
        String email,
        String passwordHash,
        String role,
        OffsetDateTime createdAt
) {}
