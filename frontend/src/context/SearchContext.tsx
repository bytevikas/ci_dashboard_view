import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, RateLimitInfo } from '../api/client'

type SearchContextType = {
  registrationNumber: string
  setRegistrationNumber: (v: string) => void
  onSearch: ((regNo: string) => void) | null
  setOnSearch: (fn: ((regNo: string) => void) | null) => void
  searchLoading: boolean
  setSearchLoading: (v: boolean) => void
  remainingSearches: number | null
  dailyLimit: number | null
  refreshRateLimit: () => Promise<void>
}

const SearchContext = createContext<SearchContextType | null>(null)

export function SearchProvider({ children }: { children: React.ReactNode }) {
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [onSearch, setOnSearch] = useState<((regNo: string) => void) | null>(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [remainingSearches, setRemainingSearches] = useState<number | null>(null)
  const [dailyLimit, setDailyLimit] = useState<number | null>(null)

  const refreshRateLimit = useCallback(async () => {
    const { data } = await api<RateLimitInfo>('/vehicle/rate-limit')
    if (data) {
      if (data.adminConfigured) {
        setRemainingSearches(data.remainingSearchesToday)
        setDailyLimit(data.dailyLimit)
      } else {
        // Admin hasn't configured a limit — don't show it
        setRemainingSearches(null)
        setDailyLimit(null)
      }
    }
  }, [])

  // Fetch on mount
  useEffect(() => {
    refreshRateLimit()
  }, [refreshRateLimit])

  return (
    <SearchContext.Provider
      value={{
        registrationNumber,
        setRegistrationNumber,
        onSearch,
        setOnSearch,
        searchLoading,
        setSearchLoading,
        remainingSearches,
        dailyLimit,
        refreshRateLimit,
      }}
    >
      {children}
    </SearchContext.Provider>
  )
}

export function useSearch() {
  const ctx = useContext(SearchContext)
  if (!ctx) throw new Error('useSearch must be used within SearchProvider')
  return ctx
}
