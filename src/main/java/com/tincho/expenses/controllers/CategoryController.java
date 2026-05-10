package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.CategoryResponse;
import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
class CategoryController {

    private static final String ADMIN = "ADMIN";

    private final CategoryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    CategoryResponse create(@Valid @RequestBody CreateCategoryRequest req,
                            @AuthenticationPrincipal TenantUserDetails principal) {
        return CategoryResponse.from(service.create(principal.getId(), req));
    }

    @GetMapping
    List<CategoryResponse> list(@AuthenticationPrincipal TenantUserDetails principal) {
        List<Category> categories = ADMIN.equals(principal.getRole())
                ? service.listAll()
                : service.listForTenant(principal.getId());
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    CategoryResponse get(@PathVariable Long id,
                         @AuthenticationPrincipal TenantUserDetails principal) {
        Category category = ADMIN.equals(principal.getRole())
                ? service.get(id)
                : service.getForTenant(principal.getId(), id);
        return CategoryResponse.from(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT')")
    CategoryResponse update(@PathVariable Long id,
                            @Valid @RequestBody UpdateCategoryRequest req,
                            @AuthenticationPrincipal TenantUserDetails principal) {
        return CategoryResponse.from(service.update(principal.getId(), id, req));
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
