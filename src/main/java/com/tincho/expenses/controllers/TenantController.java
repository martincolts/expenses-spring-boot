package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.TenantResponse;
import com.tincho.expenses.controllers.dto.UpdateTenantRequest;
import com.tincho.expenses.services.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
class TenantController {

    private final TenantService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<TenantResponse> list() {
        return service.list().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TENANT') and #id == authentication.principal.id)")
    TenantResponse get(@PathVariable Long id) {
        return TenantResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TENANT') and #id == authentication.principal.id)")
    TenantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest req) {
        return TenantResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
