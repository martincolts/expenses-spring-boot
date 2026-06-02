import { useQuery } from '@tanstack/react-query'
import { fetchCategories } from '../api/categories'
import type { Category } from '../types/category'

export function useCategories() {
  return useQuery<Category[], Error>({
    queryKey: ['categories'],
    queryFn: async () => {
      const page = await fetchCategories(0, 100)
      return page.content
    },
  })
}
