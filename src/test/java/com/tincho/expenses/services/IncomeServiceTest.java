package com.tincho.expenses.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.exceptions.IncomeNotFoundException;
import com.tincho.expenses.model.Income;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.IncomeRepository;
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
class IncomeServiceTest {

    @Mock
    IncomeRepository repository;

    @InjectMocks
    IncomeService service;

    @Test
    void create_persistsIncome_withTenantAmountAndCreatedAt() {
        CreateIncomeRequest req = new CreateIncomeRequest(new BigDecimal("123.45"));
        given(repository.save(any(Income.class)))
                .willAnswer(inv -> withId(42L, inv.getArgument(0)));

        Income created = service.create(7L, req);

        ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
        verify(repository).save(captor.capture());
        Income toSave = captor.getValue();
        assertThat(toSave.id()).isNull();
        assertThat(toSave.tenant().getId()).isEqualTo(7L);
        assertThat(toSave.amount()).isEqualByComparingTo("123.45");
        assertThat(toSave.createdAt()).isNotNull();

        assertThat(created.id()).isEqualTo(42L);
    }

    @Test
    void listForTenant_returnsRepositoryResults() {
        Income a = income(1L, 7L);
        Income b = income(2L, 7L);
        given(repository.findByTenantId(7L)).willReturn(List.of(a, b));

        assertThat(service.listForTenant(7L)).containsExactly(a, b);
    }

    @Test
    void listAll_returnsRepositoryResults() {
        Income a = income(1L, 7L);
        Income b = income(2L, 8L);
        given(repository.findAll()).willReturn(List.of(a, b));

        assertThat(service.listAll()).containsExactly(a, b);
    }

    @Test
    void getForTenant_returnsIncome_whenOwned() {
        Income i = income(1L, 7L);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(i));

        assertThat(service.getForTenant(7L, 1L)).isSameAs(i);
    }

    @Test
    void getForTenant_throws_whenNotOwnedOrMissing() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForTenant(7L, 1L))
                .isInstanceOf(IncomeNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void get_returnsIncome_whenFound() {
        Income i = income(1L, 7L);
        given(repository.findById(1L)).willReturn(Optional.of(i));

        assertThat(service.get(1L)).isSameAs(i);
    }

    @Test
    void get_throws_whenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(IncomeNotFoundException.class);
    }

    @Test
    void deleteForTenant_removes_whenOwned() {
        Income existing = income(1L, 7L);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));

        service.deleteForTenant(7L, 1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteForTenant_throws_whenNotOwned() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteForTenant(7L, 1L))
                .isInstanceOf(IncomeNotFoundException.class);

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
                .isInstanceOf(IncomeNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    private static Income income(Long id, Long tenantId) {
        return new Income(
                id,
                AggregateReference.<Tenant, Long>to(tenantId),
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2026-05-09T13:00:00Z"));
    }

    private static Income withId(Long id, Income i) {
        return new Income(id, i.tenant(), i.amount(), i.createdAt());
    }
}
