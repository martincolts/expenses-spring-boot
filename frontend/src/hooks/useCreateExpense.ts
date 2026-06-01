import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExpense } from '../api/expenses'
import type { CreateExpenseRequest, Expense } from '../types/expense'

export function useCreateExpense() {
  const queryClient = useQueryClient()
  return useMutation<Expense, Error, CreateExpenseRequest>({
    mutationFn: createExpense,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}
