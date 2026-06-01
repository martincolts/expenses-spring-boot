import { useFormik } from 'formik'
import * as Yup from 'yup'
import { Alert, Box, Button, Stack, TextField } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useRegister } from '../hooks/useAuth'
import { useAuthContext } from '../contexts/AuthContext'

const schema = Yup.object({
  name: Yup.string().required('Required'),
  email: Yup.string().email('Invalid email').required('Required'),
  password: Yup.string().min(8, 'Minimum 8 characters').required('Required'),
})

export function RegisterForm() {
  const { mutate, isPending, isError, error } = useRegister()
  const { login } = useAuthContext()
  const navigate = useNavigate()

  const formik = useFormik({
    initialValues: { name: '', email: '', password: '' },
    validationSchema: schema,
    onSubmit: (values) =>
      mutate(values, {
        onSuccess: (data) => {
          login(data)
          navigate('/')
        },
      }),
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
          label="Full Name"
          {...formik.getFieldProps('name')}
          error={formik.touched.name && Boolean(formik.errors.name)}
          helperText={formik.touched.name && formik.errors.name}
          fullWidth
          autoComplete="name"
          autoFocus
        />
        <TextField
          label="Email"
          type="email"
          {...formik.getFieldProps('email')}
          error={formik.touched.email && Boolean(formik.errors.email)}
          helperText={formik.touched.email && formik.errors.email}
          fullWidth
          autoComplete="email"
        />
        <TextField
          label="Password"
          type="password"
          {...formik.getFieldProps('password')}
          error={formik.touched.password && Boolean(formik.errors.password)}
          helperText={formik.touched.password && formik.errors.password}
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
          {isPending ? 'Creating account…' : 'Create Account'}
        </Button>
      </Stack>
    </Box>
  )
}
