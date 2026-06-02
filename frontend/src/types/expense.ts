export interface CreateExpenseRequest {
  categoryId: number | null
  description: string
  amount: number
  currency: string
  incurredAt: string
  paid: boolean
}

export interface Expense {
  id: number
  tenantId: number
  categoryId: number | null
  description: string
  amount: string
  currency: string
  status: 'PENDING' | 'PAID'
  incurredAt: string
  createdAt: string
  paidAt: string | null
}

export interface Income {
  id: number
  tenantId: number
  amount: string
  createdAt: string
}

export interface CreateIncomeRequest {
  amount: number
}

export interface UpdateExpenseRequest {
  categoryId: number | null
  description: string
  amount: number
  currency: string
  incurredAt: string
  paid: boolean
}

export interface UpdateIncomeRequest {
  amount: number
}
