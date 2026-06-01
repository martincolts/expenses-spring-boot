import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteIncome } from '../api/incomes'

export function useDeleteIncome() {
  const queryClient = useQueryClient()
  return useMutation<void, Error, number>({
    mutationFn: deleteIncome,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incomes'] })
    },
  })
}
