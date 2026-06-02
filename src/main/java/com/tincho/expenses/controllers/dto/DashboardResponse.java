package com.tincho.expenses.controllers.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalExpenses,
        BigDecimal totalIncome,
        BigDecimal balance,
        int daysUntilEndOfMonth,
        BigDecimal dailyRemainingBudget,
        List<CategoryExpenseTotal> expensesByCategory
) {}
