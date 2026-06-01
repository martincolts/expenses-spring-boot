import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateExpense } from '../api/expenses'
import type { UpdateExpenseRequest } from '../types/expense'

export function useUpdateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateExpenseRequest }) =>
      updateExpense(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['expenses'] }),
  })
}
