// Authenticated fetch wrapper with transparent token refresh on 401
let onUnauthenticated: (() => void) | null = null
let onTokenRefreshed: ((token: string) => void) | null = null
let isRefreshing = false
let refreshQueue: Array<(token: string | null) => void> = []

export function configureClient(handlers: {
  onUnauthenticated: () => void
  onTokenRefreshed: (token: string) => void
}) {
  onUnauthenticated = handlers.onUnauthenticated
  onTokenRefreshed = handlers.onTokenRefreshed
}

export function getToken(): string | null {
  try {
    const raw = localStorage.getItem('auth')
    return raw ? JSON.parse(raw).token : null
  } catch {
    return null
  }
}

export function getRefreshToken(): string | null {
  try {
    const raw = localStorage.getItem('auth')
    return raw ? JSON.parse(raw).refreshToken : null
  } catch {
    return null
  }
}

async function doRefresh(refreshToken: string): Promise<string | null> {
  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const data = await res.json()
    return (data as { token?: string }).token ?? null
  } catch {
    return null
  }
}

async function attemptRefresh(): Promise<string | null> {
  const current = getRefreshToken()
  if (!current) return null

  if (isRefreshing) {
    return new Promise(resolve => { refreshQueue.push(resolve) })
  }

  isRefreshing = true
  const newToken = await doRefresh(current)
  isRefreshing = false

  refreshQueue.forEach(resolve => resolve(newToken))
  refreshQueue = []

  if (newToken) {
    onTokenRefreshed?.(newToken)
  } else {
    onUnauthenticated?.()
  }
  return newToken
}

export async function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const token = getToken()
  const headers: Record<string, string> = {
    ...(init.headers as Record<string, string> ?? {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  let res = await fetch(url, { ...init, headers })

  if (res.status === 401) {
    const newToken = await attemptRefresh()
    if (newToken) {
      res = await fetch(url, {
        ...init,
        headers: { ...headers, Authorization: `Bearer ${newToken}` },
      })
    }
  }

  return res
}
