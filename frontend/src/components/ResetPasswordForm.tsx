import { useFormik } from 'formik'
import * as Yup from 'yup'
import { Alert, Box, Button, Link, Stack, TextField } from '@mui/material'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom'
import { useConfirmPasswordReset } from '../hooks/useAuth'

const schema = Yup.object({
  newPassword: Yup.string().min(8, 'At least 8 characters').required('Required'),
  confirmPassword: Yup.string()
    .oneOf([Yup.ref('newPassword')], 'Passwords must match')
    .required('Required'),
})

export function ResetPasswordForm() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { mutate, isPending, isError, error, isSuccess } = useConfirmPasswordReset()
  const navigate = useNavigate()

  const formik = useFormik({
    initialValues: { newPassword: '', confirmPassword: '' },
    validationSchema: schema,
    onSubmit: (values) => {
      if (!token) return
      mutate(
        { token, newPassword: values.newPassword },
        { onSuccess: () => setTimeout(() => navigate('/login'), 1500) },
      )
    },
  })

  if (!token) {
    return (
      <Stack spacing={2.5}>
        <Alert severity="error" variant="outlined">
          This password reset link is invalid or incomplete.
        </Alert>
        <Link component={RouterLink} to="/forgot-password" variant="body2" align="center">
          Request a new reset link
        </Link>
      </Stack>
    )
  }

  if (isSuccess) {
    return (
      <Alert severity="success" variant="outlined">
        Your password has been reset. Redirecting to sign in…
      </Alert>
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
        <TextField
          label="New password"
          type="password"
          {...formik.getFieldProps('newPassword')}
          error={formik.touched.newPassword && Boolean(formik.errors.newPassword)}
          helperText={formik.touched.newPassword && formik.errors.newPassword}
          fullWidth
          autoComplete="new-password"
          autoFocus
        />
        <TextField
          label="Confirm password"
          type="password"
          {...formik.getFieldProps('confirmPassword')}
          error={formik.touched.confirmPassword && Boolean(formik.errors.confirmPassword)}
          helperText={formik.touched.confirmPassword && formik.errors.confirmPassword}
          fullWidth
          autoComplete="new-password"
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={isPending}
          sx={{ mt: 1, py: 1.5, fontWeight: 600 }}
        >
          {isPending ? 'Resetting…' : 'Reset password'}
        </Button>
      </Stack>
    </Box>
  )
}
