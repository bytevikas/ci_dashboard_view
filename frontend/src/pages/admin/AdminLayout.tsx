import { Link, Outlet, useLocation } from 'react-router-dom'

const TABS = [
  { path: '/admin',        label: 'Dashboard',     icon: 'monitoring',  description: 'Search activity & analytics' },
  { path: '/admin/users',  label: 'Manage Users',  icon: 'group',       description: 'Add, remove & manage roles' },
  { path: '/admin/config', label: 'Manage Config', icon: 'tune',        description: 'Rate limits & cache settings' },
] as const

export default function AdminLayout() {
  const { pathname } = useLocation()

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* Page heading */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
            <span className="material-symbols-outlined text-primary text-xl">admin_panel_settings</span>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Admin Panel</h1>
            <p className="text-sm text-slate-500">Manage your application settings and monitor activity</p>
          </div>
        </div>
      </div>

      {/* Tab navigation — pill cards */}
      <nav className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8">
        {TABS.map((tab) => {
          const active = tab.path === '/admin'
            ? pathname === '/admin'
            : pathname.startsWith(tab.path)
          return (
            <Link
              key={tab.path}
              to={tab.path}
              className={`group relative flex items-center gap-3 px-5 py-4 rounded-2xl transition-all
                ${active
                  ? 'bg-primary text-white shadow-lg shadow-primary/20'
                  : 'bg-white text-slate-600 border border-slate-200 hover:border-primary/30 hover:shadow-md card-shadow'
                }`}
            >
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-colors
                ${active ? 'bg-white/20' : 'bg-slate-100 group-hover:bg-primary/10'}`}
              >
                <span className={`material-symbols-outlined text-xl ${active ? 'text-white' : 'text-slate-500 group-hover:text-primary'}`}>
                  {tab.icon}
                </span>
              </div>
              <div className="min-w-0">
                <p className={`text-sm font-bold ${active ? 'text-white' : 'text-slate-800'}`}>{tab.label}</p>
                <p className={`text-xs truncate ${active ? 'text-white/70' : 'text-slate-400'}`}>{tab.description}</p>
              </div>
              {active && (
                <div className="absolute right-4 top-1/2 -translate-y-1/2">
                  <span className="material-symbols-outlined text-white/50 text-lg">chevron_right</span>
                </div>
              )}
            </Link>
          )
        })}
      </nav>

      {/* Active sub-page */}
      <Outlet />
    </div>
  )
}
