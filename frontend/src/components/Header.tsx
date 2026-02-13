import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSearch } from '../context/SearchContext'
import { useRecentSearches } from '../hooks/useRecentSearches'

export default function Header() {
  const { registrationNumber, setRegistrationNumber, onSearch, searchLoading, remainingSearches, dailyLimit } = useSearch()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const { recentSearches, addSearch, removeSearch } = useRecentSearches()
  const [showDropdown, setShowDropdown] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = registrationNumber.trim()
    if (trimmed) {
      addSearch(trimmed)
      if (onSearch) onSearch(trimmed)
      else navigate(`/?q=${encodeURIComponent(trimmed)}`)
    }
    setShowDropdown(false)
  }

  const handleQuickSearch = (regNo: string) => {
    setRegistrationNumber(regNo)
    addSearch(regNo)
    if (onSearch) onSearch(regNo)
    else navigate(`/?q=${encodeURIComponent(regNo)}`)
    setShowDropdown(false)
  }

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  return (
    <header className="bg-white/80 backdrop-blur-md sticky top-0 z-40 border-b border-gray-100 px-6 py-4">
      <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        <div ref={dropdownRef} className="w-full max-w-xl relative">
          <form id="search-form" onSubmit={handleSubmit}>
            <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 z-10">
              search
            </span>
            <input
              className="w-full pl-12 pr-4 py-3 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-primary/20 text-base placeholder:text-slate-400"
              placeholder="Look up your car (e.g. MH12AB1234)"
              type="text"
              value={registrationNumber}
              onChange={(e) => setRegistrationNumber(e.target.value.toUpperCase())}
              onFocus={() => recentSearches.length > 0 && setShowDropdown(true)}
              disabled={searchLoading}
            />
          </form>
          {showDropdown && recentSearches.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden z-50">
              <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Recent Searches</span>
              </div>
              <div className="max-h-60 overflow-y-auto">
                {recentSearches.map((regNo) => (
                  <div
                    key={regNo}
                    className="flex items-center justify-between px-4 py-3 hover:bg-slate-50 cursor-pointer group"
                  >
                    <button
                      type="button"
                      onClick={() => handleQuickSearch(regNo)}
                      className="flex items-center gap-3 flex-1 text-left"
                    >
                      <span className="material-symbols-outlined text-slate-400 text-lg">history</span>
                      <span className="font-medium text-slate-700">{regNo}</span>
                    </button>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation()
                        removeSearch(regNo)
                      }}
                      className="text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                      title="Remove"
                    >
                      <span className="material-symbols-outlined text-lg">close</span>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="flex items-center gap-4">
          {remainingSearches !== null && dailyLimit !== null && (
            <div
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold ${
                remainingSearches > 20
                  ? 'bg-emerald-50 text-emerald-700'
                  : remainingSearches > 5
                    ? 'bg-amber-50 text-amber-700'
                    : 'bg-red-50 text-red-700'
              }`}
              title={`${remainingSearches} of ${dailyLimit} searches remaining today`}
            >
              <span className="material-symbols-outlined text-sm">bolt</span>
              {remainingSearches}/{dailyLimit} left today
            </div>
          )}
          {user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN' ? (
            <button
              type="button"
              onClick={() => navigate('/admin')}
              className="bg-white border border-gray-200 p-2.5 rounded-xl text-slate-600 hover:bg-slate-50 transition-colors"
              title="Admin"
            >
              <span className="material-symbols-outlined">settings</span>
            </button>
          ) : null}
          <button
            type="submit"
            form="search-form"
            onClick={(e) => {
              e.preventDefault()
              const trimmed = registrationNumber.trim()
              if (trimmed && onSearch) onSearch(trimmed)
            }}
            disabled={searchLoading || !registrationNumber.trim()}
            className="bg-primary text-white px-6 py-2.5 rounded-2xl font-semibold hover:brightness-110 hover:shadow-lg hover:shadow-primary/25 transition-all disabled:bg-slate-200 disabled:text-slate-400 disabled:shadow-none disabled:cursor-not-allowed"
          >
            {searchLoading ? 'Searching...' : 'Search Now'}
          </button>
          <button
            type="button"
            onClick={() => logout()}
            className="flex items-center gap-1.5 text-slate-500 hover:text-accent hover:bg-red-50 px-3 py-2 rounded-xl text-sm font-medium transition-colors"
            title="Logout"
          >
            <span className="material-symbols-outlined text-base">logout</span>
            Logout
          </button>
        </div>
      </div>
    </header>
  )
}
