package com.tincho.expenses.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.controllers.dto.IncomeResponse;
import com.tincho.expenses.controllers.dto.PageResponse;
import com.tincho.expenses.controllers.dto.UpdateIncomeRequest;
import com.tincho.expenses.model.Income;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.IncomeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Validated
class IncomeController {

    private static final String ADMIN = "ADMIN";

    private final IncomeService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    IncomeResponse create(@Valid @RequestBody CreateIncomeRequest req,
                          @AuthenticationPrincipal TenantUserDetails principal) {
        return IncomeResponse.from(service.create(principal.getId(), req));
    }

    @GetMapping
    PageResponse<IncomeResponse> list(
            @AuthenticationPrincipal TenantUserDetails principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Income> incomes = ADMIN.equals(principal.getRole())
                ? service.listAll(PageRequest.of(page, size))
                : service.listForTenant(principal.getId(), PageRequest.of(page, size));
        return PageResponse.from(incomes.map(IncomeResponse::from));
    }

    @GetMapping("/{id}")
    IncomeResponse get(@PathVariable Long id,
                       @AuthenticationPrincipal TenantUserDetails principal) {
        Income income = ADMIN.equals(principal.getRole())
                ? service.get(id)
                : service.getForTenant(principal.getId(), id);
        return IncomeResponse.from(income);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT')")
    IncomeResponse update(@PathVariable Long id,
                          @Valid @RequestBody UpdateIncomeRequest req,
                          @AuthenticationPrincipal TenantUserDetails principal) {
        return IncomeResponse.from(service.update(principal.getId(), id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id,
                @AuthenticationPrincipal TenantUserDetails principal) {
        if (ADMIN.equals(principal.getRole())) {
            service.delete(id);
        } else {
            service.deleteForTenant(principal.getId(), id);
        }
    }
}
