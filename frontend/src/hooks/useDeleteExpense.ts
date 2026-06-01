import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteExpense } from '../api/expenses'

export function useDeleteExpense() {
  const queryClient = useQueryClient()
  return useMutation<void, Error, number>({
    mutationFn: deleteExpense,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}
