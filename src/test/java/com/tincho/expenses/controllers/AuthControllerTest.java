package com.tincho.expenses.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.tincho.expenses.controllers.dto.ConfirmResetPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordRequest;
import com.tincho.expenses.controllers.dto.ForgotPasswordResponse;
import com.tincho.expenses.exceptions.InvalidPasswordResetTokenException;
import com.tincho.expenses.services.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthService authService;

    @InjectMocks
    AuthController controller;

    @Test
    void forgotPassword_delegatesToServiceAndReturnsResponse() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("user@example.com");
        ForgotPasswordResponse expected =
                new ForgotPasswordResponse("http://localhost:5173/reset-password?token=abc");
        given(authService.requestPasswordReset(req)).willReturn(expected);

        ForgotPasswordResponse response = controller.forgotPassword(req);

        verify(authService).requestPasswordReset(req);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void confirmResetPassword_delegatesToService() {
        ConfirmResetPasswordRequest req = new ConfirmResetPasswordRequest("tok", "brand-new-pass");

        controller.confirmResetPassword(req);

        verify(authService).confirmResetPassword(req);
    }

    @Test
    void confirmResetPassword_propagatesInvalidTokenException() {
        ConfirmResetPasswordRequest req = new ConfirmResetPasswordRequest("bad", "brand-new-pass");
        willThrow(new InvalidPasswordResetTokenException()).given(authService).confirmResetPassword(req);

        assertThatThrownBy(() -> controller.confirmResetPassword(req))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }
}
