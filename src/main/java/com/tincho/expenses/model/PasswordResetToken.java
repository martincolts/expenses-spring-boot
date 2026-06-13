package com.tincho.expenses.model;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("password_reset_token")
public record PasswordResetToken(
        @Id Long id,
        @Column("tenant_id") AggregateReference<Tenant, Long> tenant,
        String token,
        String resetUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt
) {}
