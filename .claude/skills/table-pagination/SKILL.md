# Table pagination skill

## When to use this skill

Use whenever adding or modifying a page that renders a list in a table (desktop) or card stack (mobile). Every table in this project **must** use server-side pagination. No table should ever load all rows at once.

Trigger phrases: "add pagination", "paginate the table", "add a table", "add a list page", "show X per page", "server-side pagination".

---

## Backend contract

The backend exposes paginated list endpoints following Spring's `Page<T>` structure. Query params are `page` (0-indexed) and `size`.

```
GET /api/expenses?page=0&size=20
```

Response shape:
```json
{
  "content": [...],
  "totalElements": 143,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

---

## TypeScript type

Add this once to `src/types/page.ts` (create if it doesn't exist):

```ts
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // 0-indexed current page
  size: number
}
```

---

## API layer

Update the fetch function to accept `page` and `size` and return `Page<T>`:

```ts
// src/api/expenses.ts
import type { Page } from '../types/page'

export async function fetchExpenses(page: number, size: number): Promise<Page<Expense>> {
  const res = await apiFetch(`${BASE}?page=${page}&size=${size}`)
  if (!res.ok) throw new Error('Failed to fetch expenses')
  return res.json()
}
```

---

## Query key & useQuery pattern

Include `page` and `rowsPerPage` in the query key so TanStack Query refetches automatically when either changes. The `queryFn` must be a closure that captures the current values.

```ts
const [page, setPage] = useState(0)
const [rowsPerPage, setRowsPerPage] = useState(20)

const { data, isPending, isError } = useQuery({
  queryKey: ['expenses', { page, rowsPerPage }],
  queryFn: () => fetchExpenses(page, rowsPerPage),
})

const expenses = data?.content ?? []
const totalElements = data?.totalElements ?? 0
```

When a mutation (create/delete/update) succeeds, invalidate the base key so all pages are refreshed:
```ts
queryClient.invalidateQueries({ queryKey: ['expenses'] })
```

---

## Desktop table with `TablePagination`

Place `TablePagination` **outside** the `Table` but inside the same `Paper`/`TableContainer` wrapper. Use `component="div"` to avoid invalid HTML nesting. Always reset page to 0 when `rowsPerPage` changes.

```tsx
<TableContainer
  component={Paper}
  elevation={0}
  sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}
>
  <Table>
    <TableHead>...</TableHead>
    <TableBody>
      {expenses.map(e => (
        <TableRow key={e.id} hover onClick={() => setSelected(e)} sx={{ cursor: 'pointer' }}>
          ...
        </TableRow>
      ))}
    </TableBody>
  </Table>
  <TablePagination
    component="div"
    count={totalElements}
    page={page}
    onPageChange={(_, newPage) => setPage(newPage)}
    rowsPerPage={rowsPerPage}
    onRowsPerPageChange={e => {
      setRowsPerPage(parseInt(e.target.value))
      setPage(0)
    }}
    rowsPerPageOptions={[10, 20, 50]}
  />
</TableContainer>
```

MUI imports to add:
```ts
import { TablePagination } from '@mui/material'
```

---

## Mobile card list with pagination controls

The mobile view uses a card stack instead of a table. Add simple previous/next controls below the stack using MUI `TablePagination` with `component="div"` — it renders as a standalone row with the same styling as desktop.

```tsx
{/* Mobile card list */}
<Box sx={{ display: { xs: 'block', md: 'none' } }}>
  <Stack spacing={1.5}>
    {expenses.map(e => (
      <ExpenseCard key={e.id} expense={e} onDelete={deleteExpense} onClick={setSelected} />
    ))}
  </Stack>
  <TablePagination
    component="div"
    count={totalElements}
    page={page}
    onPageChange={(_, newPage) => setPage(newPage)}
    rowsPerPage={rowsPerPage}
    onRowsPerPageChange={e => {
      setRowsPerPage(parseInt(e.target.value))
      setPage(0)
    }}
    rowsPerPageOptions={[10, 20, 50]}
    sx={{ mt: 1 }}
  />
</Box>
```

---

## Full page example (ExpensesPage skeleton)

```tsx
export function ExpensesPage() {
  const [selected, setSelected] = useState<Expense | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(20)

  const { data, isPending, isError } = useQuery({
    queryKey: ['expenses', { page, rowsPerPage }],
    queryFn: () => fetchExpenses(page, rowsPerPage),
  })

  const expenses = data?.content ?? []
  const totalElements = data?.totalElements ?? 0

  const { mutate: deleteExpense } = useDeleteExpense()

  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(e.target.value))
    setPage(0)
  }

  const paginationProps = {
    count: totalElements,
    page,
    onPageChange: (_: unknown, newPage: number) => setPage(newPage),
    rowsPerPage,
    onRowsPerPageChange: handleRowsPerPageChange,
    rowsPerPageOptions: [10, 20, 50],
  }

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        {/* Header */}
        ...

        {/* Loading / error / empty states */}
        ...

        {/* Mobile */}
        {!isPending && expenses.length > 0 && (
          <Box sx={{ display: { xs: 'block', md: 'none' } }}>
            <Stack spacing={1.5}>
              {expenses.map(e => <ExpenseCard key={e.id} ... />)}
            </Stack>
            <TablePagination component="div" {...paginationProps} sx={{ mt: 1 }} />
          </Box>
        )}

        {/* Desktop */}
        {!isPending && expenses.length > 0 && (
          <Box sx={{ display: { xs: 'none', md: 'block' } }}>
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
              <Table>
                <TableHead>...</TableHead>
                <TableBody>{expenses.map(e => <TableRow key={e.id} .../>)}</TableBody>
              </Table>
              <TablePagination component="div" {...paginationProps} />
            </TableContainer>
          </Box>
        )}
      </Stack>
    </Container>
  )
}
```

---

## What NOT to do

- Do not use client-side pagination (slicing a full array). Always use server-side `page`/`size` params.
- Do not put page state inside a hook file — keep it in the page component where it lives alongside `rowsPerPage`.
- Do not forget to reset `page` to `0` when `rowsPerPage` changes, or when the user applies a filter.
- Do not invalidate with `{ queryKey: ['expenses', { page, rowsPerPage }] }` after a mutation — invalidate the base `['expenses']` key so all cached pages are marked stale.
- Do not show the pagination controls while `isPending` is true — only render them alongside the data.
