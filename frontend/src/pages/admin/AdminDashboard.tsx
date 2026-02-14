import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api/client'
import type { SearchStats, SearchLogEntry, PagedSearchLogs } from '../../api/client'

// ── Helpers ──────────────────────────────────────────────────────────

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  return `${days}d ago`
}

const OUTCOME_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  SUCCESS:             { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Success' },
  CACHE_HIT:           { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Cached' },
  NO_DATA:             { bg: 'bg-red-50',     text: 'text-red-700',     label: 'No Data' },
  API_ERROR:           { bg: 'bg-red-50',     text: 'text-red-700',     label: 'API Error' },
  RATE_LIMITED:        { bg: 'bg-amber-50',   text: 'text-amber-700',   label: 'Rate Limited' },
  COOLDOWN:            { bg: 'bg-amber-50',   text: 'text-amber-700',   label: 'Cooldown' },
  DAILY_LIMIT_REACHED: { bg: 'bg-amber-50',   text: 'text-amber-700',   label: 'Daily Limit' },
}

function OutcomeBadge({ outcome }: { outcome: string }) {
  const style = OUTCOME_STYLES[outcome] ?? { bg: 'bg-slate-100', text: 'text-slate-600', label: outcome }
  return (
    <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold ${style.bg} ${style.text}`}>
      {style.label}
    </span>
  )
}

function StatCard({ icon, label, value, color }: { icon: string; label: string; value: number; color: string }) {
  const colorMap: Record<string, { bg: string; iconColor: string }> = {
    primary: { bg: 'bg-primary/10', iconColor: 'text-primary' },
    emerald: { bg: 'bg-emerald-50', iconColor: 'text-emerald-600' },
    blue:    { bg: 'bg-blue-50',    iconColor: 'text-blue-600' },
    purple:  { bg: 'bg-purple-50',  iconColor: 'text-purple-600' },
  }
  const c = colorMap[color] ?? colorMap.primary
  return (
    <div className="bg-slate-50 rounded-xl p-4 flex items-center gap-4">
      <div className={`w-10 h-10 ${c.bg} rounded-xl flex items-center justify-center`}>
        <span className={`material-symbols-outlined ${c.iconColor} text-xl`}>{icon}</span>
      </div>
      <div>
        <p className="text-2xl font-bold text-slate-900">{value.toLocaleString()}</p>
        <p className="text-xs font-medium text-slate-500">{label}</p>
      </div>
    </div>
  )
}

// ── Component ────────────────────────────────────────────────────────

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true)
  const [searchStats, setSearchStats] = useState<SearchStats | null>(null)
  const [searchLogs, setSearchLogs] = useState<SearchLogEntry[]>([])
  const [searchLogsPage, setSearchLogsPage] = useState(0)
  const [searchLogsHasMore, setSearchLogsHasMore] = useState(false)
  const [searchLogsLoading, setSearchLogsLoading] = useState(false)

  const loadSearchStats = useCallback(async () => {
    const { data } = await api<SearchStats>('/admin/search-stats')
    if (data) setSearchStats(data)
  }, [])

  const loadSearchLogs = useCallback(async (page: number, append: boolean) => {
    setSearchLogsLoading(true)
    try {
      const { data } = await api<PagedSearchLogs>(`/admin/search-logs?page=${page}&size=20`)
      if (data) {
        setSearchLogs((prev) => (append ? [...prev, ...data.content] : data.content))
        setSearchLogsPage(data.number)
        setSearchLogsHasMore(!data.last)
      }
    } finally {
      setSearchLogsLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function run() {
      setLoading(true)
      await Promise.all([loadSearchStats(), loadSearchLogs(0, false)])
      if (!cancelled) setLoading(false)
    }
    run()
    return () => { cancelled = true }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return <p className="text-slate-500 py-12 text-center">Loading dashboard...</p>
  }

  return (
    <section className="bg-white rounded-[2rem] p-8 card-shadow border border-slate-50">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
          <span className="material-symbols-outlined text-primary text-xl filled-icon">monitoring</span>
        </div>
        <h2 className="text-xl font-bold">Search Activity</h2>
      </div>

      {/* Stats cards */}
      {searchStats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <StatCard icon="search" label="Total Searches" value={searchStats.totalSearches} color="primary" />
          <StatCard icon="today" label="Today" value={searchStats.todaySearches} color="emerald" />
          <StatCard icon="group" label="Unique Users" value={searchStats.uniqueUsers} color="blue" />
          <StatCard icon="directions_car" label="Unique Reg Numbers" value={searchStats.uniqueRegNumbers} color="purple" />
        </div>
      )}

      {/* Top Searchers + Recent Searches */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {searchStats && searchStats.topSearchers.length > 0 && (
          <div className="lg:col-span-1">
            <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wide mb-3">Top Searchers</h3>
            <div className="bg-slate-50 rounded-xl overflow-hidden">
              {searchStats.topSearchers.map((s, i) => (
                <div key={s.email} className="flex items-center gap-3 px-4 py-3 border-b border-slate-100 last:border-b-0">
                  <span
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                      i === 0 ? 'bg-amber-100 text-amber-700'
                        : i === 1 ? 'bg-slate-200 text-slate-600'
                        : i === 2 ? 'bg-orange-100 text-orange-700'
                        : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {i + 1}
                  </span>
                  <span className="flex-1 text-sm font-medium text-slate-700 truncate">{s.email}</span>
                  <span className="text-sm font-bold text-slate-900">{s.count}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className={searchStats && searchStats.topSearchers.length > 0 ? 'lg:col-span-2' : 'lg:col-span-3'}>
          <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wide mb-3">Recent Searches</h3>
          {searchLogs.length === 0 ? (
            <p className="text-slate-400 text-sm py-6 text-center">No search logs yet.</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-slate-200">
                      <th className="pb-2 font-bold text-slate-500">Time</th>
                      <th className="pb-2 font-bold text-slate-500">User</th>
                      <th className="pb-2 font-bold text-slate-500">Reg Number</th>
                      <th className="pb-2 font-bold text-slate-500">Outcome</th>
                    </tr>
                  </thead>
                  <tbody>
                    {searchLogs.map((entry) => (
                      <tr key={entry.id} className="border-b border-slate-50 hover:bg-slate-50/50">
                        <td className="py-2.5 text-slate-500 whitespace-nowrap" title={new Date(entry.createdAt).toLocaleString()}>
                          {timeAgo(entry.createdAt)}
                        </td>
                        <td className="py-2.5 text-slate-700 font-medium truncate max-w-[200px]">{entry.userEmail}</td>
                        <td className="py-2.5 font-mono text-slate-800">{entry.registrationNumber || '\u2014'}</td>
                        <td className="py-2.5"><OutcomeBadge outcome={entry.details} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {searchLogsHasMore && (
                <div className="mt-4 text-center">
                  <button
                    type="button"
                    disabled={searchLogsLoading}
                    onClick={() => loadSearchLogs(searchLogsPage + 1, true)}
                    className="px-5 py-2 bg-slate-100 text-slate-600 text-sm font-semibold rounded-xl hover:bg-slate-200 transition-colors disabled:opacity-50"
                  >
                    {searchLogsLoading ? 'Loading...' : 'Load More'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </section>
  )
}
