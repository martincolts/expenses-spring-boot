import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Grid,
  IconButton,
  Slider,
  Stack,
  Typography,
} from '@mui/material'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import TodayIcon from '@mui/icons-material/Today'
import ScienceIcon from '@mui/icons-material/Science'
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
import { fetchDashboard } from '../api/dashboard'
import { getCategoryColor } from '../constants/categoryColors'

const MONTH_LABELS = ['January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December']

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount)
}

interface SummaryCardProps {
  label: string
  value: string
  subtitle?: string
  icon: React.ReactNode
  color: string
}

function SummaryCard({ label, value, subtitle, icon, color }: SummaryCardProps) {
  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, height: '100%' }}>
      <CardContent sx={{ p: { xs: 2, md: 3 } }}>
        <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {label}
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
              {value}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                {subtitle}
              </Typography>
            )}
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
              flexShrink: 0,
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

interface BudgetPlannerCardProps {
  balance: number
  actualDaysLeft: number
}

function BudgetPlannerCard({ balance, actualDaysLeft }: BudgetPlannerCardProps) {
  const [planDays, setPlanDays] = useState<number>(() => {
    const saved = localStorage.getItem('planningDays')
    if (saved) {
      const n = parseInt(saved, 10)
      if (n >= 1) return n
    }
    return actualDaysLeft
  })

  useEffect(() => {
    if (actualDaysLeft > 0 && planDays > actualDaysLeft) setPlanDays(actualDaysLeft)
  }, [actualDaysLeft])

  useEffect(() => {
    localStorage.setItem('planningDays', String(planDays))
  }, [planDays])

  const effectiveDays = Math.min(planDays, actualDaysLeft)
  const dailyBudget = effectiveDays > 0 ? balance / effectiveDays : 0
  const isPositive = dailyBudget >= 0

  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, height: '100%' }}>
      <CardContent sx={{ p: { xs: 2, md: 3 } }}>
        <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Budget Planner
            </Typography>
            <Typography
              variant="h5"
              sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' }, color: isPositive ? 'success.main' : 'error.main' }}
            >
              {formatCurrency(dailyBudget)}/day
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              if you spread over {effectiveDays} day{effectiveDays !== 1 ? 's' : ''} (of {actualDaysLeft} remaining)
            </Typography>
          </Box>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2,
              bgcolor: 'secondary.50',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'secondary.main',
              flexShrink: 0,
            }}
          >
            <ScienceIcon />
          </Box>
        </Stack>
        <Stack direction="row" sx={{ alignItems: 'center', gap: 2 }}>
          <Typography variant="caption" color="text.secondary" sx={{ minWidth: 16 }}>1</Typography>
          <Slider
            value={effectiveDays}
            min={1}
            max={actualDaysLeft}
            step={1}
            onChange={(_, v) => setPlanDays(v as number)}
            size="small"
            sx={{ flex: 1 }}
          />
          <Typography variant="caption" color="text.secondary" sx={{ minWidth: 24, textAlign: 'right' }}>
            {actualDaysLeft}
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  )
}

export function HomePage() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard', year, month],
    queryFn: () => fetchDashboard(year, month),
  })

  function prevMonth() {
    if (month === 1) { setYear(y => y - 1); setMonth(12) }
    else setMonth(m => m - 1)
  }

  function nextMonth() {
    const currentYear = now.getFullYear()
    const currentMonth = now.getMonth() + 1
    if (year === currentYear && month === currentMonth) return
    if (month === 12) { setYear(y => y + 1); setMonth(1) }
    else setMonth(m => m + 1)
  }

  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth() + 1

  const totalExpenses = parseFloat(data?.totalExpenses ?? '0')
  const totalIncome = parseFloat(data?.totalIncome ?? '0')
  const balance = parseFloat(data?.balance ?? '0')
  const daysLeft = data?.daysUntilEndOfMonth ?? 0
  const dailyBudget = parseFloat(data?.dailyRemainingBudget ?? '0')

  const chartData = (data?.expensesByCategory ?? []).map((c) => ({
    name: c.categoryName,
    amount: parseFloat(c.total),
    color: getCategoryColor(c.colorId),
  }))

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 3, md: 4 }}>
        {/* Month navigator */}
        <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
          <IconButton onClick={prevMonth} size="small">
            <ChevronLeftIcon />
          </IconButton>
          <Typography variant="h5" sx={{ fontWeight: 700, minWidth: 200, textAlign: 'center', fontSize: { xs: '1.1rem', md: '1.4rem' } }}>
            {MONTH_LABELS[month - 1]} {year}
          </Typography>
          <IconButton onClick={nextMonth} size="small" disabled={isCurrentMonth}>
            <ChevronRightIcon />
          </IconButton>
        </Stack>

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            {/* Summary cards */}
            <Grid container spacing={{ xs: 2, md: 3 }}>
              <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <SummaryCard
                  label="Expenses"
                  value={formatCurrency(totalExpenses)}
                  icon={<TrendingDownIcon />}
                  color="error"
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <SummaryCard
                  label="Income"
                  value={formatCurrency(totalIncome)}
                  icon={<TrendingUpIcon />}
                  color="success"
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <SummaryCard
                  label="Balance"
                  value={formatCurrency(balance)}
                  icon={<AccountBalanceWalletIcon />}
                  color={balance >= 0 ? 'primary' : 'warning'}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <SummaryCard
                  label={isCurrentMonth ? `${daysLeft} days left` : 'Days left'}
                  value={isCurrentMonth && daysLeft > 0 ? formatCurrency(dailyBudget) + '/day' : '—'}
                  subtitle={isCurrentMonth && daysLeft > 0 ? 'remaining daily budget' : undefined}
                  icon={<TodayIcon />}
                  color="info"
                />
              </Grid>
            </Grid>

            {/* Budget planner */}
            {isCurrentMonth && daysLeft > 0 && (
              <Grid container spacing={{ xs: 2, md: 3 }}>
                <Grid size={{ xs: 12, sm: 8, md: 6 }}>
                  <BudgetPlannerCard balance={balance} actualDaysLeft={daysLeft} />
                </Grid>
              </Grid>
            )}

            {/* Charts */}
            <Grid container spacing={{ xs: 2, md: 3 }}>
              {/* Bar chart by category */}
              <Grid size={{ xs: 12, md: 7 }}>
                <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
                  <CardContent sx={{ p: { xs: 2, md: 3 } }}>
                    <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 600 }}>
                      Expenses by Category
                    </Typography>
                    {chartData.length === 0 ? (
                      <Box sx={{ py: 6, textAlign: 'center' }}>
                        <Typography color="text.secondary">No expenses this month</Typography>
                      </Box>
                    ) : (
                      <Box sx={{ height: { xs: 240, md: 300 }, mt: 1 }}>
                        <ResponsiveContainer width="100%" height="100%">
                          <BarChart data={chartData} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 0 }}>
                            <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0f0f0" />
                            <XAxis
                              type="number"
                              tick={{ fontSize: 12 }}
                              axisLine={false}
                              tickLine={false}
                              tickFormatter={(v) => formatCurrency(v)}
                            />
                            <YAxis
                              type="category"
                              dataKey="name"
                              tick={{ fontSize: 12 }}
                              axisLine={false}
                              tickLine={false}
                              width={90}
                            />
                            <Tooltip
                              formatter={(v) => [formatCurrency(Number(v)), 'Amount']}
                              contentStyle={{ borderRadius: 8, border: '1px solid #e0e0e0' }}
                            />
                            <Bar dataKey="amount" radius={[0, 6, 6, 0]} maxBarSize={32}>
                              {chartData.map((entry, i) => (
                                <Cell key={i} fill={entry.color} />
                              ))}
                            </Bar>
                          </BarChart>
                        </ResponsiveContainer>
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* Pie chart by category */}
              <Grid size={{ xs: 12, md: 5 }}>
                <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, height: '100%' }}>
                  <CardContent sx={{ p: { xs: 2, md: 3 } }}>
                    <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 600 }}>
                      Category Distribution
                    </Typography>
                    {chartData.length === 0 ? (
                      <Box sx={{ py: 6, textAlign: 'center' }}>
                        <Typography color="text.secondary">No data yet</Typography>
                      </Box>
                    ) : (
                      <Box sx={{ height: { xs: 240, md: 300 }, mt: 1 }}>
                        <ResponsiveContainer width="100%" height="100%">
                          <PieChart>
                            <Pie
                              data={chartData}
                              cx="50%"
                              cy="42%"
                              innerRadius="38%"
                              outerRadius="62%"
                              dataKey="amount"
                              paddingAngle={2}
                            >
                              {chartData.map((entry, i) => (
                                <Cell key={i} fill={entry.color} />
                              ))}
                            </Pie>
                            <Legend
                              formatter={(v, entry) => {
                                const p = entry.payload as { amount: number }
                                return `${v}: ${formatCurrency(p.amount)}`
                              }}
                              wrapperStyle={{ fontSize: 12 }}
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
          </>
        )}
      </Stack>
    </Container>
  )
}
