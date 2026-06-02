import { apiFetch } from './client'
import type { CreateIncomeRequest, Income, UpdateIncomeRequest } from '../types/expense'
import type { Page } from '../types/page'

const BASE = '/api/incomes'

export async function fetchIncomes(page: number, size: number): Promise<Page<Income>> {
  const res = await apiFetch(`${BASE}?page=${page}&size=${size}`)
  if (!res.ok) throw new Error('Failed to fetch incomes')
  return res.json()
}

export async function createIncome(body: CreateIncomeRequest): Promise<Income> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to create income')
  return res.json()
}

export async function updateIncome(id: number, body: UpdateIncomeRequest): Promise<Income> {
  const res = await apiFetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to update income')
  return res.json()
}

export async function deleteIncome(id: number): Promise<void> {
  const res = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete income')
}
