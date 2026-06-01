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
  Tooltip,
  Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import CategoryIcon from '@mui/icons-material/Category'
import { useCreateCategory } from '../hooks/useCreateCategory'
import { useUpdateCategory } from '../hooks/useUpdateCategory'
import type { Category } from '../types/category'
import { CATEGORY_ICONS, getCategoryIcon } from '../constants/categoryIcons'
import { CATEGORY_COLORS, getCategoryColor } from '../constants/categoryColors'

const schema = Yup.object({
  name: Yup.string().max(50, 'Max 50 characters').required('Required'),
  iconId: Yup.string().optional(),
  colorId: Yup.string().optional(),
})

interface Props {
  open: boolean
  onClose: () => void
  category?: Category
}

export function NewCategoryModal({ open, onClose, category }: Props) {
  const isEdit = category != null
  const { mutate: create, isPending: createPending, isError: createError, error: createErr, reset: resetCreate } = useCreateCategory()
  const { mutate: update, isPending: updatePending, isError: updateError, error: updateErr, reset: resetUpdate } = useUpdateCategory()

  const isPending = createPending || updatePending
  const isError = createError || updateError
  const error = createErr ?? updateErr

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      name: category?.name ?? '',
      iconId: category?.iconId ?? '',
      colorId: category?.colorId ?? '',
    },
    validationSchema: schema,
    onSubmit: (values, { resetForm }) => {
      const body = {
        name: values.name,
        iconId: values.iconId || undefined,
        colorId: values.colorId || undefined,
      }
      if (isEdit) {
        update(
          { id: category.id, body },
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

  const selectedColor = getCategoryColor(formik.values.colorId)
  const SelectedIcon = getCategoryIcon(formik.values.iconId)

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="sm"
      slotProps={{ paper: { sx: { borderRadius: 3 } } }}
    >
      <DialogTitle sx={{ pr: 6 }}>
        {isEdit ? 'Edit Category' : 'New Category'}
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
          <Stack spacing={3}>
            {isError && (
              <Alert severity="error" variant="outlined">
                {error?.message}
              </Alert>
            )}

            {/* Preview */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box
                sx={{
                  width: 48,
                  height: 48,
                  borderRadius: 2,
                  bgcolor: selectedColor,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {SelectedIcon
                  ? <SelectedIcon sx={{ color: '#fff', fontSize: 26 }} />
                  : <CategoryIcon sx={{ color: '#fff', fontSize: 26 }} />
                }
              </Box>
              <TextField
                label="Name"
                fullWidth
                autoFocus={!isEdit}
                {...formik.getFieldProps('name')}
                error={formik.touched.name && Boolean(formik.errors.name)}
                helperText={formik.touched.name && formik.errors.name}
              />
            </Box>

            {/* Color picker */}
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                Color
              </Typography>
              <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                {CATEGORY_COLORS.map(c => (
                  <Tooltip key={c.id} title={c.label}>
                    <Box
                      onClick={() => formik.setFieldValue('colorId', formik.values.colorId === c.id ? '' : c.id)}
                      sx={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        bgcolor: c.value,
                        cursor: 'pointer',
                        border: formik.values.colorId === c.id ? '3px solid' : '2px solid transparent',
                        borderColor: formik.values.colorId === c.id ? 'text.primary' : 'transparent',
                        outline: '2px solid',
                        outlineColor: formik.values.colorId === c.id ? c.value : 'transparent',
                        outlineOffset: '2px',
                        transition: 'outline-color 0.15s, border-color 0.15s',
                      }}
                    />
                  </Tooltip>
                ))}
              </Stack>
            </Box>

            {/* Icon picker */}
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                Icon
              </Typography>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(44px, 1fr))',
                  gap: 0.75,
                }}
              >
                {CATEGORY_ICONS.map(({ id, Icon, label }) => {
                  const selected = formik.values.iconId === id
                  return (
                    <Tooltip key={id} title={label}>
                      <Box
                        onClick={() => formik.setFieldValue('iconId', selected ? '' : id)}
                        sx={{
                          width: 44,
                          height: 44,
                          borderRadius: 2,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          bgcolor: selected ? selectedColor : 'action.hover',
                          color: selected ? '#fff' : 'text.secondary',
                          border: '2px solid',
                          borderColor: selected ? selectedColor : 'transparent',
                          transition: 'background-color 0.15s, color 0.15s',
                          '&:hover': {
                            bgcolor: selected ? selectedColor : 'action.selected',
                          },
                        }}
                      >
                        <Icon fontSize="small" />
                      </Box>
                    </Tooltip>
                  )
                })}
              </Box>
            </Box>

            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={isPending}
              sx={{ py: 1.5 }}
            >
              {isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Category'}
            </Button>
          </Stack>
        </Box>
      </DialogContent>
    </Dialog>
  )
}
