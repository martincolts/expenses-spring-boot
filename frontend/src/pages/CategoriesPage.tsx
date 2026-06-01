import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Container,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import CategoryIcon from '@mui/icons-material/Category'
import { fetchCategories } from '../api/categories'
import type { Category } from '../types/category'
import { NewCategoryModal } from '../components/NewCategoryModal'
import { useDeleteCategory } from '../hooks/useDeleteCategory'
import { getCategoryIcon } from '../constants/categoryIcons'
import { getCategoryColor } from '../constants/categoryColors'

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function CategoryAvatar({ category }: { category: Category }) {
  const Icon = getCategoryIcon(category.iconId)
  const color = getCategoryColor(category.colorId)
  return (
    <Box
      sx={{
        width: 36,
        height: 36,
        borderRadius: 1.5,
        bgcolor: color,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}
    >
      {Icon
        ? <Icon sx={{ color: '#fff', fontSize: 20 }} />
        : <CategoryIcon sx={{ color: '#fff', fontSize: 20 }} />
      }
    </Box>
  )
}

function CategoryCard({
  category,
  onDelete,
  onClick,
}: {
  category: Category
  onDelete: (id: number) => void
  onClick: (category: Category) => void
}) {
  return (
    <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
      <CardActionArea onClick={() => onClick(category)} sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0, flexGrow: 1, mr: 1 }}>
              <CategoryAvatar category={category} />
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="body1" sx={{ fontWeight: 500 }} noWrap>
                  {category.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {formatDate(category.createdAt)}
                </Typography>
              </Box>
            </Stack>
            <IconButton
              size="small"
              color="error"
              onClick={e => { e.stopPropagation(); onDelete(category.id) }}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}

export function CategoriesPage() {
  const [selected, setSelected] = useState<Category | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const { data: categories = [], isLoading, isError } = useQuery({
    queryKey: ['categories'],
    queryFn: fetchCategories,
  })
  const { mutate: deleteCategory } = useDeleteCategory()

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 2, md: 4 } }}>
      <Stack spacing={{ xs: 2, md: 3 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, fontSize: { xs: '1.25rem', md: '1.5rem' } }}>
            Categories
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateOpen(true)}
            size="medium"
          >
            Add
          </Button>
        </Stack>

        <NewCategoryModal open={createOpen} onClose={() => setCreateOpen(false)} />
        <NewCategoryModal
          open={selected != null}
          onClose={() => setSelected(null)}
          category={selected ?? undefined}
        />

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {isError && (
          <Typography color="error">Failed to load categories.</Typography>
        )}

        {!isLoading && !isError && categories.length === 0 && (
          <Box sx={{ py: 8, textAlign: 'center' }}>
            <Typography color="text.secondary">No categories yet.</Typography>
          </Box>
        )}

        {/* Mobile card list */}
        {!isLoading && categories.length > 0 && (
          <Box sx={{ display: { xs: 'block', md: 'none' } }}>
            <Stack spacing={1.5}>
              {categories.map(c => (
                <CategoryCard key={c.id} category={c} onDelete={deleteCategory} onClick={setSelected} />
              ))}
            </Stack>
          </Box>
        )}

        {/* Desktop table */}
        {!isLoading && categories.length > 0 && (
          <Box sx={{ display: { xs: 'none', md: 'block' } }}>
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ width: 52 }} />
                    <TableCell>Name</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {categories.map(c => (
                    <TableRow
                      key={c.id}
                      hover
                      onClick={() => setSelected(c)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell sx={{ py: 1.25 }}>
                        <CategoryAvatar category={c} />
                      </TableCell>
                      <TableCell>{c.name}</TableCell>
                      <TableCell>{formatDate(c.createdAt)}</TableCell>
                      <TableCell align="right">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={e => { e.stopPropagation(); deleteCategory(c.id) }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Stack>
    </Container>
  )
}
