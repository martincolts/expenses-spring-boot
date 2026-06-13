export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  refreshToken: string
  id: number
  name: string
  email: string
  role: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ForgotPasswordResponse {
  resetUrl: string | null
}

export interface ConfirmResetPasswordRequest {
  token: string
  newPassword: string
}

export interface AuthUser {
  token: string
  refreshToken: string
  id: number
  name: string
  email: string
  role: string
}
