import { useEffect } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import { SearchProvider } from './context/SearchContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Admin from './pages/Admin'

function PrivateRoute({ children, adminOnly }: { children: React.ReactNode; adminOnly?: boolean }) {
  const { user, loading, setToken } = useAuth()
  const navigate = useNavigate()
  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-appBg">
        <p className="text-slate-600">Loading...</p>
        <button
          type="button"
          onClick={() => {
            setToken(null)
            navigate('/login', { replace: true })
          }}
          className="text-sm text-primary underline hover:no-underline"
        >
          Back to login
        </button>
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN') return <Navigate to="/" replace />
  return <>{children}</>
}

function App() {
  const { setToken } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const hash = window.location.hash
    if (hash.startsWith('#token=')) {
      const token = hash.slice(7).split('&')[0]
      if (token) {
        setToken(token)
        window.history.replaceState(null, '', window.location.pathname)
        navigate('/', { replace: true })
      }
    }
  }, [setToken, navigate])

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <SearchProvider>
              <Layout />
            </SearchProvider>
          </PrivateRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route
          path="admin"
          element={
            <PrivateRoute adminOnly>
              <Admin />
            </PrivateRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
