package com.tincho.expenses.controllers.dto;

import java.time.OffsetDateTime;

import com.tincho.expenses.model.Tenant;

public record TenantResponse(
        Long id,
        String name,
        String email,
        String role,
        OffsetDateTime createdAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.id(), t.name(), t.email(), t.role(), t.createdAt());
    }
}
