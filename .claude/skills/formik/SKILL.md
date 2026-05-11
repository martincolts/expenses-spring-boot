# Formik + Yup skill

## When to use this skill

Use whenever a component needs a form with user input — login, register, create/edit records. Formik manages form state and submission; Yup handles validation schemas.

Trigger phrases: "add a form", "form with validation", "controlled inputs", "validate the fields", "show field errors", "form submission", "edit form for X".

---

## Setup

Both packages are already installed:
```
formik    — form state management
yup       — schema-based validation
```

No global provider needed. Import per file.

---

## Standard pattern — `useFormik` + Yup + MUI TextField

This is the project's standard form pattern.

```tsx
import { useFormik } from 'formik'
import * as Yup from 'yup'
import { Box, Button, Stack, TextField, Alert } from '@mui/material'

const schema = Yup.object({
  email: Yup.string().email('Invalid email').required('Required'),
  password: Yup.string().required('Required'),
})

export function LoginForm() {
  const formik = useFormik({
    initialValues: { email: '', password: '' },
    validationSchema: schema,
    onSubmit: (values) => {
      // call mutate(values) or any async action here
    },
  })

  return (
    <Box component="form" onSubmit={formik.handleSubmit} noValidate>
      <Stack spacing={2.5}>
        <TextField
          label="Email"
          type="email"
          {...formik.getFieldProps('email')}
          error={formik.touched.email && Boolean(formik.errors.email)}
          helperText={formik.touched.email && formik.errors.email}
          fullWidth
        />
        <TextField
          label="Password"
          type="password"
          {...formik.getFieldProps('password')}
          error={formik.touched.password && Boolean(formik.errors.password)}
          helperText={formik.touched.password && formik.errors.password}
          fullWidth
        />
        <Button type="submit" variant="contained" fullWidth disabled={formik.isSubmitting}>
          Sign In
        </Button>
      </Stack>
    </Box>
  )
}
```

**Key points:**
- `formik.getFieldProps('name')` spreads `value`, `onChange`, `onBlur`, and `name` onto the TextField — always use it.
- `error` is only shown when the field has been touched AND has an error — never show errors on pristine fields.
- `noValidate` on the `<form>` disables native browser validation so Yup controls everything.
- `formik.isSubmitting` is true while `onSubmit` is executing (if it returns a promise).

---

## Wiring with TanStack Query mutations

When the form submits to the backend, pass the `useMutation` result's `mutate` into `onSubmit`. Show the server error from the mutation separately (it is not a Yup error).

```tsx
const { mutate, isPending, isError, error } = useCreateExpense()

const formik = useFormik({
  initialValues: { description: '', amount: 0 },
  validationSchema: schema,
  onSubmit: (values) => mutate(values),
})

// In JSX — server error above the fields:
{isError && (
  <Alert severity="error" variant="outlined">
    {error.message}
  </Alert>
)}
```

Use `isPending` (not `formik.isSubmitting`) on the submit button when using `useMutation`, because `mutate` is fire-and-forget and doesn't return a promise to Formik.

---

## Common Yup validators

```ts
Yup.string().required('Required')
Yup.string().email('Invalid email').required('Required')
Yup.string().min(8, 'Minimum 8 characters').required('Required')
Yup.number().positive('Must be positive').required('Required')
Yup.number().min(0.01, 'Must be greater than 0')
```

---

## Showing field errors correctly

Always gate on `touched` so pristine fields are silent:

```tsx
error={formik.touched.fieldName && Boolean(formik.errors.fieldName)}
helperText={formik.touched.fieldName && formik.errors.fieldName}
```

---

## Resetting after submission

```tsx
onSubmit: (values, { resetForm }) => {
  mutate(values, { onSuccess: () => resetForm() })
}
```

---

## What NOT to do

- Do not use `<Formik>` component + `<Field>` — the `useFormik` hook is cleaner for MUI.
- Do not use `onChange` / `onBlur` handlers manually — `getFieldProps` already wires them.
- Do not write inline validation functions — define a `Yup.object` schema outside the component so it isn't recreated on every render.
- Do not suppress Yup errors by skipping `validationSchema` — put all rules in Yup, not in the `onSubmit` handler.
