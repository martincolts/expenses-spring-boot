export interface Category {
  id: number
  tenantId: number
  name: string
  iconId?: string
  colorId?: string
  createdAt: string
}

export interface CreateCategoryRequest {
  name: string
  iconId?: string
  colorId?: string
}

export interface UpdateCategoryRequest {
  name: string
  iconId?: string
  colorId?: string
}
