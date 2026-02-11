import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, VehicleSearchResponse } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useSearch } from '../context/SearchContext'
import VehicleResult from '../components/VehicleResult'
import VehicleNoData from '../components/VehicleNoData'
import { trackSearch } from '../utils/ga'
import { groupDataBySection, SectionId, SECTION_LABELS } from '../utils/vehicleSections'

export default function Dashboard() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialQ = searchParams.get('q') ?? ''
  const { setRegistrationNumber, setOnSearch, setSearchLoading } = useSearch()
  const [result, setResult] = useState<VehicleSearchResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeFilter, setActiveFilter] = useState<SectionId | 'all'>('all')
  const [fieldSearch, setFieldSearch] = useState('')

  const doSearch = useCallback(async (regNo: string) => {
    if (!regNo.trim()) return
    setSearchLoading(true)
    setLoading(true)
    setResult(null)
    setSearchParams({ q: regNo.trim() })
    setRegistrationNumber(regNo.trim())

    const { data, status, error } = await api<VehicleSearchResponse>('/vehicle/search', {
      method: 'POST',
      body: JSON.stringify({ registrationNumber: regNo.trim() }),
    })

    setLoading(false)
    setSearchLoading(false)
    if (data) {
      setResult(data)
      trackSearch(regNo.trim(), data.success, data.fromCache ?? false)
    } else {
      setResult({
        success: false,
        errorMessage: error || (status === 429 ? 'Too many requests. Please try again later.' : 'Search failed.'),
      })
      trackSearch(regNo.trim(), false, false)
    }
  }, [setSearchParams, setSearchLoading, setRegistrationNumber])

  useEffect(() => {
    setOnSearch(() => doSearch)
    return () => setOnSearch(null)
  }, [doSearch, setOnSearch])

  useEffect(() => {
    if (initialQ && !result && !loading) {
      setRegistrationNumber(initialQ)
      doSearch(initialQ)
    }
  }, [initialQ, doSearch, setRegistrationNumber])

  const sections = result?.data ? groupDataBySection(result.data as Record<string, unknown>) : {}
  const sectionIds = Object.keys(sections) as SectionId[]

  return (
    <>
      <section className="px-6 pt-10 pb-6 max-w-6xl mx-auto">
        {!result && !loading && (
          <div className="relative overflow-hidden rounded-[3rem] bg-gradient-to-br from-indigo-50 to-purple-100 min-h-[400px] flex flex-col md:flex-row items-center px-8 md:px-16 py-12">
            <div className="flex-1 z-10 text-center md:text-left mb-10 md:mb-0">
              <span className="inline-block px-4 py-1.5 bg-white/60 backdrop-blur-sm text-primary font-bold text-xs rounded-full mb-4 tracking-wider uppercase">
                Your Car's Health Report
              </span>
              <h1 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4 leading-tight">
                Welcome, <br />
                <span className="text-primary">{user?.name?.split(' ')[0] ?? 'User'}!</span>
              </h1>
              <p className="text-slate-600 text-lg mb-8 max-w-md">
                Enter a vehicle registration number above to view RC, insurance, PUC and other details.
              </p>
            </div>
            <div className="flex-1 relative w-full h-full flex justify-center">
              <span className="material-symbols-outlined text-slate-200 text-[12rem] filled-icon">directions_car</span>
            </div>
          </div>
        )}

        {result && !result.success && (
          <VehicleNoData
            message={result.errorMessage || 'No data found for this registration number.'}
            onTryAgain={() => {
              setResult(null)
              setRegistrationNumber('')
              setSearchParams({})
            }}
          />
        )}

        {result?.success && result.data && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-primary/10 rounded-xl flex items-center justify-center">
                  <span className="material-symbols-outlined text-primary text-2xl filled-icon">directions_car</span>
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-slate-900">
                    {result.registrationNumber}
                    {result.fromCache && (
                      <span className="ml-2 text-xs font-medium text-slate-400 bg-slate-100 px-2 py-1 rounded">cached</span>
                    )}
                  </h1>
                  <p className="text-sm text-slate-500">Vehicle Information</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  setResult(null)
                  setRegistrationNumber('')
                  setSearchParams({})
                }}
                className="px-4 py-2 bg-slate-100 text-slate-600 font-medium rounded-xl hover:bg-slate-200 transition-colors text-sm"
              >
                <span className="material-symbols-outlined text-sm align-middle mr-1">search</span>
                New Search
              </button>
            </div>

            <div className="flex flex-col gap-4 mb-6">
              <div className="relative w-full max-w-md">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-lg">search</span>
                <input
                  type="text"
                  placeholder="Quick search fields (e.g. PUC, insurance, owner)"
                  value={fieldSearch}
                  onChange={(e) => setFieldSearch(e.target.value)}
                  className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary/30"
                />
                {fieldSearch && (
                  <button
                    type="button"
                    onClick={() => setFieldSearch('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    <span className="material-symbols-outlined text-lg">close</span>
                  </button>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => { setActiveFilter('all'); setFieldSearch(''); }}
                  className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
                    activeFilter === 'all' && !fieldSearch
                      ? 'bg-primary text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  All
                </button>
                {sectionIds.map((sectionId) => (
                  <button
                    key={sectionId}
                    type="button"
                    onClick={() => { setActiveFilter(sectionId); setFieldSearch(''); }}
                    className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
                      activeFilter === sectionId && !fieldSearch
                        ? 'bg-primary text-white'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {SECTION_LABELS[sectionId] ?? sectionId}
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {sectionIds.map((sectionId) => {
                if (activeFilter !== 'all' && activeFilter !== sectionId && !fieldSearch) return null
                
                // Filter data based on field search
                let filteredData = sections[sectionId]
                if (fieldSearch) {
                  const searchLower = fieldSearch.toLowerCase()
                  filteredData = Object.fromEntries(
                    Object.entries(sections[sectionId]).filter(([key, value]) => {
                      const keyMatch = key.toLowerCase().includes(searchLower)
                      const valueMatch = String(value).toLowerCase().includes(searchLower)
                      const labelMatch = key.replace(/([A-Z])/g, ' $1').toLowerCase().includes(searchLower)
                      return keyMatch || valueMatch || labelMatch
                    })
                  )
                  if (Object.keys(filteredData).length === 0) return null
                }
                
                return (
                  <VehicleResult
                    key={sectionId}
                    sectionId={sectionId}
                    data={filteredData}
                    highlightTerm={fieldSearch}
                  />
                )
              })}
            </div>
          </>
        )}
      </section>
      <footer className="bg-white border-t border-slate-100 py-8 px-6 mt-12">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 text-center md:text-left">
          <div className="flex items-center gap-3 text-slate-400">
            <span className="material-symbols-outlined text-2xl">verified</span>
            <span className="text-sm font-semibold">Data powered by CarInfo API</span>
          </div>
          <p className="text-sm text-slate-400">© Vehicle Health Dashboard</p>
        </div>
      </footer>
    </>
  )
}
