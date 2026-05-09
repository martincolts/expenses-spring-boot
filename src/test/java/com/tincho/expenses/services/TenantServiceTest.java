package com.tincho.expenses.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.UpdateTenantRequest;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    TenantRepository repository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    TenantService service;

    @Test
    void create_persistsTenant_withEncodedPassword() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "new@example.com", "plaintext");
        given(repository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("plaintext")).willReturn("encoded-hash");
        given(repository.save(any(Tenant.class)))
                .willAnswer(inv -> {
                    Tenant t = inv.getArgument(0);
                    return new Tenant(1L, t.name(), t.email(), t.passwordHash(), t.createdAt());
                });

        Tenant created = service.create(req);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(repository).save(captor.capture());
        Tenant toSave = captor.getValue();
        assertThat(toSave.id()).isNull();
        assertThat(toSave.name()).isEqualTo("Acme");
        assertThat(toSave.email()).isEqualTo("new@example.com");
        assertThat(toSave.passwordHash()).isEqualTo("encoded-hash");
        assertThat(toSave.createdAt()).isNotNull();

        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.passwordHash()).isEqualTo("encoded-hash");
    }

    @Test
    void create_throwsWhenEmailAlreadyExists() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "dup@example.com", "plaintext");
        given(repository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(TenantEmailAlreadyExistsException.class)
                .hasMessageContaining("dup@example.com");

        verify(passwordEncoder, never()).encode(any());
        verify(repository, never()).save(any());
    }

    @Test
    void list_returnsRepositoryResults() {
        Tenant a = new Tenant(1L, "A", "a@example.com", "h", OffsetDateTime.now());
        Tenant b = new Tenant(2L, "B", "b@example.com", "h", OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(a, b));

        assertThat(service.list()).containsExactly(a, b);
    }

    @Test
    void get_returnsTenant_whenFound() {
        Tenant t = new Tenant(1L, "Acme", "a@example.com", "h", OffsetDateTime.now());
        given(repository.findById(1L)).willReturn(Optional.of(t));

        assertThat(service.get(1L)).isSameAs(t);
    }

    @Test
    void get_throws_whenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_changesNameAndPreservesOtherFields() {
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        Tenant existing = new Tenant(1L, "Old", "keep@example.com", "keep-hash", createdAt);
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(repository.save(any(Tenant.class))).willAnswer(inv -> inv.getArgument(0));

        Tenant updated = service.update(1L, new UpdateTenantRequest("New"));

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(repository).save(captor.capture());
        Tenant toSave = captor.getValue();
        assertThat(toSave.id()).isEqualTo(1L);
        assertThat(toSave.name()).isEqualTo("New");
        assertThat(toSave.email()).isEqualTo("keep@example.com");
        assertThat(toSave.passwordHash()).isEqualTo("keep-hash");
        assertThat(toSave.createdAt()).isEqualTo(createdAt);

        assertThat(updated.name()).isEqualTo("New");
    }

    @Test
    void update_throws_whenTenantMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateTenantRequest("New")))
                .isInstanceOf(TenantNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void delete_removesTenant_whenExists() {
        given(repository.existsById(1L)).willReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throws_whenMissing() {
        given(repository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).deleteById(any());
    }
}
