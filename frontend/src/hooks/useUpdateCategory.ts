import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateCategory } from '../api/categories'
import type { UpdateCategoryRequest } from '../types/category'

export function useUpdateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateCategoryRequest }) =>
      updateCategory(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['categories'] }),
  })
}
