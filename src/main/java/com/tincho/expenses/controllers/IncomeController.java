package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.controllers.dto.IncomeResponse;
import com.tincho.expenses.model.Income;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.IncomeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
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
    List<IncomeResponse> list(@AuthenticationPrincipal TenantUserDetails principal) {
        List<Income> incomes = ADMIN.equals(principal.getRole())
                ? service.listAll()
                : service.listForTenant(principal.getId());
        return incomes.stream().map(IncomeResponse::from).toList();
    }

    @GetMapping("/{id}")
    IncomeResponse get(@PathVariable Long id,
                       @AuthenticationPrincipal TenantUserDetails principal) {
        Income income = ADMIN.equals(principal.getRole())
                ? service.get(id)
                : service.getForTenant(principal.getId(), id);
        return IncomeResponse.from(income);
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
