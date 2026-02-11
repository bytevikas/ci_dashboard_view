import { useState, useEffect, useCallback } from 'react'

const STORAGE_KEY = 'recentSearches'
const MAX_RECENT = 5

export function useRecentSearches() {
  const [recentSearches, setRecentSearches] = useState<string[]>([])

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      try {
        setRecentSearches(JSON.parse(stored))
      } catch {
        setRecentSearches([])
      }
    }
  }, [])

  const addSearch = useCallback((regNo: string) => {
    const normalized = regNo.trim().toUpperCase()
    if (!normalized) return
    
    setRecentSearches((prev) => {
      const filtered = prev.filter((s) => s !== normalized)
      const updated = [normalized, ...filtered].slice(0, MAX_RECENT)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
      return updated
    })
  }, [])

  const removeSearch = useCallback((regNo: string) => {
    setRecentSearches((prev) => {
      const updated = prev.filter((s) => s !== regNo)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
      return updated
    })
  }, [])

  return { recentSearches, addSearch, removeSearch }
}
