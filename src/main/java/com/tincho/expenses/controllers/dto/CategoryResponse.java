package com.tincho.expenses.controllers.dto;

import com.tincho.expenses.model.Category;
import java.time.OffsetDateTime;

public record CategoryResponse(
        Long id,
        Long tenantId,
        String name,
        OffsetDateTime createdAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.tenant().getId(), c.name(), c.createdAt());
    }
}
