import { useFormik } from 'formik'
import * as Yup from 'yup'
import { Alert, Box, Button, Stack, TextField } from '@mui/material'
import { useLogin } from '../hooks/useAuth'

const schema = Yup.object({
  email: Yup.string().email('Invalid email').required('Required'),
  password: Yup.string().required('Required'),
})

export function LoginForm() {
  const { mutate, isPending, isError, error } = useLogin()

  const formik = useFormik({
    initialValues: { email: '', password: '' },
    validationSchema: schema,
    onSubmit: (values) => mutate(values),
  })

  return (
    <Box component="form" onSubmit={formik.handleSubmit} noValidate>
      <Stack spacing={2.5}>
        {isError && (
          <Alert severity="error" variant="outlined">
            {error.message}
          </Alert>
        )}
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
        <TextField
          label="Password"
          type="password"
          {...formik.getFieldProps('password')}
          error={formik.touched.password && Boolean(formik.errors.password)}
          helperText={formik.touched.password && formik.errors.password}
          fullWidth
          autoComplete="current-password"
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={isPending}
          sx={{ mt: 1, py: 1.5, fontWeight: 600 }}
        >
          {isPending ? 'Signing in…' : 'Sign In'}
        </Button>
      </Stack>
    </Box>
  )
}
