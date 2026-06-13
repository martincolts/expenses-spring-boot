package com.tincho.expenses.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.config.PasswordResetProperties;
import com.tincho.expenses.controllers.dto.ConfirmResetPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordResponse;
import com.tincho.expenses.exceptions.InvalidPasswordResetTokenException;
import com.tincho.expenses.model.PasswordResetToken;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.PasswordResetTokenRepository;
import com.tincho.expenses.repositories.TenantRepository;
import com.tincho.expenses.security.JwtService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    TenantService tenantService;
    @Mock
    TenantRepository tenantRepository;
    @Mock
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;

    AuthService service;

    final PasswordResetProperties props =
            new PasswordResetProperties("http://localhost:5173/reset-password", 3_600_000);

    @BeforeEach
    void setUp() {
        service = new AuthService(tenantService, tenantRepository, passwordResetTokenRepository,
                passwordEncoder, jwtService, props);
    }

    private Tenant tenant() {
        return new Tenant(1L, "Acme", "user@example.com", "old-hash", "TENANT", OffsetDateTime.now());
    }

    @Test
    void requestPasswordReset_invalidatesPriorTokens_andStoresNewOne() {
        given(tenantRepository.findByEmail("user@example.com")).willReturn(Optional.of(tenant()));

        ForgotPasswordResponse response =
                service.requestPasswordReset(new ForgotPasswordRequest("user@example.com"));

        verify(passwordResetTokenRepository).invalidateActiveForTenant(1L);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.id()).isNull();
        assertThat(saved.tenant().getId()).isEqualTo(1L);
        assertThat(saved.usedAt()).isNull();
        assertThat(saved.expiresAt()).isAfter(OffsetDateTime.now().plusMinutes(50));
        assertThat(saved.resetUrl()).isEqualTo(response.resetUrl());
        assertThat(response.resetUrl())
                .startsWith("http://localhost:5173/reset-password?token=")
                .endsWith(saved.token());
    }

    @Test
    void requestPasswordReset_returnsNullUrl_andStoresNothing_whenEmailUnknown() {
        given(tenantRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        ForgotPasswordResponse response =
                service.requestPasswordReset(new ForgotPasswordRequest("ghost@example.com"));

        assertThat(response.resetUrl()).isNull();
        verify(passwordResetTokenRepository, never()).invalidateActiveForTenant(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void confirmResetPassword_updatesPassword_andMarksTokenUsed() {
        PasswordResetToken token = new PasswordResetToken(
                10L, AggregateReference.to(1L), "tok-1", "url",
                OffsetDateTime.now().plusHours(1), null, OffsetDateTime.now());
        given(passwordResetTokenRepository.findByToken("tok-1")).willReturn(Optional.of(token));
        given(tenantRepository.findById(1L)).willReturn(Optional.of(tenant()));
        given(passwordEncoder.encode("brand-new-pass")).willReturn("new-hash");

        service.confirmResetPassword(new ConfirmResetPasswordRequest("tok-1", "brand-new-pass"));

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().passwordHash()).isEqualTo("new-hash");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().usedAt()).isNotNull();
    }

    @Test
    void confirmResetPassword_throws_whenTokenUnknown() {
        given(passwordResetTokenRepository.findByToken("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.confirmResetPassword(new ConfirmResetPasswordRequest("nope", "brand-new-pass")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void confirmResetPassword_throws_whenTokenAlreadyUsed() {
        PasswordResetToken used = new PasswordResetToken(
                10L, AggregateReference.to(1L), "tok-1", "url",
                OffsetDateTime.now().plusHours(1), OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now());
        given(passwordResetTokenRepository.findByToken("tok-1")).willReturn(Optional.of(used));

        assertThatThrownBy(() ->
                service.confirmResetPassword(new ConfirmResetPasswordRequest("tok-1", "brand-new-pass")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void confirmResetPassword_throws_whenTokenExpired() {
        PasswordResetToken expired = new PasswordResetToken(
                10L, AggregateReference.to(1L), "tok-1", "url",
                OffsetDateTime.now().minusMinutes(1), null, OffsetDateTime.now().minusHours(2));
        given(passwordResetTokenRepository.findByToken("tok-1")).willReturn(Optional.of(expired));

        assertThatThrownBy(() ->
                service.confirmResetPassword(new ConfirmResetPasswordRequest("tok-1", "brand-new-pass")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(tenantRepository, never()).save(any());
    }
}
