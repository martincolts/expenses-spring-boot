export interface CategoryColor {
  id: string
  value: string
  label: string
}

export const CATEGORY_COLORS: CategoryColor[] = [
  { id: 'red', value: '#ef4444', label: 'Red' },
  { id: 'orange', value: '#f97316', label: 'Orange' },
  { id: 'amber', value: '#f59e0b', label: 'Amber' },
  { id: 'green', value: '#22c55e', label: 'Green' },
  { id: 'teal', value: '#14b8a6', label: 'Teal' },
  { id: 'blue', value: '#3b82f6', label: 'Blue' },
  { id: 'indigo', value: '#6366f1', label: 'Indigo' },
  { id: 'purple', value: '#a855f7', label: 'Purple' },
  { id: 'pink', value: '#ec4899', label: 'Pink' },
  { id: 'gray', value: '#6b7280', label: 'Gray' },
]

export const DEFAULT_CATEGORY_COLOR = '#6b7280'

export function getCategoryColor(id: string | null | undefined): string {
  if (!id) return DEFAULT_CATEGORY_COLOR
  return CATEGORY_COLORS.find(c => c.id === id)?.value ?? DEFAULT_CATEGORY_COLOR
}
