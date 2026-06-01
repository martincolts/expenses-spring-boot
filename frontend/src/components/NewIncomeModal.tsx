import { useFormik } from 'formik'
import * as Yup from 'yup'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  TextField,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import { useCreateIncome } from '../hooks/useCreateIncome'
import { useUpdateIncome } from '../hooks/useUpdateIncome'
import type { Income } from '../types/expense'

const schema = Yup.object({
  amount: Yup.number().positive('Must be positive').required('Required'),
})

interface Props {
  open: boolean
  onClose: () => void
  income?: Income
}

export function NewIncomeModal({ open, onClose, income }: Props) {
  const isEdit = income != null
  const { mutate: create, isPending: createPending, isError: createError, error: createErr, reset: resetCreate } = useCreateIncome()
  const { mutate: update, isPending: updatePending, isError: updateError, error: updateErr, reset: resetUpdate } = useUpdateIncome()

  const isPending = createPending || updatePending
  const isError = createError || updateError
  const error = createErr ?? updateErr

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: { amount: income?.amount ?? '' },
    validationSchema: schema,
    onSubmit: (values, { resetForm }) => {
      const body = { amount: parseFloat(values.amount as string) }
      if (isEdit) {
        update(
          { id: income.id, body },
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
        {isEdit ? 'Edit Income' : 'New Income'}
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
              label="Amount"
              type="number"
              inputProps={{ min: 0, step: '0.01' }}
              fullWidth
              {...formik.getFieldProps('amount')}
              error={formik.touched.amount && Boolean(formik.errors.amount)}
              helperText={formik.touched.amount && formik.errors.amount}
            />

            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={isPending}
              sx={{ py: 1.5 }}
            >
              {isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Income'}
            </Button>
          </Stack>
        </Box>
      </DialogContent>
    </Dialog>
  )
}
