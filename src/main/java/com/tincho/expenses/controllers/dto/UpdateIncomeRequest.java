package com.tincho.expenses.controllers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateIncomeRequest(
        @NotNull @Positive BigDecimal amount
) {}
