import { useState } from 'react'
import { Box, Card, CardContent, Divider, Tab, Tabs, Typography } from '@mui/material'
import { LoginForm } from '../components/LoginForm'
import { RegisterForm } from '../components/RegisterForm'
import logoUrl from '../../assets/logo.svg'

export function LoginPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(160deg, #dceeff 0%, #c2dcf5 55%, #a8cce8 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card
        sx={{
          width: '100%',
          maxWidth: 460,
          borderRadius: 4,
          boxShadow: '0 8px 40px rgba(26,106,184,0.15)',
          overflow: 'visible',
        }}
      >
        <Box
          sx={{
            background: '#fff',
            borderRadius: '16px 16px 0 0',
            px: 3,
            pt: 3,
            pb: 1,
            display: 'flex',
            justifyContent: 'center',
          }}
        >
          <img
            src={logoUrl}
            alt="Spendly"
            style={{ width: '100%', maxWidth: 300 }}
          />
        </Box>

        <CardContent sx={{ p: 4 }}>
          <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
            {tab === 0 ? 'Welcome back — sign in to continue.' : 'Create your free account.'}
          </Typography>

          <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="fullWidth" sx={{ mb: 3 }}>
            <Tab label="Sign In" />
            <Tab label="Create Account" />
          </Tabs>

          <Divider sx={{ mb: 3 }} />

          {tab === 0 ? <LoginForm /> : <RegisterForm />}
        </CardContent>
      </Card>

      <Typography variant="caption" color="text.secondary" sx={{ mt: 3, opacity: 0.7 }}>
        © {new Date().getFullYear()} Spendly — Personal Expense Tracker
      </Typography>
    </Box>
  )
}
