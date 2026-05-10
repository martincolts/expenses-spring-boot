package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.CreateExpenseRequest;
import com.tincho.expenses.controllers.dto.ExpenseResponse;
import com.tincho.expenses.controllers.dto.UpdateExpenseRequest;
import com.tincho.expenses.model.Expense;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.ExpenseService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
class ExpenseController {

    private static final String ADMIN = "ADMIN";

    private final ExpenseService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest req,
                           @AuthenticationPrincipal TenantUserDetails principal) {
        return ExpenseResponse.from(service.create(principal.getId(), req));
    }

    @GetMapping
    List<ExpenseResponse> list(@AuthenticationPrincipal TenantUserDetails principal) {
        List<Expense> expenses = ADMIN.equals(principal.getRole())
                ? service.listAll()
                : service.listForTenant(principal.getId());
        return expenses.stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    ExpenseResponse get(@PathVariable Long id,
                        @AuthenticationPrincipal TenantUserDetails principal) {
        Expense expense = ADMIN.equals(principal.getRole())
                ? service.get(id)
                : service.getForTenant(principal.getId(), id);
        return ExpenseResponse.from(expense);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT')")
    ExpenseResponse update(@PathVariable Long id,
                           @Valid @RequestBody UpdateExpenseRequest req,
                           @AuthenticationPrincipal TenantUserDetails principal) {
        return ExpenseResponse.from(service.update(principal.getId(), id, req));
    }

    @PostMapping("/{id}/pay")
    ExpenseResponse pay(@PathVariable Long id,
                        @AuthenticationPrincipal TenantUserDetails principal) {
        Expense expense = ADMIN.equals(principal.getRole())
                ? service.markPaid(id)
                : service.markPaidForTenant(principal.getId(), id);
        return ExpenseResponse.from(expense);
    }

    @PostMapping("/{id}/unpay")
    ExpenseResponse unpay(@PathVariable Long id,
                          @AuthenticationPrincipal TenantUserDetails principal) {
        Expense expense = ADMIN.equals(principal.getRole())
                ? service.markUnpaid(id)
                : service.markUnpaidForTenant(principal.getId(), id);
        return ExpenseResponse.from(expense);
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
