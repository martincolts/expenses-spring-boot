import { useFormik } from 'formik'
import * as Yup from 'yup'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  MenuItem,
  Stack,
  Switch,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import { useCreateExpense } from '../hooks/useCreateExpense'
import { useUpdateExpense } from '../hooks/useUpdateExpense'
import { useCategories } from '../hooks/useCategories'
import type { Expense } from '../types/expense'

const schema = Yup.object({
  description: Yup.string().max(255, 'Max 255 characters').required('Required'),
  amount: Yup.number().positive('Must be positive').required('Required'),
  currency: Yup.string().length(3, 'Must be 3 characters').required('Required'),
  incurredAt: Yup.string().required('Required'),
  paid: Yup.boolean(),
  categoryId: Yup.number().nullable(),
})

const CURRENCIES = ['USD', 'EUR', 'GBP', 'ARS', 'BRL']

interface Props {
  open: boolean
  onClose: () => void
  expense?: Expense
}

export function NewExpenseModal({ open, onClose, expense }: Props) {
  const isEdit = expense != null
  const { mutate: create, isPending: createPending, isError: createError, error: createErr, reset: resetCreate } = useCreateExpense()
  const { mutate: update, isPending: updatePending, isError: updateError, error: updateErr, reset: resetUpdate } = useUpdateExpense()

  const isPending = createPending || updatePending
  const isError = createError || updateError
  const error = createErr ?? updateErr

  const { data: categories = [] } = useCategories()

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      description: expense?.description ?? '',
      amount: expense?.amount ?? '',
      currency: expense?.currency ?? 'USD',
      incurredAt: expense?.incurredAt
        ? expense.incurredAt.slice(0, 10)
        : new Date().toISOString().slice(0, 10),
      paid: expense?.status === 'PAID',
      categoryId: expense?.categoryId ?? ('' as number | ''),
    },
    validationSchema: schema,
    onSubmit: (values, { resetForm }) => {
      const body = {
        categoryId: values.categoryId === '' ? null : values.categoryId,
        description: values.description,
        amount: parseFloat(values.amount as string),
        currency: values.currency,
        incurredAt: new Date(values.incurredAt).toISOString(),
        paid: values.paid,
      }
      if (isEdit) {
        update(
          { id: expense.id, body },
          {
            onSuccess: () => {
              resetForm()
              resetUpdate()
              onClose()
            },
          },
        )
      } else {
        create(body, {
          onSuccess: () => {
            resetForm()
            resetCreate()
            onClose()
          },
        })
      }
    },
  })

  function handleClose() {
    formik.resetForm()
    resetCreate()
    resetUpdate()
    onClose()
  }

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="sm"
      PaperProps={{ sx: { borderRadius: 3 } }}
    >
      <DialogTitle sx={{ pr: 6 }}>
        {isEdit ? 'Edit Expense' : 'New Expense'}
        <IconButton
          onClick={handleClose}
          size="small"
          sx={{ position: 'absolute', right: 12, top: 12 }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent>
        <Box component="form" onSubmit={formik.handleSubmit} noValidate sx={{ pt: 1 }}>
          <Stack spacing={2.5}>
            {isError && (
              <Alert severity="error" variant="outlined">
                {error?.message}
              </Alert>
            )}

            <TextField
              label="Description"
              fullWidth
              {...formik.getFieldProps('description')}
              error={formik.touched.description && Boolean(formik.errors.description)}
              helperText={formik.touched.description && formik.errors.description}
            />

            <TextField
              label="Category"
              select
              fullWidth
              {...formik.getFieldProps('categoryId')}
            >
              <MenuItem value="">
                <em>None</em>
              </MenuItem>
              {categories.map(c => (
                <MenuItem key={c.id} value={c.id}>
                  {c.name}
                </MenuItem>
              ))}
            </TextField>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Amount"
                type="number"
                inputProps={{ min: 0, step: '0.01' }}
                fullWidth
                {...formik.getFieldProps('amount')}
                error={formik.touched.amount && Boolean(formik.errors.amount)}
                helperText={formik.touched.amount && formik.errors.amount}
              />

              <TextField
                label="Currency"
                select
                fullWidth
                {...formik.getFieldProps('currency')}
                error={formik.touched.currency && Boolean(formik.errors.currency)}
                helperText={formik.touched.currency && formik.errors.currency}
              >
                {CURRENCIES.map(c => (
                  <MenuItem key={c} value={c}>
                    {c}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>

            <TextField
              label="Date"
              type="date"
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              {...formik.getFieldProps('incurredAt')}
              error={formik.touched.incurredAt && Boolean(formik.errors.incurredAt)}
              helperText={formik.touched.incurredAt && formik.errors.incurredAt}
            />

            <FormControlLabel
              control={
                <Switch
                  checked={formik.values.paid}
                  onChange={e => formik.setFieldValue('paid', e.target.checked)}
                />
              }
              label="Mark as paid"
            />

            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={isPending}
              sx={{ py: 1.5 }}
            >
              {isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Expense'}
            </Button>
          </Stack>
        </Box>
      </DialogContent>
    </Dialog>
  )
}
