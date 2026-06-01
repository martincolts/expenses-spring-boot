package com.tincho.expenses.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CategoryResponse;
import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.CategoryService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    CategoryService service;

    @InjectMocks
    CategoryController controller;

    @Test
    void create_delegatesToService_andReturnsResponse() {
        TenantUserDetails principal = tenantPrincipal(7L);
        CreateCategoryRequest req = new CreateCategoryRequest("Food", null, null);
        Category saved = category(1L, 7L, "Food");
        given(service.create(7L, req)).willReturn(saved);

        CategoryResponse response = controller.create(req, principal);

        verify(service).create(7L, req);
        assertThat(response).isEqualTo(CategoryResponse.from(saved));
    }

    @Test
    void list_callsListForTenant_whenTenant() {
        TenantUserDetails principal = tenantPrincipal(7L);
        Category a = category(1L, 7L, "A");
        Category b = category(2L, 7L, "B");
        given(service.listForTenant(7L)).willReturn(List.of(a, b));

        List<CategoryResponse> response = controller.list(principal);

        verify(service).listForTenant(7L);
        verify(service, never()).listAll();
        assertThat(response).containsExactly(CategoryResponse.from(a), CategoryResponse.from(b));
    }

    @Test
    void list_callsListAll_whenAdmin() {
        TenantUserDetails principal = adminPrincipal(99L);
        Category a = category(1L, 7L, "A");
        given(service.listAll()).willReturn(List.of(a));

        controller.list(principal);

        verify(service).listAll();
        verify(service, never()).listForTenant(anyLong());
    }

    @Test
    void list_returnsEmpty_whenServiceReturnsEmpty() {
        given(service.listForTenant(7L)).willReturn(List.of());

        assertThat(controller.list(tenantPrincipal(7L))).isEmpty();
    }

    @Test
    void get_callsGetForTenant_whenTenant() {
        TenantUserDetails principal = tenantPrincipal(7L);
        Category c = category(1L, 7L, "Food");
        given(service.getForTenant(7L, 1L)).willReturn(c);

        CategoryResponse response = controller.get(1L, principal);

        verify(service).getForTenant(7L, 1L);
        verify(service, never()).get(anyLong());
        assertThat(response).isEqualTo(CategoryResponse.from(c));
    }

    @Test
    void get_callsGet_whenAdmin() {
        TenantUserDetails principal = adminPrincipal(99L);
        Category c = category(1L, 7L, "Food");
        given(service.get(1L)).willReturn(c);

        controller.get(1L, principal);

        verify(service).get(1L);
        verify(service, never()).getForTenant(anyLong(), anyLong());
    }

    @Test
    void update_delegatesToService_andReturnsResponse() {
        TenantUserDetails principal = tenantPrincipal(7L);
        UpdateCategoryRequest req = new UpdateCategoryRequest("New", null, null);
        Category updated = category(1L, 7L, "New");
        given(service.update(7L, 1L, req)).willReturn(updated);

        CategoryResponse response = controller.update(1L, req, principal);

        verify(service).update(7L, 1L, req);
        assertThat(response).isEqualTo(CategoryResponse.from(updated));
    }

    @Test
    void delete_callsDeleteForTenant_whenTenant() {
        controller.delete(5L, tenantPrincipal(7L));

        verify(service).deleteForTenant(7L, 5L);
        verify(service, never()).delete(anyLong());
    }

    @Test
    void delete_callsDelete_whenAdmin() {
        controller.delete(5L, adminPrincipal(99L));

        verify(service).delete(5L);
        verify(service, never()).deleteForTenant(anyLong(), anyLong());
    }

    @Test
    void delete_propagatesServiceException() {
        RuntimeException boom = new RuntimeException("nope");
        willThrow(boom).given(service).deleteForTenant(7L, 99L);

        assertThatThrownBy(() -> controller.delete(99L, tenantPrincipal(7L))).isSameAs(boom);
    }

    private static TenantUserDetails tenantPrincipal(Long id) {
        return new TenantUserDetails(id, "tenant@example.com", "TENANT");
    }

    private static TenantUserDetails adminPrincipal(Long id) {
        return new TenantUserDetails(id, "admin@example.com", "ADMIN");
    }

    private static Category category(Long id, Long tenantId, String name) {
        return new Category(
                id, AggregateReference.<Tenant, Long>to(tenantId), name, null, null, OffsetDateTime.now());
    }
}
