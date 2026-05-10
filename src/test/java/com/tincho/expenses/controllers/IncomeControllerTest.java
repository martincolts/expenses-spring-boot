package com.tincho.expenses.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.controllers.dto.IncomeResponse;
import com.tincho.expenses.model.Income;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.IncomeService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

@ExtendWith(MockitoExtension.class)
class IncomeControllerTest {

    @Mock
    IncomeService service;

    @InjectMocks
    IncomeController controller;

    @Test
    void create_delegatesToService_andReturnsResponse() {
        TenantUserDetails principal = tenantPrincipal(7L);
        CreateIncomeRequest req = new CreateIncomeRequest(new BigDecimal("100.00"));
        Income saved = income(1L, 7L);
        given(service.create(7L, req)).willReturn(saved);

        IncomeResponse response = controller.create(req, principal);

        verify(service).create(7L, req);
        assertThat(response).isEqualTo(IncomeResponse.from(saved));
    }

    @Test
    void list_callsListForTenant_whenTenant() {
        TenantUserDetails principal = tenantPrincipal(7L);
        Income a = income(1L, 7L);
        Income b = income(2L, 7L);
        given(service.listForTenant(7L)).willReturn(List.of(a, b));

        List<IncomeResponse> response = controller.list(principal);

        verify(service).listForTenant(7L);
        verify(service, never()).listAll();
        assertThat(response).containsExactly(IncomeResponse.from(a), IncomeResponse.from(b));
    }

    @Test
    void list_callsListAll_whenAdmin() {
        TenantUserDetails principal = adminPrincipal(99L);
        Income a = income(1L, 7L);
        given(service.listAll()).willReturn(List.of(a));

        controller.list(principal);

        verify(service).listAll();
        verify(service, never()).listForTenant(anyLong());
    }

    @Test
    void get_callsGetForTenant_whenTenant() {
        Income i = income(1L, 7L);
        given(service.getForTenant(7L, 1L)).willReturn(i);

        IncomeResponse response = controller.get(1L, tenantPrincipal(7L));

        verify(service).getForTenant(7L, 1L);
        verify(service, never()).get(anyLong());
        assertThat(response).isEqualTo(IncomeResponse.from(i));
    }

    @Test
    void get_callsGet_whenAdmin() {
        Income i = income(1L, 7L);
        given(service.get(1L)).willReturn(i);

        controller.get(1L, adminPrincipal(99L));

        verify(service).get(1L);
        verify(service, never()).getForTenant(anyLong(), anyLong());
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

    private static Income income(Long id, Long tenantId) {
        return new Income(
                id,
                AggregateReference.<Tenant, Long>to(tenantId),
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2026-05-09T13:00:00Z"));
    }
}
