import { Navigate, Outlet } from 'react-router-dom'
import { useAuthContext } from '../contexts/AuthContext'
import { AppShell } from './AppShell'

export function ProtectedRoute() {
  const { user } = useAuthContext()
  if (!user) return <Navigate to="/login" replace />
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}
