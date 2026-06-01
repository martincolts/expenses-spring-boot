import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateIncome } from '../api/incomes'
import type { UpdateIncomeRequest } from '../types/expense'

export function useUpdateIncome() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateIncomeRequest }) =>
      updateIncome(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incomes'] }),
  })
}
