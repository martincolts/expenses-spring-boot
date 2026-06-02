import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Grid,
  Stack,
  Typography,
} from '@mui/material'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts'
import { fetchExpenses } from '../api/expenses'
import { fetchIncomes } from '../api/incomes'
import type { Expense } from '../types/expense'

const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const PIE_COLORS = ['#1976d2', '#42a5f5']

interface SummaryCardProps {
  label: string
  value: string
  icon: React.ReactNode
  color: string
}

function SummaryCard({ label, value, icon, color }: SummaryCardProps) {
  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
      <CardContent sx={{ p: { xs: 2, md: 3 } }}>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {label}
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
              {value}
            </Typography>
          </Box>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2,
              bgcolor: `${color}.50`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: `${color}.main`,
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

function buildMonthlyData(expenses: Expense[]) {
  const now = new Date()
  const months: { month: string; amount: number }[] = []

  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({ month: MONTH_LABELS[d.getMonth()], amount: 0 })
  }

  expenses.forEach(e => {
    const d = new Date(e.incurredAt)
    const diffMonths = (now.getFullYear() - d.getFullYear()) * 12 + (now.getMonth() - d.getMonth())
    if (diffMonths >= 0 && diffMonths < 6) {
      months[5 - diffMonths].amount += parseFloat(e.amount)
    }
  })

  return months.map(m => ({ ...m, amount: parseFloat(m.amount.toFixed(2)) }))
}

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount)
}

export function HomePage() {
  const { data: expensesPage, isLoading: loadingExpenses } = useQuery({
    queryKey: ['expenses', { page: 0, rowsPerPage: 100 }],
    queryFn: () => fetchExpenses(0, 100),
  })

  const { data: incomesPage, isLoading: loadingIncomes } = useQuery({
    queryKey: ['incomes', { page: 0, rowsPerPage: 100 }],
    queryFn: () => fetchIncomes(0, 100),
  })

  const expenses = expensesPage?.content ?? []
  const incomes = incomesPage?.content ?? []

  const totalExpenses = useMemo(
    () => expenses.reduce((sum, e) => sum + parseFloat(e.amount), 0),
    [expenses],
  )

  const totalIncome = useMemo(
    () => incomes.reduce((sum, i) => sum + parseFloat(i.amount), 0),
    [incomes],
  )

  const monthlyData = useMemo(() => buildMonthlyData(expenses), [expenses])

  const statusData = useMemo(() => {
    const paid = expenses.filter(e => e.status === 'PAID').reduce((s, e) => s + parseFloat(e.amount), 0)
    const pending = expenses.filter(e => e.status === 'PENDING').reduce((s, e) => s + parseFloat(e.amount), 0)
    return [
      { name: 'Paid', value: parseFloat(paid.toFixed(2)) },
      { name: 'Pending', value: parseFloat(pending.toFixed(2)) },
    ].filter(d => d.value > 0)
  }, [expenses])

  const isLoading = loadingExpenses || loadingIncomes

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 3, md: 4 }}>
        <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
          Dashboard
        </Typography>

        {/* Summary cards */}
        <Grid container spacing={{ xs: 2, md: 3 }}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <SummaryCard
              label="Total Expenses"
              value={formatCurrency(totalExpenses)}
              icon={<TrendingDownIcon />}
              color="error"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <SummaryCard
              label="Total Income"
              value={formatCurrency(totalIncome)}
              icon={<TrendingUpIcon />}
              color="success"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <SummaryCard
              label="Balance"
              value={formatCurrency(totalIncome - totalExpenses)}
              icon={<AccountBalanceWalletIcon />}
              color={totalIncome - totalExpenses >= 0 ? 'primary' : 'warning'}
            />
          </Grid>
        </Grid>

        {/* Charts row */}
        <Grid container spacing={{ xs: 2, md: 3 }}>
          {/* Monthly bar chart */}
          <Grid size={{ xs: 12, md: 8 }}>
            <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
              <CardContent sx={{ p: { xs: 2, md: 3 } }}>
                <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 600 }}>
                  Expenses — Last 6 Months
                </Typography>
                {monthlyData.every(m => m.amount === 0) ? (
                  <Box sx={{ py: 6, textAlign: 'center' }}>
                    <Typography color="text.secondary">No expense data yet</Typography>
                  </Box>
                ) : (
                  <Box sx={{ height: { xs: 220, md: 280 }, mt: 1 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={monthlyData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                        <XAxis dataKey="month" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                        <YAxis tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                        <Tooltip
                          formatter={(v) => [formatCurrency(Number(v)), 'Expenses']}
                          contentStyle={{ borderRadius: 8, border: '1px solid #e0e0e0' }}
                        />
                        <Bar dataKey="amount" fill="#1976d2" radius={[6, 6, 0, 0]} maxBarSize={48} />
                      </BarChart>
                    </ResponsiveContainer>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Status pie chart */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, height: '100%' }}>
              <CardContent sx={{ p: { xs: 2, md: 3 } }}>
                <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 600 }}>
                  Expense Status
                </Typography>
                {statusData.length === 0 ? (
                  <Box sx={{ py: 6, textAlign: 'center' }}>
                    <Typography color="text.secondary">No data yet</Typography>
                  </Box>
                ) : (
                  <Box sx={{ height: { xs: 220, md: 280 }, mt: 1 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={statusData}
                          cx="50%"
                          cy="45%"
                          innerRadius="45%"
                          outerRadius="70%"
                          dataKey="value"
                          paddingAngle={3}
                        >
                          {statusData.map((_, i) => (
                            <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                          ))}
                        </Pie>
                        <Legend
                          formatter={(v, entry) =>
                            `${v}: ${formatCurrency((entry.payload as { value: number }).value)}`
                          }
                        />
                        <Tooltip
                          formatter={(v) => [formatCurrency(Number(v))]}
                          contentStyle={{ borderRadius: 8, border: '1px solid #e0e0e0' }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Stack>
    </Container>
  )
}
