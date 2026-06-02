import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markExpenseUnpaid } from '../api/expenses'
import type { Expense } from '../types/expense'

export function useMarkExpenseUnpaid() {
  const queryClient = useQueryClient()
  return useMutation<Expense, Error, number>({
    mutationFn: markExpenseUnpaid,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}
