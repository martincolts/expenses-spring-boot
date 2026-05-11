import { useMutation } from '@tanstack/react-query'
import { login, register } from '../api/auth'
import type { LoginRequest, RegisterRequest, AuthResponse } from '../types/auth'

export function useLogin() {
  return useMutation<AuthResponse, Error, LoginRequest>({
    mutationFn: login,
    onSuccess: (data) => {
      localStorage.setItem('token', data.token)
    },
  })
}

export function useRegister() {
  return useMutation<AuthResponse, Error, RegisterRequest>({
    mutationFn: register,
    onSuccess: (data) => {
      localStorage.setItem('token', data.token)
    },
  })
}
