package com.tincho.expenses.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 255) String newPassword
) {}
