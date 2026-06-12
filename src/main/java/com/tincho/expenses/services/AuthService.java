package com.tincho.expenses.services;

import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.RefreshRequest;
import com.tincho.expenses.controllers.dto.RefreshResponse;
import com.tincho.expenses.controllers.dto.ResetPasswordRequest;
import com.tincho.expenses.exceptions.InvalidCurrentPasswordException;
import com.tincho.expenses.exceptions.InvalidRefreshTokenException;
import com.tincho.expenses.exceptions.TenantNotFoundException;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.TenantRepository;
import com.tincho.expenses.security.JwtService;
import com.tincho.expenses.security.TenantUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
}
