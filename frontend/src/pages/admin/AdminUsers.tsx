import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../../api/client'

type UserRow = {
  id: string
  email: string
  name: string | null
  role: string
  ssoEnabled: boolean
  createdAt: string
}

const SUPER_ADMIN_EMAIL = 'vikas.kumar8@cars24.com'

export default function AdminUsers() {
  const [users, setUsers] = useState<UserRow[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [addEmail, setAddEmail] = useState('')
  const [addName, setAddName] = useState('')
  const [addRole, setAddRole] = useState<'USER' | 'ADMIN'>('USER')
  const [addSsoEnabled, setAddSsoEnabled] = useState(true)
  const [message, setMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const showMsg = (type: 'ok' | 'err', text: string) => {
    setMessage({ type, text })
    setTimeout(() => setMessage(null), 4000)
  }

  const loadUsers = useCallback(async () => {
    const q = search ? `?search=${encodeURIComponent(search)}` : ''
    const { data, error } = await api<UserRow[]>(`/admin/users${q}`)
    if (error) { showMsg('err', error); return }
    if (data) setUsers(data)
  }, [search])

  useEffect(() => {
    let cancelled = false
    async function run() {
      setLoading(true)
      await loadUsers()
      if (!cancelled) setLoading(false)
    }
    run()
    return () => { cancelled = true }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Debounced search
  const searchMountedRef = useRef(false)
  useEffect(() => {
    if (!searchMountedRef.current) { searchMountedRef.current = true; return }
    const t = setTimeout(loadUsers, 300)
    return () => clearTimeout(t)
  }, [search, loadUsers])

  // ── Handlers ─────────────────────────────────────────────────────────

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!addEmail.trim()) return
    const { error } = await api<unknown>('/admin/users', {
      method: 'POST',
      body: JSON.stringify({ email: addEmail.trim(), name: addName.trim() || null, role: addRole, ssoEnabled: addSsoEnabled }),
    })
    if (error) { showMsg('err', error); return }
    showMsg('ok', 'User added/updated.')
    setAddEmail('')
    setAddName('')
    setAddRole('USER')
    await loadUsers()
  }

  const handleRemoveUser = async (userId: string, email: string) => {
    if (email.toLowerCase() === SUPER_ADMIN_EMAIL) { showMsg('err', 'Cannot remove super admin.'); return }
    if (!window.confirm(`Remove user ${email}?`)) return
    const { status, error } = await api(`/admin/users/${userId}`, { method: 'DELETE' })
    if (status === 204) { showMsg('ok', 'User removed.'); await loadUsers() }
    else { showMsg('err', error || 'Failed to remove.') }
  }

  const handleSetRole = async (userId: string, email: string, newRole: string) => {
    if (email.toLowerCase() === SUPER_ADMIN_EMAIL) { showMsg('err', 'Cannot change super admin role.'); return }
    const { error } = await api(`/admin/users/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role: newRole }),
    })
    if (error) { showMsg('err', error); return }
    showMsg('ok', 'Role updated.')
    await loadUsers()
  }

  // ── Render ───────────────────────────────────────────────────────────

  if (loading && users.length === 0) {
    return <p className="text-slate-500 py-12 text-center">Loading users...</p>
  }

  return (
    <>
      {message && (
        <div className={`mb-6 px-4 py-3 rounded-2xl ${message.type === 'ok' ? 'bg-pastelGreen text-green-800' : 'bg-softPeach text-red-800'}`}>
          {message.text}
        </div>
      )}

      <section className="bg-white rounded-[2rem] p-8 card-shadow border border-slate-50">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
            <span className="material-symbols-outlined text-blue-600 text-xl">group</span>
          </div>
          <h2 className="text-xl font-bold">Manage Users</h2>
        </div>

        {/* Search */}
        <div className="flex flex-wrap gap-4 mb-6">
          <input
            type="text"
            placeholder="Search by email or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="px-4 py-2 rounded-xl border border-slate-200 max-w-xs"
          />
        </div>

        {/* Add user form */}
        <form onSubmit={handleAddUser} className="flex flex-wrap gap-4 mb-8 items-end">
          <div>
            <label className="block text-sm font-bold text-slate-500 mb-1">Email</label>
            <input
              type="email"
              required
              value={addEmail}
              onChange={(e) => setAddEmail(e.target.value)}
              placeholder="user@company.com"
              className="px-4 py-2 rounded-xl border border-slate-200"
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-slate-500 mb-1">Name</label>
            <input
              type="text"
              value={addName}
              onChange={(e) => setAddName(e.target.value)}
              placeholder="Optional"
              className="px-4 py-2 rounded-xl border border-slate-200"
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-slate-500 mb-1">Role</label>
            <select
              value={addRole}
              onChange={(e) => setAddRole(e.target.value as 'USER' | 'ADMIN')}
              className="px-4 py-2 rounded-xl border border-slate-200"
            >
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <label className="flex items-center gap-2 pb-1">
            <input type="checkbox" checked={addSsoEnabled} onChange={(e) => setAddSsoEnabled(e.target.checked)} />
            <span className="text-sm font-medium">SSO enabled</span>
          </label>
          <button type="submit" className="bg-primary text-white px-6 py-2.5 rounded-2xl font-semibold hover:shadow-lg transition-shadow">
            Add / Enable user
          </button>
        </form>

        {/* Users table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-slate-200">
                <th className="pb-2 font-bold text-slate-600">Email</th>
                <th className="pb-2 font-bold text-slate-600">Name</th>
                <th className="pb-2 font-bold text-slate-600">Role</th>
                <th className="pb-2 font-bold text-slate-600">SSO</th>
                <th className="pb-2 font-bold text-slate-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-slate-100">
                  <td className="py-3">{u.email}</td>
                  <td className="py-3">{u.name ?? '\u2014'}</td>
                  <td className="py-3">
                    {u.email.toLowerCase() === SUPER_ADMIN_EMAIL ? (
                      <span className="font-semibold">SUPER_ADMIN</span>
                    ) : (
                      <select
                        value={u.role}
                        onChange={(e) => handleSetRole(u.id, u.email, e.target.value)}
                        className="rounded-lg border border-slate-200 px-2 py-1"
                      >
                        <option value="USER">USER</option>
                        <option value="ADMIN">ADMIN</option>
                        <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                      </select>
                    )}
                  </td>
                  <td className="py-3">{u.ssoEnabled ? 'Yes' : 'No'}</td>
                  <td className="py-3">
                    {u.email.toLowerCase() !== SUPER_ADMIN_EMAIL && (
                      <button
                        type="button"
                        onClick={() => handleRemoveUser(u.id, u.email)}
                        className="text-accent hover:underline text-sm"
                      >
                        Remove
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">No users found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
