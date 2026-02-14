import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api/client'

type AppConfig = {
  id?: string
  cacheTtlDays: number
  rateLimitPerSecond: number
  rateLimitPerDayDefault: number
  updatedAt?: string
  updatedBy?: string
}

export default function AdminConfig() {
  const [loading, setLoading] = useState(true)
  const [config, setConfig] = useState<AppConfig | null>(null)
  const [configForm, setConfigForm] = useState({ cacheTtlDays: 3, rateLimitPerSecond: 5, rateLimitPerDayDefault: 100 })
  const [message, setMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const showMsg = (type: 'ok' | 'err', text: string) => {
    setMessage({ type, text })
    setTimeout(() => setMessage(null), 4000)
  }

  const loadConfig = useCallback(async () => {
    const { data, error } = await api<AppConfig>('/admin/config')
    if (error) { showMsg('err', error); return }
    if (data) {
      setConfig(data)
      setConfigForm({
        cacheTtlDays: data.cacheTtlDays,
        rateLimitPerSecond: data.rateLimitPerSecond,
        rateLimitPerDayDefault: data.rateLimitPerDayDefault,
      })
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function run() {
      setLoading(true)
      await loadConfig()
      if (!cancelled) setLoading(false)
    }
    run()
    return () => { cancelled = true }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault()
    const { error } = await api('/admin/config', { method: 'PUT', body: JSON.stringify(configForm) })
    if (error) { showMsg('err', error); return }
    showMsg('ok', 'Config saved.')
    await loadConfig()
  }

  if (loading) {
    return <p className="text-slate-500 py-12 text-center">Loading configuration...</p>
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
          <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center">
            <span className="material-symbols-outlined text-purple-600 text-xl">tune</span>
          </div>
          <h2 className="text-xl font-bold">Manage Config</h2>
        </div>

        <form onSubmit={handleSaveConfig} className="space-y-6 max-w-2xl">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-sm font-bold text-slate-500 mb-2">Cache TTL (days)</label>
              <input
                type="number"
                min={1}
                max={30}
                value={configForm.cacheTtlDays}
                onChange={(e) => setConfigForm((c) => ({ ...c, cacheTtlDays: parseInt(e.target.value, 10) || 1 }))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors"
              />
              <p className="text-xs text-slate-400 mt-1">How long cached vehicle data is valid.</p>
            </div>
            <div>
              <label className="block text-sm font-bold text-slate-500 mb-2">Rate limit (per second)</label>
              <input
                type="number"
                min={1}
                max={20}
                value={configForm.rateLimitPerSecond}
                onChange={(e) => setConfigForm((c) => ({ ...c, rateLimitPerSecond: parseInt(e.target.value, 10) || 1 }))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors"
              />
              <p className="text-xs text-slate-400 mt-1">Max requests per user per second.</p>
            </div>
            <div>
              <label className="block text-sm font-bold text-slate-500 mb-2">Daily limit (per user)</label>
              <input
                type="number"
                min={1}
                max={1000}
                value={configForm.rateLimitPerDayDefault}
                onChange={(e) => setConfigForm((c) => ({ ...c, rateLimitPerDayDefault: parseInt(e.target.value, 10) || 1 }))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors"
              />
              <p className="text-xs text-slate-400 mt-1">Max searches per user in 24 hours.</p>
            </div>
          </div>

          <div className="flex items-center gap-4 pt-2">
            <button type="submit" className="bg-primary text-white px-6 py-2.5 rounded-2xl font-semibold hover:shadow-lg transition-shadow">
              Save config
            </button>
            {config?.updatedBy && (
              <p className="text-xs text-slate-400">
                Last updated by <span className="font-medium text-slate-500">{config.updatedBy}</span>
                {config.updatedAt && <> on {new Date(config.updatedAt).toLocaleDateString()}</>}
              </p>
            )}
          </div>
        </form>
      </section>
    </>
  )
}
