import { AuthCardShell } from '../components/AuthCardShell'
import { ForgotPasswordForm } from '../components/ForgotPasswordForm'

export function ForgotPasswordPage() {
  return (
    <AuthCardShell title="Forgot password?" subtitle="Reset your account password.">
      <ForgotPasswordForm />
    </AuthCardShell>
  )
}
