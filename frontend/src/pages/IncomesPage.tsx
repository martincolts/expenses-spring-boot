import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Container,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { fetchIncomes } from '../api/incomes'
import type { Income } from '../types/expense'
import { NewIncomeModal } from '../components/NewIncomeModal'
import { useDeleteIncome } from '../hooks/useDeleteIncome'

function formatAmount(amount: string) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(parseFloat(amount))
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function IncomeCard({
  income,
  onDelete,
  onClick,
}: {
  income: Income
  onDelete: (id: number) => void
  onClick: (income: Income) => void
}) {
  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
      <CardActionArea onClick={() => onClick(income)} sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="caption" color="text.secondary">
              {formatDate(income.createdAt)}
            </Typography>
            <Stack direction="row" sx={{ alignItems: 'center' }} spacing={1}>
              <Typography variant="body1" sx={{ fontWeight: 600 }} color="success.main">
                {formatAmount(income.amount)}
              </Typography>
              <IconButton
                size="small"
                color="error"
                onClick={e => { e.stopPropagation(); onDelete(income.id) }}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Stack>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}

export function IncomesPage() {
  const [selected, setSelected] = useState<Income | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const { data: incomes = [], isLoading, isError } = useQuery({
    queryKey: ['incomes'],
    queryFn: fetchIncomes,
  })
  const { mutate: deleteIncome } = useDeleteIncome()

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
            Incomes
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

        <NewIncomeModal open={createOpen} onClose={() => setCreateOpen(false)} />
        <NewIncomeModal
          open={selected != null}
          onClose={() => setSelected(null)}
          income={selected ?? undefined}
        />

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {isError && (
          <Typography color="error">Failed to load incomes.</Typography>
        )}

        {!isLoading && !isError && incomes.length === 0 && (
          <Box sx={{ py: 8, textAlign: 'center' }}>
            <Typography color="text.secondary">No incomes yet.</Typography>
          </Box>
        )}

        {/* Mobile card list */}
        {!isLoading && incomes.length > 0 && (
          <Box sx={{ display: { xs: 'block', md: 'none' } }}>
            <Stack spacing={1.5}>
              {incomes.map(i => (
                <IncomeCard key={i.id} income={i} onDelete={deleteIncome} onClick={setSelected} />
              ))}
            </Stack>
          </Box>
        )}

        {/* Desktop table */}
        {!isLoading && incomes.length > 0 && (
          <Box sx={{ display: { xs: 'none', md: 'block' } }}>
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {incomes.map(i => (
                    <TableRow
                      key={i.id}
                      hover
                      onClick={() => setSelected(i)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{formatDate(i.createdAt)}</TableCell>
                      <TableCell align="right" sx={{ color: 'success.main', fontWeight: 600 }}>
                        {formatAmount(i.amount)}
                      </TableCell>
                      <TableCell align="right">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={e => { e.stopPropagation(); deleteIncome(i.id) }}
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
