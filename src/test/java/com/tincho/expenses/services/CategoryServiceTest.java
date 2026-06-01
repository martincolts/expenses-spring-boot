package com.tincho.expenses.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.exceptions.CategoryNameAlreadyExistsException;
import com.tincho.expenses.exceptions.CategoryNotFoundException;
import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;
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
class CategoryServiceTest {

    @Mock
    CategoryRepository repository;

    @InjectMocks
    CategoryService service;

    @Test
    void create_persistsCategory_forTenant() {
        CreateCategoryRequest req = new CreateCategoryRequest("Food", null, null);
        given(repository.existsByTenantIdAndName(7L, "Food")).willReturn(false);
        given(repository.save(any(Category.class)))
                .willAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    return new Category(42L, c.tenant(), c.name(), c.iconId(), c.colorId(), c.createdAt());
                });

        Category created = service.create(7L, req);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        Category toSave = captor.getValue();
        assertThat(toSave.id()).isNull();
        assertThat(toSave.tenant().getId()).isEqualTo(7L);
        assertThat(toSave.name()).isEqualTo("Food");
        assertThat(toSave.createdAt()).isNotNull();

        assertThat(created.id()).isEqualTo(42L);
    }

    @Test
    void create_throwsWhenNameAlreadyExistsForTenant() {
        given(repository.existsByTenantIdAndName(7L, "Food")).willReturn(true);

        assertThatThrownBy(() -> service.create(7L, new CreateCategoryRequest("Food", null, null)))
                .isInstanceOf(CategoryNameAlreadyExistsException.class)
                .hasMessageContaining("Food");

        verify(repository, never()).save(any());
    }

    @Test
    void listForTenant_returnsRepositoryResults() {
        Category a = category(1L, 7L, "A");
        Category b = category(2L, 7L, "B");
        given(repository.findByTenantId(7L)).willReturn(List.of(a, b));

        assertThat(service.listForTenant(7L)).containsExactly(a, b);
    }

    @Test
    void listAll_returnsRepositoryResults() {
        Category a = category(1L, 7L, "A");
        Category b = category(2L, 8L, "B");
        given(repository.findAll()).willReturn(List.of(a, b));

        assertThat(service.listAll()).containsExactly(a, b);
    }

    @Test
    void getForTenant_returnsCategory_whenOwned() {
        Category c = category(1L, 7L, "Food");
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(c));

        assertThat(service.getForTenant(7L, 1L)).isSameAs(c);
    }

    @Test
    void getForTenant_throws_whenNotOwnedOrMissing() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForTenant(7L, 1L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void get_returnsCategory_whenFound() {
        Category c = category(1L, 7L, "Food");
        given(repository.findById(1L)).willReturn(Optional.of(c));

        assertThat(service.get(1L)).isSameAs(c);
    }

    @Test
    void get_throws_whenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void update_changesName_andPreservesOtherFields() {
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        Category existing = new Category(
                1L, AggregateReference.<Tenant, Long>to(7L), "Old", null, null, createdAt);
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(repository.existsByTenantIdAndName(7L, "New")).willReturn(false);
        given(repository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        Category updated = service.update(7L, 1L, new UpdateCategoryRequest("New", null, null));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        Category toSave = captor.getValue();
        assertThat(toSave.id()).isEqualTo(1L);
        assertThat(toSave.tenant().getId()).isEqualTo(7L);
        assertThat(toSave.name()).isEqualTo("New");
        assertThat(toSave.createdAt()).isEqualTo(createdAt);

        assertThat(updated.name()).isEqualTo("New");
    }

    @Test
    void update_skipsConflictCheck_whenNameUnchanged() {
        Category existing = category(1L, 7L, "Same");
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(repository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        service.update(7L, 1L, new UpdateCategoryRequest("Same", null, null));

        verify(repository, never()).existsByTenantIdAndName(any(), any());
    }

    @Test
    void update_throws_whenNewNameConflicts() {
        Category existing = category(1L, 7L, "Old");
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));
        given(repository.existsByTenantIdAndName(7L, "Taken")).willReturn(true);

        assertThatThrownBy(() ->
                service.update(7L, 1L, new UpdateCategoryRequest("Taken", null, null)))
                .isInstanceOf(CategoryNameAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_throws_whenNotOwned() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(7L, 1L, new UpdateCategoryRequest("New", null, null)))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deleteForTenant_removes_whenOwned() {
        Category existing = category(1L, 7L, "Food");
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.of(existing));

        service.deleteForTenant(7L, 1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteForTenant_throws_whenNotOwned() {
        given(repository.findByIdAndTenantId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteForTenant(7L, 1L))
                .isInstanceOf(CategoryNotFoundException.class);

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
                .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    private static Category category(Long id, Long tenantId, String name) {
        return new Category(
                id, AggregateReference.<Tenant, Long>to(tenantId), name, null, null, OffsetDateTime.now());
    }
}
