import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const API_BASE = '/api'

export default function Login() {
  const { user, loading, setToken } = useAuth()
  const [devLoginError, setDevLoginError] = useState<string | null>(null)

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-appBg">
        <p className="text-slate-600">Loading...</p>
      </div>
    )
  }

  if (user) {
    return <Navigate to="/" replace />
  }

  const handleGoogleLogin = () => {
    window.location.href = API_BASE + '/oauth2/authorization/google'
  }

  const handleDevLogin = async () => {
    setDevLoginError(null)
    try {
      const res = await fetch(API_BASE + '/dev/login', { credentials: 'include' })
      if (!res.ok) {
        setDevLoginError('Dev login not available (start backend with DEV_MODE=true)')
        return
      }
      const data = await res.json()
      if (data?.token) {
        setToken(data.token)
      } else {
        setDevLoginError('Invalid response')
      }
    } catch {
      setDevLoginError('Backend not reachable. Is it running on port 8080?')
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-appBg px-4">
      <div className="w-full max-w-md text-center">
        <div className="mb-8 text-primary bg-pastelPurple p-4 rounded-3xl inline-flex">
          <span className="material-symbols-outlined text-5xl filled-icon">directions_car</span>
        </div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Vehicle Health Report</h1>
        <p className="text-slate-600 mb-8">Sign in with your company Google account to continue.</p>
        <button
          type="button"
          onClick={handleGoogleLogin}
          className="w-full flex items-center justify-center gap-3 bg-white border-2 border-slate-200 hover:border-primary/50 hover:bg-pastelPurple/30 text-slate-800 font-semibold px-6 py-4 rounded-2xl transition-all card-shadow"
        >
          <svg className="w-6 h-6" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.18H12v4.13h5.92c-.26 1.37-1.04 2.58-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            />
          </svg>
          Sign in with Google
        </button>
        <div className="mt-6 pt-6 border-t border-slate-200">
          <p className="text-xs text-slate-400 mb-2">Testing without Google or MongoDB?</p>
          <button
            type="button"
            onClick={handleDevLogin}
            className="w-full py-2.5 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            Dev Login (no SSO, no DB)
          </button>
          {devLoginError && (
            <p className="mt-2 text-xs text-red-600">{devLoginError}</p>
          )}
        </div>
        <p className="mt-6 text-sm text-slate-500">
          Contact your admin if SSO is not enabled for your account.
        </p>
      </div>
    </div>
  )
}
