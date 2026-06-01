import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createIncome } from '../api/incomes'
import type { CreateIncomeRequest, Income } from '../types/expense'

export function useCreateIncome() {
  const queryClient = useQueryClient()
  return useMutation<Income, Error, CreateIncomeRequest>({
    mutationFn: createIncome,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incomes'] })
    },
  })
}
