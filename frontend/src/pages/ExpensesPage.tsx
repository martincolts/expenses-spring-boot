import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  CardActionArea,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import FilterListIcon from '@mui/icons-material/FilterList'
import ClearIcon from '@mui/icons-material/Clear'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import { fetchExpenses, type ExpenseFilters } from '../api/expenses'
import type { Expense } from '../types/expense'
import { NewExpenseModal } from '../components/NewExpenseModal'
import { useDeleteExpense } from '../hooks/useDeleteExpense'
import { useMarkExpensePaid } from '../hooks/useMarkExpensePaid'
import { useMarkExpenseUnpaid } from '../hooks/useMarkExpenseUnpaid'
import { useCategories } from '../hooks/useCategories'

function formatAmount(amount: string, currency: string) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(parseFloat(amount))
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' })
}

function ExpenseCard({
  expense,
  onDelete,
  onMarkPaid,
  onMarkPending,
  onClick,
}: {
  expense: Expense
  onDelete: (expense: Expense) => void
  onMarkPaid: (expense: Expense) => void
  onMarkPending: (expense: Expense) => void
  onClick: (expense: Expense) => void
}) {
  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
      <CardActionArea onClick={() => onClick(expense)} sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <Box sx={{ minWidth: 0, flexGrow: 1, mr: 1 }}>
              <Typography variant="body1" sx={{ fontWeight: 500 }} noWrap>
                {expense.description}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {formatDate(expense.incurredAt)}
              </Typography>
            </Box>
            <Stack sx={{ alignItems: 'flex-end' }} spacing={0.5}>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {formatAmount(expense.amount, expense.currency)}
              </Typography>
              <Stack direction="row" sx={{ alignItems: 'center' }} spacing={0.5}>
                <Chip
                  label={expense.status}
                  size="small"
                  color={expense.status === 'PAID' ? 'success' : 'warning'}
                  variant="outlined"
                />
                {expense.status === 'PENDING' && (
                  <Tooltip title="Mark as paid">
                    <IconButton
                      size="small"
                      color="success"
                      onClick={e => { e.stopPropagation(); onMarkPaid(expense) }}
                    >
                      <CheckCircleOutlineIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
                {expense.status === 'PAID' && (
                  <Tooltip title="Mark as pending">
                    <IconButton
                      size="small"
                      color="warning"
                      onClick={e => { e.stopPropagation(); onMarkPending(expense) }}
                    >
                      <RadioButtonUncheckedIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
                <IconButton
                  size="small"
                  color="error"
                  onClick={e => { e.stopPropagation(); onDelete(expense) }}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            </Stack>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}

const EMPTY_FILTERS: ExpenseFilters = {
  categoryId: null,
  status: null,
  dateFrom: null,
  dateTo: null,
}

function hasActiveFilters(f: ExpenseFilters) {
  return f.categoryId != null || f.status != null || f.dateFrom != null || f.dateTo != null
}

export function ExpensesPage() {
  const [selected, setSelected] = useState<Expense | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(20)
  const [filters, setFilters] = useState<ExpenseFilters>(EMPTY_FILTERS)
  const [showFilters, setShowFilters] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<Expense | null>(null)
  const [payTarget, setPayTarget] = useState<Expense | null>(null)
  const [unpayTarget, setUnpayTarget] = useState<Expense | null>(null)

  const { data: categoriesData } = useCategories()
  const categories = categoriesData ?? []

  const { data, isPending, isError } = useQuery({
    queryKey: ['expenses', { page, rowsPerPage, ...filters }],
    queryFn: () => fetchExpenses(page, rowsPerPage, filters),
  })

  const expenses = data?.content ?? []
  const totalElements = data?.totalElements ?? 0

  const { mutate: deleteExpense, isPending: isDeleting } = useDeleteExpense()
  const { mutate: markPaid, isPending: isMarkingPaid } = useMarkExpensePaid()
  const { mutate: markPending, isPending: isMarkingPending } = useMarkExpenseUnpaid()

  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(e.target.value))
    setPage(0)
  }

  function setFilter<K extends keyof ExpenseFilters>(key: K, value: ExpenseFilters[K]) {
    setFilters(prev => ({ ...prev, [key]: value || null }))
    setPage(0)
  }

  function clearFilters() {
    setFilters(EMPTY_FILTERS)
    setPage(0)
  }

  function confirmDelete() {
    if (!deleteTarget) return
    deleteExpense(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  function confirmMarkPaid() {
    if (!payTarget) return
    markPaid(payTarget.id, { onSuccess: () => setPayTarget(null) })
  }

  function confirmMarkPending() {
    if (!unpayTarget) return
    markPending(unpayTarget.id, { onSuccess: () => setUnpayTarget(null) })
  }

  const paginationProps = {
    count: totalElements,
    page,
    onPageChange: (_: unknown, newPage: number) => setPage(newPage),
    rowsPerPage,
    onRowsPerPageChange: handleRowsPerPageChange,
    rowsPerPageOptions: [10, 20, 50],
  }

  const active = hasActiveFilters(filters)

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
            Expenses
          </Typography>
          <Stack direction="row" spacing={1}>
            <Button
              variant={active ? 'contained' : 'outlined'}
              startIcon={<FilterListIcon />}
              onClick={() => setShowFilters(v => !v)}
              size="medium"
              color={active ? 'primary' : 'inherit'}
            >
              Filter{active ? 's on' : ''}
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCreateOpen(true)}
              size="medium"
            >
              Add
            </Button>
          </Stack>
        </Stack>

        {showFilters && (
          <Paper
            elevation={0}
            sx={{ p: { xs: 2, md: 2.5 }, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}
          >
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              sx={{ flexWrap: 'wrap', alignItems: { sm: 'center' } }}
            >
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Category</InputLabel>
                <Select
                  label="Category"
                  value={filters.categoryId ?? ''}
                  onChange={e => setFilter('categoryId', e.target.value ? Number(e.target.value) : null)}
                >
                  <MenuItem value="">All categories</MenuItem>
                  {categories.map(c => (
                    <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  label="Status"
                  value={filters.status ?? ''}
                  onChange={e => setFilter('status', (e.target.value as ExpenseFilters['status']) || null)}
                >
                  <MenuItem value="">All statuses</MenuItem>
                  <MenuItem value="PENDING">Pending</MenuItem>
                  <MenuItem value="PAID">Paid</MenuItem>
                </Select>
              </FormControl>

              <TextField
                label="From"
                type="date"
                size="small"
                slotProps={{ inputLabel: { shrink: true } }}
                value={filters.dateFrom ?? ''}
                onChange={e => setFilter('dateFrom', e.target.value || null)}
                sx={{ minWidth: 150 }}
              />

              <TextField
                label="To"
                type="date"
                size="small"
                slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: filters.dateFrom ?? undefined } }}
                value={filters.dateTo ?? ''}
                onChange={e => setFilter('dateTo', e.target.value || null)}
                sx={{ minWidth: 150 }}
              />

              {active && (
                <Button
                  size="small"
                  startIcon={<ClearIcon />}
                  onClick={clearFilters}
                  color="inherit"
                >
                  Clear
                </Button>
              )}
            </Stack>
          </Paper>
        )}

        <NewExpenseModal open={createOpen} onClose={() => setCreateOpen(false)} />
        <NewExpenseModal
          open={selected != null}
          onClose={() => setSelected(null)}
          expense={selected ?? undefined}
        />

        {/* Delete confirmation dialog */}
        <Dialog open={deleteTarget != null} onClose={() => setDeleteTarget(null)}>
          <DialogTitle>Delete expense?</DialogTitle>
          <DialogContent>
            <DialogContentText>
              {deleteTarget && (
                <>
                  Are you sure you want to delete <strong>{deleteTarget.description}</strong> ({formatAmount(deleteTarget.amount, deleteTarget.currency)})?
                  This action cannot be undone.
                </>
              )}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteTarget(null)} disabled={isDeleting}>Cancel</Button>
            <Button onClick={confirmDelete} color="error" variant="contained" loading={isDeleting}>
              Delete
            </Button>
          </DialogActions>
        </Dialog>

        {/* Mark as paid confirmation dialog */}
        <Dialog open={payTarget != null} onClose={() => setPayTarget(null)}>
          <DialogTitle>Mark as paid?</DialogTitle>
          <DialogContent>
            <DialogContentText>
              {payTarget && (
                <>
                  Mark <strong>{payTarget.description}</strong> ({formatAmount(payTarget.amount, payTarget.currency)}) as paid?
                </>
              )}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPayTarget(null)} disabled={isMarkingPaid}>Cancel</Button>
            <Button onClick={confirmMarkPaid} color="success" variant="contained" loading={isMarkingPaid}>
              Mark as paid
            </Button>
          </DialogActions>
        </Dialog>

        {/* Mark as pending confirmation dialog */}
        <Dialog open={unpayTarget != null} onClose={() => setUnpayTarget(null)}>
          <DialogTitle>Mark as pending?</DialogTitle>
          <DialogContent>
            <DialogContentText>
              {unpayTarget && (
                <>
                  Move <strong>{unpayTarget.description}</strong> ({formatAmount(unpayTarget.amount, unpayTarget.currency)}) back to pending?
                </>
              )}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setUnpayTarget(null)} disabled={isMarkingPending}>Cancel</Button>
            <Button onClick={confirmMarkPending} color="warning" variant="contained" loading={isMarkingPending}>
              Mark as pending
            </Button>
          </DialogActions>
        </Dialog>

        {isPending && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {isError && (
          <Typography color="error">Failed to load expenses.</Typography>
        )}

        {!isPending && !isError && expenses.length === 0 && (
          <Box sx={{ py: 8, textAlign: 'center' }}>
            <Typography color="text.secondary">
              {active ? 'No expenses match the current filters.' : 'No expenses yet.'}
            </Typography>
          </Box>
        )}

        {/* Mobile card list */}
        {!isPending && expenses.length > 0 && (
          <Box sx={{ display: { xs: 'block', md: 'none' } }}>
            <Stack spacing={1.5}>
              {expenses.map(e => (
                <ExpenseCard
                  key={e.id}
                  expense={e}
                  onDelete={setDeleteTarget}
                  onMarkPaid={setPayTarget}
                  onMarkPending={setUnpayTarget}
                  onClick={setSelected}
                />
              ))}
            </Stack>
            <TablePagination component="div" {...paginationProps} sx={{ mt: 1 }} />
          </Box>
        )}

        {/* Desktop table */}
        {!isPending && expenses.length > 0 && (
          <Box sx={{ display: { xs: 'none', md: 'block' } }}>
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Description</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {expenses.map(e => (
                    <TableRow
                      key={e.id}
                      hover
                      onClick={() => setSelected(e)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{e.description}</TableCell>
                      <TableCell>{formatDate(e.incurredAt)}</TableCell>
                      <TableCell align="right">{formatAmount(e.amount, e.currency)}</TableCell>
                      <TableCell>
                        <Chip
                          label={e.status}
                          size="small"
                          color={e.status === 'PAID' ? 'success' : 'warning'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end" spacing={0.5}>
                          {e.status === 'PENDING' && (
                            <Tooltip title="Mark as paid">
                              <IconButton
                                size="small"
                                color="success"
                                onClick={e2 => { e2.stopPropagation(); setPayTarget(e) }}
                              >
                                <CheckCircleOutlineIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {e.status === 'PAID' && (
                            <Tooltip title="Mark as pending">
                              <IconButton
                                size="small"
                                color="warning"
                                onClick={e2 => { e2.stopPropagation(); setUnpayTarget(e) }}
                              >
                                <RadioButtonUncheckedIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          <IconButton
                            size="small"
                            color="error"
                            onClick={e2 => { e2.stopPropagation(); setDeleteTarget(e) }}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <TablePagination component="div" {...paginationProps} />
            </TableContainer>
          </Box>
        )}
      </Stack>
    </Container>
  )
}
