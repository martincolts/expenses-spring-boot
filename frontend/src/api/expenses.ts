import { apiFetch } from './client'
import type { CreateExpenseRequest, Expense, UpdateExpenseRequest } from '../types/expense'

const BASE = '/api/expenses'

export async function fetchExpenses(): Promise<Expense[]> {
  const res = await apiFetch(BASE)
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
