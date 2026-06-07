import { useState, type ReactNode } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Avatar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Divider,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Paper,
  Toolbar,
  Typography,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import SavingsIcon from '@mui/icons-material/Savings'
import LabelIcon from '@mui/icons-material/Label'
import { useAuthContext } from '../contexts/AuthContext'
import { SpendlyLogo } from './SpendlyLogo'

const DRAWER_WIDTH = 240

const NAV_ITEMS = [
  { label: 'Dashboard', icon: <DashboardIcon />, path: '/' },
  { label: 'Expenses', icon: <ReceiptLongIcon />, path: '/expenses' },
  { label: 'Incomes', icon: <SavingsIcon />, path: '/incomes' },
  { label: 'Categories', icon: <LabelIcon />, path: '/categories' },
]

interface AppShellProps {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const { user, logout } = useAuthContext()
  const navigate = useNavigate()
  const location = useLocation()

  const currentNavIndex = NAV_ITEMS.findIndex(item => item.path === location.pathname)

  function handleLogout() {
    setAnchorEl(null)
    logout()
    navigate('/login')
  }

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ justifyContent: 'center', gap: 1.5 }}>
        <SpendlyLogo size={30} />
        <Typography variant="h6" color="primary.main" sx={{ fontWeight: 700 }}>
          Spendly
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ px: 1, pt: 1, flexGrow: 1 }}>
        {NAV_ITEMS.map(({ label, icon, path }) => (
          <ListItem key={path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              selected={location.pathname === path}
              onClick={() => navigate(path)}
              sx={{ borderRadius: 2 }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>{icon}</ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 1 }}>
        <ListItemButton
          onClick={e => setAnchorEl(e.currentTarget)}
          sx={{ borderRadius: 2, gap: 1.5 }}
        >
          <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: 13 }}>
            {user?.name?.[0]?.toUpperCase()}
          </Avatar>
          <Box sx={{ overflow: 'hidden' }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>{user?.name}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
              {user?.email}
            </Typography>
          </Box>
        </ListItemButton>
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={() => setAnchorEl(null)}
          anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
          transformOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <MenuItem onClick={handleLogout}>Sign out</MenuItem>
        </Menu>
      </Box>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Desktop permanent drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: { md: DRAWER_WIDTH },
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
        open
      >
        {drawerContent}
      </Drawer>

      {/* Main content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          mb: { xs: '56px', md: 0 },
          minHeight: '100vh',
          bgcolor: 'grey.50',
        }}
      >
        {children}
      </Box>

      {/* Mobile bottom navigation */}
      <Paper
        elevation={3}
        sx={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          display: { xs: 'block', md: 'none' },
          zIndex: theme => theme.zIndex.appBar,
        }}
      >
        <BottomNavigation
          value={currentNavIndex}
          onChange={(_, idx) => navigate(NAV_ITEMS[idx].path)}
        >
          {NAV_ITEMS.map(({ label, icon, path }) => (
            <BottomNavigationAction key={path} label={label} icon={icon} />
          ))}
        </BottomNavigation>
      </Paper>
    </Box>
  )
}
