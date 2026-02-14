import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Sidebar() {
  const { user } = useAuth()
  const location = useLocation()
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'
  const initials = user?.name
    ? user.name.split(/\s+/).map((n) => n[0]).slice(0, 2).join('').toUpperCase()
    : user?.email?.slice(0, 2).toUpperCase() ?? '?'

  return (
    <aside className="fixed left-0 top-0 h-full w-20 bg-white border-r border-gray-100 flex flex-col items-center py-8 z-50 hidden md:flex">
      <div className="mb-12 text-primary bg-pastelBrand p-3 rounded-2xl">
        <span className="material-symbols-outlined text-3xl filled-icon">directions_car</span>
      </div>
      <nav className="flex flex-col gap-10 flex-1">
        <Link
          to="/"
          className={location.pathname === '/' ? 'text-primary' : 'text-slate-400 hover:text-primary transition-colors'}
        >
          <span className="material-symbols-outlined text-2xl filled-icon">home</span>
        </Link>
        {isAdmin && (
          <Link
            to="/admin"
            className={
              location.pathname.startsWith('/admin') ? 'text-primary' : 'text-slate-400 hover:text-primary transition-colors'
            }
          >
            <span className="material-symbols-outlined text-2xl">admin_panel_settings</span>
          </Link>
        )}
      </nav>
      <div className="mt-auto flex flex-col gap-8 items-center">
        <div
          className="w-12 h-12 rounded-2xl bg-pastelOrange flex items-center justify-center text-orange-700 font-bold shadow-sm"
          title={user?.email}
        >
          {initials}
        </div>
      </div>
    </aside>
  )
}
