import { createContext, useContext, useState, type ReactNode } from 'react'
import type { AuthResponse, AuthUser } from '../types/auth'

interface AuthContextValue {
  user: AuthUser | null
  login: (data: AuthResponse) => void
  logout: () => void
  updateToken: (token: string) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const AUTH_KEY = 'auth'

function loadUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadUser)

  function login(data: AuthResponse) {
    const u: AuthUser = {
      token: data.token,
      id: data.id,
      name: data.name,
      email: data.email,
      role: data.role,
    }
    localStorage.setItem(AUTH_KEY, JSON.stringify(u))
    setUser(u)
  }

  function logout() {
    localStorage.removeItem(AUTH_KEY)
    setUser(null)
  }

  function updateToken(token: string) {
    setUser(prev => {
      if (!prev) return null
      const updated = { ...prev, token }
      localStorage.setItem(AUTH_KEY, JSON.stringify(updated))
      return updated
    })
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, updateToken }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuthContext() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuthContext must be used within AuthProvider')
  return ctx
}
