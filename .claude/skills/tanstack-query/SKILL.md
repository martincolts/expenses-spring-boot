# TanStack Query skill

## When to use this skill

Use when fetching data from the backend (`useQuery`), submitting forms or mutating server state (`useMutation`), or invalidating/refreshing cached data after a write.

Trigger phrases: "fetch expenses from the API", "load the list of X", "submit the form", "after saving refresh the list", "show a loading spinner", "cache the response", "invalidate after mutation".

---

## Setup

`QueryClient` and `QueryClientProvider` are wired in `src/main.tsx`. No additional setup needed per feature.

```tsx
// main.tsx (already configured)
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
const queryClient = new QueryClient()
// ... wrap <App /> with <QueryClientProvider client={queryClient}>
```

---

## Reading data — `useQuery`

Use for GET requests: lists, detail pages, any data the component needs to display.

```tsx
// hooks/useExpenses.ts
import { useQuery } from '@tanstack/react-query'
import { fetchExpenses } from '../api/expenses'
import type { Expense } from '../types/expense'

export function useExpenses() {
  return useQuery<Expense[], Error>({
    queryKey: ['expenses'],
    queryFn: fetchExpenses,
  })
}

// In a component:
const { data: expenses, isPending, isError, error } = useExpenses()
```

### Query key rules

- Always an array: `['expenses']`, `['expenses', id]`, `['expenses', { tenantId }]`
- More specific keys nest under generic ones: `['expenses']` is the parent of `['expenses', 42]`
- Invalidating `['expenses']` refreshes all queries whose key starts with `'expenses'`

---

## Writing data — `useMutation`

Use for POST / PUT / PATCH / DELETE. Always invalidate the relevant query cache on success so the UI refreshes automatically.

```tsx
// hooks/useCreateExpense.ts
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExpense } from '../api/expenses'
import type { CreateExpenseRequest, Expense } from '../types/expense'

export function useCreateExpense() {
  const queryClient = useQueryClient()

  return useMutation<Expense, Error, CreateExpenseRequest>({
    mutationFn: createExpense,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}

// In a component:
const { mutate, isPending, isError, error } = useCreateExpense()
mutate({ description: 'Lunch', amount: 12.50, categoryId: 3 })
```

---

## Error handling

Errors thrown by the `queryFn` / `mutationFn` are surfaced in `error`. Display them with MUI `Alert`:

```tsx
{isError && (
  <Alert severity="error" variant="outlined">
    {error.message}
  </Alert>
)}
```

In the API layer (`src/api/`), always throw `new Error(message)` when `res.ok` is false — never return a fallback value. This ensures TanStack Query captures the error correctly.

---

## Loading states

- `isPending` — true on first load (no cached data yet)
- `isFetching` — true whenever a network request is in flight (including background refetches)
- For initial renders, check `isPending`. For background refresh indicators, check `isFetching`.

```tsx
if (isPending) return <CircularProgress />
if (isError) return <Alert severity="error">{error.message}</Alert>
return <ExpenseList expenses={data} />
```

---

## Mutations that need the authenticated user

After `login` / `register`, the token is stored in `localStorage`. The API layer reads it via `authHeaders()`. No extra TanStack Query wiring needed — just call `mutate(credentials)` and the `onSuccess` callback persists the token.

---

## Common patterns

### Fetch by ID
```tsx
export function useExpense(id: number) {
  return useQuery<Expense, Error>({
    queryKey: ['expenses', id],
    queryFn: () => fetchExpense(id),
  })
}
```

### Delete with cache invalidation
```tsx
export function useDeleteExpense() {
  const queryClient = useQueryClient()
  return useMutation<void, Error, number>({
    mutationFn: deleteExpense,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
    },
  })
}
```

### Disable a query until a value is available
```tsx
useQuery({
  queryKey: ['expenses', tenantId],
  queryFn: () => fetchExpensesByTenant(tenantId),
  enabled: tenantId != null,
})
```

---

## What NOT to do

- Do not call `fetch()` directly inside components — put it in `src/api/` and use a hook.
- Do not use `useEffect` + `useState` for remote data — that is exactly what `useQuery` replaces.
- Do not manage `loading` / `error` booleans manually — TanStack Query provides them.
- Do not share a single global query key string — use the array form so invalidation is precise.
