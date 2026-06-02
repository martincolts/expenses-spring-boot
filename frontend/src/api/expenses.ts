import { apiFetch } from './client'
import type { CreateExpenseRequest, Expense, UpdateExpenseRequest } from '../types/expense'
import type { Page } from '../types/page'

const BASE = '/api/expenses'

export interface ExpenseFilters {
  categoryId?: number | null
  status?: 'PENDING' | 'PAID' | null
  dateFrom?: string | null
  dateTo?: string | null
}

export async function fetchExpenses(
  page: number,
  size: number,
  filters: ExpenseFilters = {},
): Promise<Page<Expense>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (filters.categoryId != null) params.set('categoryId', String(filters.categoryId))
  if (filters.status) params.set('status', filters.status)
  if (filters.dateFrom) params.set('dateFrom', `${filters.dateFrom}T00:00:00+00:00`)
  if (filters.dateTo) params.set('dateTo', `${filters.dateTo}T23:59:59+00:00`)
  const res = await apiFetch(`${BASE}?${params}`)
  if (!res.ok) throw new Error('Failed to fetch expenses')
  return res.json()
}

export async function createExpense(body: CreateExpenseRequest): Promise<Expense> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to create expense')
  return res.json()
}

export async function updateExpense(id: number, body: UpdateExpenseRequest): Promise<Expense> {
  const res = await apiFetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to update expense')
  return res.json()
}

export async function deleteExpense(id: number): Promise<void> {
  const res = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete expense')
}
