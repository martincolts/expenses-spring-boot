import { useMutation } from '@tanstack/react-query'
import { login, register, requestPasswordReset, confirmPasswordReset } from '../api/auth'
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ConfirmResetPasswordRequest,
} from '../types/auth'

export function useLogin() {
  return useMutation<AuthResponse, Error, LoginRequest>({ mutationFn: login })
}

export function useRegister() {
  return useMutation<AuthResponse, Error, RegisterRequest>({ mutationFn: register })
}

export function useRequestPasswordReset() {
  return useMutation<ForgotPasswordResponse, Error, ForgotPasswordRequest>({
    mutationFn: requestPasswordReset,
  })
}

export function useConfirmPasswordReset() {
  return useMutation<void, Error, ConfirmResetPasswordRequest>({
    mutationFn: confirmPasswordReset,
  })
}
