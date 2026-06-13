package com.tincho.expenses.controllers;

import com.tincho.expenses.controllers.dto.ConfirmResetPasswordRequest;
import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordResponse;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.RefreshRequest;
import com.tincho.expenses.controllers.dto.RefreshResponse;
import com.tincho.expenses.controllers.dto.ResetPasswordRequest;
import com.tincho.expenses.security.TenantUserDetails;
import com.tincho.expenses.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody CreateTenantRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req,
                       @AuthenticationPrincipal TenantUserDetails principal) {
        authService.resetPassword(principal.getUsername(), req);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return authService.requestPasswordReset(req);
    }

    @PostMapping("/confirm-reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmResetPassword(@Valid @RequestBody ConfirmResetPasswordRequest req) {
        authService.confirmResetPassword(req);
    }
}
