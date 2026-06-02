import { apiFetch } from './client'

export interface CategoryExpenseTotal {
  categoryId: number
  categoryName: string
  total: string
}

export interface DashboardData {
  totalExpenses: string
  totalIncome: string
  balance: string
  daysUntilEndOfMonth: number
  dailyRemainingBudget: string
  expensesByCategory: CategoryExpenseTotal[]
}

export async function fetchDashboard(year: number, month: number): Promise<DashboardData> {
  const params = new URLSearchParams({ year: String(year), month: String(month) })
  const res = await apiFetch(`/api/dashboard?${params}`)
  if (!res.ok) throw new Error('Failed to fetch dashboard')
  return res.json()
}
