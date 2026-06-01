import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteCategory } from '../api/categories'

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation<void, Error, number>({
    mutationFn: deleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
  })
}
