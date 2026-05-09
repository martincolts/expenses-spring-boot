package com.tincho.expenses.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.TenantResponse;
import com.tincho.expenses.controllers.dto.UpdateTenantRequest;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.services.TenantService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    @Mock
    TenantService service;

    @InjectMocks
    TenantController controller;

    @Test
    void create_delegatesToServiceAndReturnsResponse() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "a@example.com", "plaintext");
        OffsetDateTime createdAt = OffsetDateTime.now();
        Tenant saved = new Tenant(1L, "Acme", "a@example.com", "hash", createdAt);
        given(service.create(req)).willReturn(saved);

        TenantResponse response = controller.create(req);

        verify(service).create(req);
        assertThat(response).isEqualTo(new TenantResponse(1L, "Acme", "a@example.com", createdAt));
    }

    @Test
    void list_mapsServiceResultsToResponses() {
        OffsetDateTime now = OffsetDateTime.now();
        Tenant a = new Tenant(1L, "A", "a@example.com", "hashA", now);
        Tenant b = new Tenant(2L, "B", "b@example.com", "hashB", now);
        given(service.list()).willReturn(List.of(a, b));

        List<TenantResponse> response = controller.list();

        assertThat(response).containsExactly(
                new TenantResponse(1L, "A", "a@example.com", now),
                new TenantResponse(2L, "B", "b@example.com", now)
        );
    }

    @Test
    void list_returnsEmptyList_whenServiceReturnsEmpty() {
        given(service.list()).willReturn(List.of());

        assertThat(controller.list()).isEmpty();
    }

    @Test
    void get_delegatesToServiceAndReturnsResponse() {
        OffsetDateTime createdAt = OffsetDateTime.now();
        Tenant t = new Tenant(7L, "Acme", "a@example.com", "hash", createdAt);
        given(service.get(7L)).willReturn(t);

        TenantResponse response = controller.get(7L);

        verify(service).get(7L);
        assertThat(response).isEqualTo(new TenantResponse(7L, "Acme", "a@example.com", createdAt));
    }

    @Test
    void update_delegatesToServiceAndReturnsResponse() {
        UpdateTenantRequest req = new UpdateTenantRequest("New Name");
        OffsetDateTime createdAt = OffsetDateTime.now();
        Tenant updated = new Tenant(3L, "New Name", "a@example.com", "hash", createdAt);
        given(service.update(3L, req)).willReturn(updated);

        TenantResponse response = controller.update(3L, req);

        verify(service).update(3L, req);
        assertThat(response).isEqualTo(new TenantResponse(3L, "New Name", "a@example.com", createdAt));
    }

    @Test
    void delete_delegatesToService() {
        controller.delete(5L);

        verify(service).delete(5L);
    }

    @Test
    void delete_propagatesServiceException() {
        RuntimeException boom = new RuntimeException("nope");
        willThrow(boom).given(service).delete(99L);

        assertThatThrownBy(() -> controller.delete(99L)).isSameAs(boom);
    }


    @Test
    void create_propagatesServiceException() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "dup@example.com", "plaintext");
        RuntimeException boom = new RuntimeException("dup");
        given(service.create(req)).willThrow(boom);

        assertThatThrownBy(() -> controller.create(req)).isSameAs(boom);
    }
}
