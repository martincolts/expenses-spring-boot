import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCategory } from '../api/categories'
import type { Category, CreateCategoryRequest } from '../types/category'

export function useCreateCategory() {
  const queryClient = useQueryClient()
  return useMutation<Category, Error, CreateCategoryRequest>({
    mutationFn: createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
  })
}
