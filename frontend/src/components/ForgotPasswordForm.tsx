import { useState } from 'react'
import { useFormik } from 'formik'
import * as Yup from 'yup'
import { Alert, Box, Button, Link, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { useRequestPasswordReset } from '../hooks/useAuth'

const schema = Yup.object({
  email: Yup.string().email('Invalid email').required('Required'),
})

export function ForgotPasswordForm() {
  const { mutate, isPending, isError, error } = useRequestPasswordReset()
  const [resetUrl, setResetUrl] = useState<string | null | undefined>(undefined)

  const formik = useFormik({
    initialValues: { email: '' },
    validationSchema: schema,
    onSubmit: (values) =>
      mutate(values, {
        onSuccess: (data) => setResetUrl(data.resetUrl),
      }),
  })

  if (resetUrl !== undefined) {
    return (
      <Stack spacing={2.5}>
        <Alert severity="success" variant="outlined">
          If an account exists for that email, a password reset link has been generated.
        </Alert>
        {resetUrl && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              No email is sent yet — use this temporary link to reset your password:
            </Typography>
            <Link
              href={resetUrl}
              sx={{ wordBreak: 'break-all', fontSize: '0.85rem' }}
            >
              {resetUrl}
            </Link>
          </Box>
        )}
        <Button component={RouterLink} to="/login" variant="text" fullWidth>
          Back to sign in
        </Button>
      </Stack>
    )
  }

  return (
    <Box component="form" onSubmit={formik.handleSubmit} noValidate>
      <Stack spacing={2.5}>
        {isError && (
          <Alert severity="error" variant="outlined">
            {error.message}
          </Alert>
        )}
        <Typography variant="body2" color="text.secondary">
          Enter your email and we'll generate a link to reset your password.
        </Typography>
        <TextField
          label="Email"
          type="email"
          {...formik.getFieldProps('email')}
          error={formik.touched.email && Boolean(formik.errors.email)}
          helperText={formik.touched.email && formik.errors.email}
          fullWidth
          autoComplete="email"
          autoFocus
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={isPending}
          sx={{ mt: 1, py: 1.5, fontWeight: 600 }}
        >
          {isPending ? 'Sending…' : 'Send reset link'}
        </Button>
        <Link component={RouterLink} to="/login" variant="body2" align="center">
          Back to sign in
        </Link>
      </Stack>
    </Box>
  )
}
