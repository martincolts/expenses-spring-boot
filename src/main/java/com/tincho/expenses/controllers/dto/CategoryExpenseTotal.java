package com.tincho.expenses.controllers.dto;

import java.math.BigDecimal;

public record CategoryExpenseTotal(Long categoryId, String categoryName, BigDecimal total) {}
