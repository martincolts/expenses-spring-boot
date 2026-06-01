---
name: responsive-mobile-first
description: Use when adding or modifying any component, page, or layout in the frontend/. Enforces mobile-first responsive design using MUI v7 breakpoints and sx prop syntax. Triggers on "make this responsive", "mobile layout", "add a page", "create a component", or any time a component uses fixed widths, px sizes, or desktop-only layout.
---

# Responsive Mobile-First Design (MUI v7)

## Core principle

Design for `xs` (mobile, ≥0px) first, then override upward. Never write a desktop layout and try to shrink it down.

```tsx
// Wrong — desktop first
sx={{ display: 'flex', flexDirection: 'row' }}  // then fighting mobile

// Correct — mobile first
sx={{ display: 'flex', flexDirection: 'column', md: { flexDirection: 'row' } }}
```

## MUI breakpoints reference

| Key | Width   | Target device            |
|-----|---------|--------------------------|
| xs  | ≥ 0px   | phones (default)         |
| sm  | ≥ 600px | large phones / portrait tablets |
| md  | ≥ 900px | landscape tablets / small laptops |
| lg  | ≥ 1200px| desktops                 |
| xl  | ≥ 1536px| wide screens             |

In this project, **xs and sm are treated as mobile**. Most layout changes happen at `md`.

## Responsive `sx` syntax

Use the object form — every CSS property that changes per breakpoint uses an object:

```tsx
sx={{
  p: { xs: 2, md: 4 },
  fontSize: { xs: '0.875rem', md: '1rem' },
  flexDirection: { xs: 'column', md: 'row' },
  display: { xs: 'none', md: 'block' },   // hide on mobile
  width: { xs: '100%', md: 400 },
}}
```

Never use `theme.breakpoints.up()` inside `sx` — the object shorthand is cleaner and preferred.

## Layout patterns

### Page wrapper

Every page gets a `Container` with responsive padding. Never use a raw `Box` with a hardcoded `maxWidth` px value.

```tsx
import { Container, Stack } from '@mui/material'

export function ExpensesPage() {
  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        {/* content */}
      </Stack>
    </Container>
  )
}
```

### Two-column → single-column

```tsx
<Grid2 container spacing={{ xs: 2, md: 3 }}>
  <Grid2 size={{ xs: 12, md: 8 }}>
    <MainContent />
  </Grid2>
  <Grid2 size={{ xs: 12, md: 4 }}>
    <Sidebar />
  </Grid2>
</Grid2>
```

### Card grid (expense/income lists)

```tsx
<Grid2 container spacing={2}>
  {items.map(item => (
    <Grid2 key={item.id} size={{ xs: 12, sm: 6, lg: 4 }}>
      <ItemCard {...item} />
    </Grid2>
  ))}
</Grid2>
```

### Table → card list on mobile

Tables overflow on small screens. Hide the table on xs/sm and show a card list instead:

```tsx
{/* Mobile card list */}
<Box sx={{ display: { xs: 'block', md: 'none' } }}>
  <Stack spacing={1.5}>
    {expenses.map(e => <ExpenseCard key={e.id} {...e} />)}
  </Stack>
</Box>

{/* Desktop table */}
<Box sx={{ display: { xs: 'none', md: 'block' } }}>
  <TableContainer component={Paper}>
    <Table>...</Table>
  </TableContainer>
</Box>
```

## Typography

Scale headings down on mobile:

```tsx
<Typography variant="h4" sx={{ fontSize: { xs: '1.25rem', md: '2.125rem' } }}>
  My Expenses
</Typography>
```

Or define responsive typography in the theme (in `main.tsx`):

```ts
const theme = createTheme({
  typography: {
    h4: { fontSize: '1.25rem', [defaultTheme.breakpoints.up('md')]: { fontSize: '2.125rem' } },
  },
})
```

## Touch targets

Buttons, icon buttons, and list items must be at least 48×48px on mobile. MUI defaults are close but check:

```tsx
// Icon-only actions — add explicit size
<IconButton size="large">   // 48px tap target
  <DeleteIcon />
</IconButton>

// Don't use size="small" for primary actions on mobile
```

## Spacing and density

- Use `spacing` units (`p: 2` = 16px), never raw `px` values in `sx` for spacing.
- On mobile (`xs`): tighter spacing — `p: 2`, `spacing: 1.5`.
- On desktop (`md+`): more breathing room — `p: 3–4`, `spacing: 2–3`.
- `fullWidth` on all form inputs and primary action buttons.

```tsx
<TextField fullWidth label="Amount" ... />
<Button fullWidth variant="contained">Save</Button>
```

## Navigation

For this app, the navigation pattern per screen size:

| Screen | Pattern                        |
|--------|-------------------------------|
| xs/sm  | Bottom navigation or hamburger drawer |
| md+    | Persistent sidebar or top nav bar     |

Show/hide with `display`:

```tsx
<BottomNavigation sx={{ display: { xs: 'flex', md: 'none' }, position: 'fixed', bottom: 0, width: '100%' }}>
  ...
</BottomNavigation>

<Drawer variant="permanent" sx={{ display: { xs: 'none', md: 'block' } }}>
  ...
</Drawer>
```

## Avoid these anti-patterns

| Anti-pattern | Fix |
|---|---|
| `width: 500` fixed px in `sx` | `width: { xs: '100%', md: 500 }` |
| `display: 'flex'` with `flexDirection: 'row'` only | Add `flexDirection: { xs: 'column', md: 'row' }` |
| `<Table>` without mobile fallback | Wrap in responsive show/hide + card list |
| `fontSize: '1.5rem'` fixed | `fontSize: { xs: '1rem', md: '1.5rem' }` |
| `p: 4` everywhere | `p: { xs: 2, md: 4 }` |
| `gap: '32px'` raw string | `gap: { xs: 1, md: 2 }` (use spacing units) |
| `<IconButton size="small">` for main actions | Use `size="medium"` or `size="large"` |
| `position: absolute` offset without mobile adjustment | Test on 375px viewport; adjust per breakpoint |

## Checklist before completing any frontend task

- [ ] All spacing uses MUI spacing units, not raw `px`
- [ ] Fixed widths are `{ xs: '100%', sm/md: <value> }`
- [ ] Flex rows become columns on mobile
- [ ] Tables have a mobile card-list fallback
- [ ] All form fields and primary buttons are `fullWidth`
- [ ] Touch targets are ≥ 48px for interactive elements
- [ ] Tested (or visually verified) at 375px and 1280px widths
