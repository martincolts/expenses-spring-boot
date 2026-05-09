package com.tincho.expenses.controllers.dto;

import com.tincho.expenses.model.Tenant;
import java.time.OffsetDateTime;

public record TenantResponse(
        Long id,
        String name,
        String email,
        OffsetDateTime createdAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.id(), t.name(), t.email(), t.createdAt());
    }
}
