package com.tincho.expenses.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 255) String newPassword
) {}
