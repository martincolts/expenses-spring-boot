package com.tincho.expenses.controllers.dto;

import com.tincho.expenses.model.Income;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IncomeResponse(
        Long id,
        Long tenantId,
        BigDecimal amount,
        OffsetDateTime createdAt
) {
    public static IncomeResponse from(Income i) {
        return new IncomeResponse(i.id(), i.tenant().getId(), i.amount(), i.createdAt());
    }
}
