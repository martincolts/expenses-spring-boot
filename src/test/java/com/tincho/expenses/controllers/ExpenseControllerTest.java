package com.tincho.expenses.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateExpenseRequest;
import com.tincho.expenses.controllers.dto.ExpenseResponse;
import com.tincho.expenses.controllers.dto.PageResponse;
import com.tincho.expenses.controllers.dto.UpdateExpenseRequest;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Expense;
import com.tincho.expenses.model.ExpenseStatus;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.ExpenseService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    ExpenseService service;

    @InjectMocks
    ExpenseController controller;

    @Test
    void create_delegatesToService_andReturnsResponse() {
        TenantUserDetails principal = tenantPrincipal(7L);
        CreateExpenseRequest req = new CreateExpenseRequest(
                3L, "Lunch", new BigDecimal("10.00"), "USD",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"), false);
        Expense saved = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(service.create(7L, req)).willReturn(saved);

        ExpenseResponse response = controller.create(req, principal);

        verify(service).create(7L, req);
        assertThat(response).isEqualTo(ExpenseResponse.from(saved));
    }

    @Test
    void list_callsListForTenant_whenTenant() {
        TenantUserDetails principal = tenantPrincipal(7L);
        Expense a = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        Expense b = expense(2L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(service.listForTenant(7L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(a, b)));

        PageResponse<ExpenseResponse> response = controller.list(principal, 0, 20);

        verify(service).listForTenant(7L, PageRequest.of(0, 20));
        verify(service, never()).listAll(PageRequest.of(0, 20));
        assertThat(response.content()).containsExactly(ExpenseResponse.from(a), ExpenseResponse.from(b));
    }

    @Test
    void list_callsListAll_whenAdmin() {
        TenantUserDetails principal = adminPrincipal(99L);
        Expense a = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(service.listAll(PageRequest.of(0, 20))).willReturn(new PageImpl<>(List.of(a)));

        controller.list(principal, 0, 20);

        verify(service).listAll(PageRequest.of(0, 20));
        verify(service, never()).listForTenant(anyLong(), any());
    }

    @Test
    void get_callsGetForTenant_whenTenant() {
        Expense e = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(service.getForTenant(7L, 1L)).willReturn(e);

        ExpenseResponse response = controller.get(1L, tenantPrincipal(7L));

        verify(service).getForTenant(7L, 1L);
        verify(service, never()).get(anyLong());
        assertThat(response).isEqualTo(ExpenseResponse.from(e));
    }

    @Test
    void get_callsGet_whenAdmin() {
        Expense e = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(service.get(1L)).willReturn(e);

        controller.get(1L, adminPrincipal(99L));

        verify(service).get(1L);
        verify(service, never()).getForTenant(anyLong(), anyLong());
    }

    @Test
    void update_delegatesToService_andReturnsResponse() {
        TenantUserDetails principal = tenantPrincipal(7L);
        UpdateExpenseRequest req = new UpdateExpenseRequest(
                4L, "New", new BigDecimal("20.00"), "EUR",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"));
        Expense updated = expense(1L, 7L, 4L, ExpenseStatus.PAID);
        given(service.update(7L, 1L, req)).willReturn(updated);

        ExpenseResponse response = controller.update(1L, req, principal);

        verify(service).update(7L, 1L, req);
        assertThat(response).isEqualTo(ExpenseResponse.from(updated));
    }

    @Test
    void pay_callsMarkPaidForTenant_whenTenant() {
        Expense paid = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(service.markPaidForTenant(7L, 1L)).willReturn(paid);

        ExpenseResponse response = controller.pay(1L, tenantPrincipal(7L));

        verify(service).markPaidForTenant(7L, 1L);
        verify(service, never()).markPaid(anyLong());
        assertThat(response).isEqualTo(ExpenseResponse.from(paid));
    }

    @Test
    void pay_callsMarkPaid_whenAdmin() {
        Expense paid = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(service.markPaid(1L)).willReturn(paid);

        controller.pay(1L, adminPrincipal(99L));

        verify(service).markPaid(1L);
        verify(service, never()).markPaidForTenant(anyLong(), anyLong());
    }

    @Test
    void unpay_callsMarkUnpaidForTenant_whenTenant() {
        Expense unpaid = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(service.markUnpaidForTenant(7L, 1L)).willReturn(unpaid);

        ExpenseResponse response = controller.unpay(1L, tenantPrincipal(7L));

        verify(service).markUnpaidForTenant(7L, 1L);
        verify(service, never()).markUnpaid(anyLong());
        assertThat(response).isEqualTo(ExpenseResponse.from(unpaid));
    }

    @Test
    void unpay_callsMarkUnpaid_whenAdmin() {
        Expense unpaid = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(service.markUnpaid(1L)).willReturn(unpaid);

        controller.unpay(1L, adminPrincipal(99L));

        verify(service).markUnpaid(1L);
        verify(service, never()).markUnpaidForTenant(anyLong(), anyLong());
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

    private static Expense expense(Long id, Long tenantId, Long categoryId, ExpenseStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        return new Expense(
                id,
                AggregateReference.<Tenant, Long>to(tenantId),
                AggregateReference.<Category, Long>to(categoryId),
                "desc", new BigDecimal("10.00"), "USD",
                status, now, now,
                status == ExpenseStatus.PAID ? now : null);
    }
}
