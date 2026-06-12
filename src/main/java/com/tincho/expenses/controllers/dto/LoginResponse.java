package com.tincho.expenses.controllers.dto;

public record LoginResponse(
        String token,
        String refreshToken,
        Long id,
        String name,
        String email,
        String role
) {}
