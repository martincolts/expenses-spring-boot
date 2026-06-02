import { apiFetch } from './client'
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../types/category'
import type { Page } from '../types/page'

const BASE = '/api/categories'

export async function fetchCategories(page: number, size: number): Promise<Page<Category>> {
  const res = await apiFetch(`${BASE}?page=${page}&size=${size}`)
  if (!res.ok) throw new Error('Failed to fetch categories')
  return res.json()
}

export async function createCategory(body: CreateCategoryRequest): Promise<Category> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to create category')
  return res.json()
}

export async function updateCategory(id: number, body: UpdateCategoryRequest): Promise<Category> {
  const res = await apiFetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to update category')
  return res.json()
}

export async function deleteCategory(id: number): Promise<void> {
  const res = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete category')
}
