package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.DashboardResponse;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
class DashboardController {

    private final DashboardService service;

    @GetMapping
    DashboardResponse get(
            @AuthenticationPrincipal TenantUserDetails principal,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return service.getForTenant(principal.getId(), year, month);
    }
}
