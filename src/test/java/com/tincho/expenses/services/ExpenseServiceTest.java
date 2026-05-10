package com.tincho.expenses.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateExpenseRequest;
import com.tincho.expenses.controllers.dto.UpdateExpenseRequest;
import com.tincho.expenses.exceptions.CategoryNotFoundException;
import com.tincho.expenses.exceptions.ExpenseAlreadyPaidException;
import com.tincho.expenses.exceptions.ExpenseNotFoundException;
import com.tincho.expenses.exceptions.ExpenseNotPaidException;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Expense;
import com.tincho.expenses.model.ExpenseStatus;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;
import com.tincho.expenses.repositories.ExpenseRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    ExpenseRepository repository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    ExpenseService service;

    @Test
    void create_persistsPendingExpense_whenPaidFlagFalse() {
        OffsetDateTime incurredAt = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        CreateExpenseRequest req = new CreateExpenseRequest(
                3L, "Lunch", new BigDecimal("12.50"), "usd", incurredAt, false);
        givenCategoryOwned(3L, 7L);
        given(repository.save(any(Expense.class)))
                .willAnswer(inv -> withId(42L, inv.getArgument(0)));

        Expense created = service.create(7L, req);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        Expense toSave = captor.getValue();
        assertThat(toSave.id()).isNull();
        assertThat(toSave.tenant().getId()).isEqualTo(7L);
        assertThat(toSave.category().getId()).isEqualTo(3L);
        assertThat(toSave.description()).isEqualTo("Lunch");
        assertThat(toSave.amount()).isEqualByComparingTo("12.50");
        assertThat(toSave.currency()).isEqualTo("USD");
        assertThat(toSave.status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
        assertThat(toSave.incurredAt()).isEqualTo(incurredAt);
        assertThat(toSave.createdAt()).isNotNull();
        assertThat(toSave.paidAt()).isNull();

        assertThat(created.id()).isEqualTo(42L);
    }

    @Test
    void create_persistsPaidExpense_whenPaidFlagTrue() {
        CreateExpenseRequest req = new CreateExpenseRequest(
                3L, "Lunch", new BigDecimal("12.50"), "USD",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"), true);
        givenCategoryOwned(3L, 7L);
        given(repository.save(any(Expense.class))).willAnswer(inv -> inv.getArgument(0));

        service.create(7L, req);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        Expense toSave = captor.getValue();
        assertThat(toSave.status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(toSave.paidAt()).isNotNull();
    }

    @Test
    void create_throws_whenCategoryNotOwnedByTenant() {
        CreateExpenseRequest req = new CreateExpenseRequest(
                3L, "Lunch", new BigDecimal("12.50"), "USD",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"), false);
        given(categoryRepository.findByIdAndTenantId(3L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(7L, req))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("3");

        verify(repository, never()).save(any());
    }

    @Test
    void listForTenant_returnsRepositoryResults() {
        Expense a = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        Expense b = expense(2L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findByTenantId(7L)).willReturn(List.of(a, b));

        assertThat(service.listForTenant(7L)).containsExactly(a, b);
    }

    @Test
    void listAll_returnsRepositoryResults() {
        Expense a = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        Expense b = expense(2L, 8L, 4L, ExpenseStatus.NOT_PAUD_YET);
        given(repository.findAll()).willReturn(List.of(a, b));

        assertThat(service.listAll()).containsExactly(a, b);
    }

    @Test
    void getForTenant_returnsExpense_whenOwned() {
        Expense e = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(e));

        assertThat(service.getForTenant(7L, 1L)).isSameAs(e);
    }

    @Test
    void getForTenant_throws_whenNotOwnedOrMissing() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForTenant(7L, 1L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void get_returnsExpense_whenFound() {
        Expense e = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findById(1L)).willReturn(Optional.of(e));

        assertThat(service.get(1L)).isSameAs(e);
    }

    @Test
    void get_throws_whenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void update_changesEditableFields_andPreservesStatusAndPaidAt() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        OffsetDateTime paidAt = OffsetDateTime.parse("2026-04-02T00:00:00Z");
        Expense existing = new Expense(
                1L,
                AggregateReference.<Tenant, Long>to(7L),
                AggregateReference.<Category, Long>to(3L),
                "Old desc", new BigDecimal("10.00"), "USD",
                ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                createdAt,
                paidAt);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        givenCategoryOwned(4L, 7L);
        given(repository.save(any(Expense.class))).willAnswer(inv -> inv.getArgument(0));

        OffsetDateTime newIncurredAt = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        UpdateExpenseRequest req = new UpdateExpenseRequest(
                4L, "New desc", new BigDecimal("20.00"), "eur", newIncurredAt);

        service.update(7L, 1L, req);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        Expense toSave = captor.getValue();
        assertThat(toSave.id()).isEqualTo(1L);
        assertThat(toSave.tenant().getId()).isEqualTo(7L);
        assertThat(toSave.category().getId()).isEqualTo(4L);
        assertThat(toSave.description()).isEqualTo("New desc");
        assertThat(toSave.amount()).isEqualByComparingTo("20.00");
        assertThat(toSave.currency()).isEqualTo("EUR");
        assertThat(toSave.incurredAt()).isEqualTo(newIncurredAt);
        assertThat(toSave.status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(toSave.createdAt()).isEqualTo(createdAt);
        assertThat(toSave.paidAt()).isEqualTo(paidAt);
    }

    @Test
    void update_throws_whenNotOwned() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, 1L, new UpdateExpenseRequest(
                3L, "x", new BigDecimal("1.00"), "USD",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"))))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_throws_whenCategoryNotOwnedByTenant() {
        Expense existing = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(categoryRepository.findByIdAndTenantId(99L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, 1L, new UpdateExpenseRequest(
                99L, "x", new BigDecimal("1.00"), "USD",
                OffsetDateTime.parse("2026-05-09T13:00:00Z"))))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void markPaidForTenant_setsStatusAndPaidAt() {
        Expense existing = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(repository.save(any(Expense.class))).willAnswer(inv -> inv.getArgument(0));

        service.markPaidForTenant(7L, 1L);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(captor.getValue().paidAt()).isNotNull();
    }

    @Test
    void markPaidForTenant_throws_whenAlreadyPaid() {
        Expense existing = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.markPaidForTenant(7L, 1L))
                .isInstanceOf(ExpenseAlreadyPaidException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void markPaid_throws_whenMissing() {
        given(repository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(1L))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void markUnpaidForTenant_clearsStatusAndPaidAt() {
        Expense existing = new Expense(
                1L,
                AggregateReference.<Tenant, Long>to(7L),
                AggregateReference.<Category, Long>to(3L),
                "Lunch", new BigDecimal("10.00"), "USD",
                ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T13:00:00Z"),
                OffsetDateTime.parse("2026-05-09T14:00:00Z"),
                OffsetDateTime.parse("2026-05-09T15:00:00Z"));
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(repository.save(any(Expense.class))).willAnswer(inv -> inv.getArgument(0));

        service.markUnpaidForTenant(7L, 1L);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
        assertThat(captor.getValue().paidAt()).isNull();
    }

    @Test
    void markUnpaidForTenant_throws_whenNotPaid() {
        Expense existing = expense(1L, 7L, 3L, ExpenseStatus.NOT_PAUD_YET);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.markUnpaidForTenant(7L, 1L))
                .isInstanceOf(ExpenseNotPaidException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void markUnpaid_throws_whenMissing() {
        given(repository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markUnpaid(1L))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void deleteForTenant_removes_whenOwned() {
        Expense existing = expense(1L, 7L, 3L, ExpenseStatus.PAID);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));

        service.deleteForTenant(7L, 1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteForTenant_throws_whenNotOwned() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteForTenant(7L, 1L))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_removes_whenExists() {
        given(repository.existsById(1L)).willReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throws_whenMissing() {
        given(repository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    private void givenCategoryOwned(Long categoryId, Long tenantId) {
        Category c = new Category(
                categoryId, AggregateReference.<Tenant, Long>to(tenantId), "Cat", OffsetDateTime.now());
        given(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).willReturn(Optional.of(c));
    }

    private static Expense expense(Long id, Long tenantId, Long categoryId, ExpenseStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        return new Expense(
                id,
                AggregateReference.<Tenant, Long>to(tenantId),
                AggregateReference.<Category, Long>to(categoryId),
                "desc", new BigDecimal("10.00"), "USD",
                status,
                now,
                now,
                status == ExpenseStatus.PAID ? now : null);
    }

    private static Expense withId(Long id, Expense e) {
        return new Expense(
                id, e.tenant(), e.category(), e.description(), e.amount(), e.currency(),
                e.status(), e.incurredAt(), e.createdAt(), e.paidAt());
    }
}
