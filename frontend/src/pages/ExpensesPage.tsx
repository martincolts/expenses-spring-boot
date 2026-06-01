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
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { fetchExpenses } from '../api/expenses'
import type { Expense } from '../types/expense'
import { NewExpenseModal } from '../components/NewExpenseModal'
import { useDeleteExpense } from '../hooks/useDeleteExpense'

function formatAmount(amount: string, currency: string) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(parseFloat(amount))
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function ExpenseCard({
  expense,
  onDelete,
  onClick,
}: {
  expense: Expense
  onDelete: (id: number) => void
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
                <IconButton
                  size="small"
                  color="error"
                  onClick={e => { e.stopPropagation(); onDelete(expense.id) }}
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

export function ExpensesPage() {
  const [selected, setSelected] = useState<Expense | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const { data: expenses = [], isLoading, isError } = useQuery({
    queryKey: ['expenses'],
    queryFn: fetchExpenses,
  })
  const { mutate: deleteExpense } = useDeleteExpense()

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
            Expenses
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateOpen(true)}
            size="medium"
          >
            Add
          </Button>
        </Stack>

        <NewExpenseModal open={createOpen} onClose={() => setCreateOpen(false)} />
        <NewExpenseModal
          open={selected != null}
          onClose={() => setSelected(null)}
          expense={selected ?? undefined}
        />

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {isError && (
          <Typography color="error">Failed to load expenses.</Typography>
        )}

        {!isLoading && !isError && expenses.length === 0 && (
          <Box sx={{ py: 8, textAlign: 'center' }}>
            <Typography color="text.secondary">No expenses yet.</Typography>
          </Box>
        )}

        {/* Mobile card list */}
        {!isLoading && expenses.length > 0 && (
          <Box sx={{ display: { xs: 'block', md: 'none' } }}>
            <Stack spacing={1.5}>
              {expenses.map(e => (
                <ExpenseCard key={e.id} expense={e} onDelete={deleteExpense} onClick={setSelected} />
              ))}
            </Stack>
          </Box>
        )}

        {/* Desktop table */}
        {!isLoading && expenses.length > 0 && (
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
                        <IconButton
                          size="small"
                          color="error"
                          onClick={e2 => { e2.stopPropagation(); deleteExpense(e.id) }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Stack>
    </Container>
  )
}
