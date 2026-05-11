---
name: react-good-practices
description: Use when adding components, pages, hooks, API calls, or any frontend code in the `frontend/` directory. Triggers on requests to "add a page", "create a component", "add a form", "call the API from React", "add a hook", or anything involving MUI, TypeScript, or the Vite dev server.
---

# React Good Practices (Vite + TypeScript + MUI)

## Project conventions

- **Framework**: React 19 + TypeScript, bundled by Vite
- **UI library**: MUI v7 (`@mui/material`) — use its components instead of raw HTML + CSS
- **Dev server**: `cd frontend && npm run dev` (port 5173, proxies `/api` to Spring Boot on 8080)
- **Production build**: `./gradlew build` — runs `npm run build` automatically and bundles `frontend/dist/` into `src/main/resources/static/`

## File structure

```
frontend/src/
├── components/     # shared, reusable UI pieces (no page-level logic)
├── pages/          # one file per route/screen; imports components
├── hooks/          # custom hooks (useXxx.ts)
├── api/            # fetch wrappers, one file per backend resource
├── types/          # shared TypeScript types and interfaces
├── App.tsx         # router root; defines routes only
└── main.tsx        # entry point; ThemeProvider + CssBaseline live here
```

## Components

- One component per file; filename matches the export name (`ExpenseList.tsx` → `export function ExpenseList`).
- Named exports only — no `export default function` on anonymous functions.
- Keep components small. Extract sub-pieces when a component exceeds ~80 lines of JSX.
- Accept a typed `props` object; never use `any`.

```tsx
interface ExpenseCardProps {
  id: number
  description: string
  amount: number
}

export function ExpenseCard({ id, description, amount }: ExpenseCardProps) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1">{description}</Typography>
        <Typography variant="h6">${amount.toFixed(2)}</Typography>
      </CardContent>
    </Card>
  )
}
```

## MUI usage

- Use MUI layout components (`Box`, `Stack`, `Grid2`) instead of custom CSS for spacing.
- Prefer `sx` prop for one-off style overrides; extract to `styled()` only when reused.
- Never import from `@mui/material/index` — use the named sub-path: `import { Button } from '@mui/material'`.
- The theme is defined once in `main.tsx`. To extend it, pass a theme object to `createTheme()` there.

```tsx
<Box sx={{ p: 2 }}>
  <Stack spacing={2}>
    <Button variant="contained" onClick={handleSave}>Save</Button>
  </Stack>
</Box>
```

## Custom hooks

- Prefix with `use` (`useExpenses`, `useAuth`).
- One concern per hook — don't bundle fetching + form state + routing into one hook.
- Return plain values and setters, not JSX.

```tsx
// hooks/useExpenses.ts
export function useExpenses() {
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchExpenses().then(data => {
      setExpenses(data)
      setLoading(false)
    })
  }, [])

  return { expenses, loading }
}
```

## API layer (`src/api/`)

- One file per backend resource (e.g., `expenses.ts`, `auth.ts`).
- All fetch calls live here — no `fetch()` calls inside components or hooks directly.
- Include the JWT from localStorage in every authenticated request.

```tsx
// api/expenses.ts
const BASE = '/api/expenses'

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function fetchExpenses(): Promise<Expense[]> {
  const res = await fetch(BASE, { headers: authHeaders() })
  if (!res.ok) throw new Error('Failed to fetch expenses')
  return res.json()
}

export async function createExpense(body: CreateExpenseRequest): Promise<Expense> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to create expense')
  return res.json()
}
```

## TypeScript

- Define types in `src/types/` and import them explicitly — no inline object types in component files.
- Mirror backend response shapes exactly; use `interface` for objects, `type` for unions/aliases.
- Strict mode is already on (Vite template default); never suppress errors with `@ts-ignore`.

## State management

- Start with `useState` + prop drilling. Extract to `useContext` only when prop drilling crosses 2+ layers.
- Don't reach for external state libraries (Redux, Zustand) until there's a demonstrated need.

## Forms

- Use Formik (`useFormik`) + Yup for all forms with validation. See the `formik` skill for the full pattern.
- Wire MUI TextField via `formik.getFieldProps('fieldName')` — never write manual `onChange`/`onBlur` handlers.

## Dev workflow

```bash
# Start backend
docker compose up -d && ./gradlew bootRun

# Start frontend dev server (separate terminal)
cd frontend && npm run dev
# → http://localhost:5173 (API calls proxied to :8080)

# Production build (frontend included in JAR)
./gradlew build
./gradlew bootRun   # serves the built React app at /
```
