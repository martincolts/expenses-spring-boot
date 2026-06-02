import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markExpensePaid } from '../api/expenses'
import type { Expense } from '../types/expense'

export function useMarkExpensePaid() {
  const queryClient = useQueryClient()
  return useMutation<Expense, Error, number>({
    mutationFn: markExpensePaid,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}
