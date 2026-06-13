package com.tincho.expenses.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.jsonwebtoken.JwtException;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tincho.expenses.config.PasswordResetProperties;
import com.tincho.expenses.controllers.dto.ConfirmResetPasswordRequest;
import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordResponse;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.RefreshRequest;
import com.tincho.expenses.controllers.dto.RefreshResponse;
import com.tincho.expenses.controllers.dto.ResetPasswordRequest;
import com.tincho.expenses.exceptions.InvalidCurrentPasswordException;
import com.tincho.expenses.exceptions.InvalidPasswordResetTokenException;
import com.tincho.expenses.exceptions.InvalidRefreshTokenException;
import com.tincho.expenses.exceptions.TenantNotFoundException;
import com.tincho.expenses.model.PasswordResetToken;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.PasswordResetTokenRepository;
import com.tincho.expenses.repositories.TenantRepository;
import com.tincho.expenses.security.JwtService;
import com.tincho.expenses.security.TenantUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetProperties passwordResetProperties;

    public LoginResponse register(CreateTenantRequest req) {
        Tenant saved = tenantService.create(req);
        return buildLoginResponse(saved);
    }

    public LoginResponse login(LoginRequest req) {
        Tenant tenant = tenantRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), tenant.passwordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return buildLoginResponse(tenant);
    }

    public RefreshResponse refresh(RefreshRequest req) {
        String email;
        try {
            email = jwtService.extractRefreshSubject(req.refreshToken());
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException();
        }
        Tenant tenant = tenantRepository.findByEmail(email)
                .orElseThrow(InvalidRefreshTokenException::new);
        String token = jwtService.generateToken(new TenantUserDetails(tenant));
        return new RefreshResponse(token);
    }

    private LoginResponse buildLoginResponse(Tenant tenant) {
        TenantUserDetails userDetails = new TenantUserDetails(tenant);
        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new LoginResponse(token, refreshToken, tenant.id(), tenant.name(), tenant.email(), tenant.role());
    }

    @Transactional
    public void resetPassword(String email, ResetPasswordRequest req) {
        Tenant tenant = tenantRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new TenantNotFoundException(-1L));
        if (!passwordEncoder.matches(req.currentPassword(), tenant.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        Tenant updated = new Tenant(
                tenant.id(), tenant.name(), tenant.email(),
                passwordEncoder.encode(req.newPassword()), tenant.role(), tenant.createdAt());
        tenantRepository.save(updated);
    }

    @Transactional
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest req) {
        // No email is sent yet: the token + reset URL are persisted in the DB and
        // (temporarily) returned in the response so they can be used without email.
        // A real email flow would simply no-op here when the email is unknown.
        return tenantRepository.findByEmail(req.email())
                .map(this::createResetToken)
                .orElseGet(() -> new ForgotPasswordResponse(null));
    }

    private ForgotPasswordResponse createResetToken(Tenant tenant) {
        // Invalidate any prior unused tokens so only the newest one works.
        passwordResetTokenRepository.invalidateActiveForTenant(tenant.id());

        String token = UUID.randomUUID().toString();
        String resetUrl = passwordResetProperties.baseUrl() + "?token=" + token;
        OffsetDateTime now = OffsetDateTime.now();

        passwordResetTokenRepository.save(new PasswordResetToken(
                null,
                AggregateReference.to(tenant.id()),
                token,
                resetUrl,
                now.plusNanos(passwordResetProperties.expirationMs() * 1_000_000),
                null,
                now));

        return new ForgotPasswordResponse(resetUrl);
    }

    @Transactional
    public void confirmResetPassword(ConfirmResetPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(req.token())
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (resetToken.usedAt() != null || resetToken.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidPasswordResetTokenException();
        }

        Tenant tenant = tenantRepository.findById(resetToken.tenant().getId())
                .orElseThrow(InvalidPasswordResetTokenException::new);
        Tenant updated = new Tenant(
                tenant.id(), tenant.name(), tenant.email(),
                passwordEncoder.encode(req.newPassword()), tenant.role(), tenant.createdAt());
        tenantRepository.save(updated);

        passwordResetTokenRepository.save(new PasswordResetToken(
                resetToken.id(), resetToken.tenant(), resetToken.token(), resetToken.resetUrl(),
                resetToken.expiresAt(), OffsetDateTime.now(), resetToken.createdAt()));
    }
}
