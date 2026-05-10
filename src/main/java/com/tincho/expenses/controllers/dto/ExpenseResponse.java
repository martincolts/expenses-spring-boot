package com.tincho.expenses.controllers.dto;

import com.tincho.expenses.model.Expense;
import com.tincho.expenses.model.ExpenseStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExpenseResponse(
        Long id,
        Long tenantId,
        Long categoryId,
        String description,
        BigDecimal amount,
        String currency,
        ExpenseStatus status,
        OffsetDateTime incurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.id(),
                e.tenant().getId(),
                e.category() == null ? null : e.category().getId(),
                e.description(),
                e.amount(),
                e.currency(),
                e.status(),
                e.incurredAt(),
                e.createdAt(),
                e.paidAt()
        );
    }
}
