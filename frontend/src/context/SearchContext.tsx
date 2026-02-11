import React, { createContext, useCallback, useContext, useState } from 'react'

type SearchContextType = {
  registrationNumber: string
  setRegistrationNumber: (v: string) => void
  onSearch: ((regNo: string) => void) | null
  setOnSearch: (fn: ((regNo: string) => void) | null) => void
  searchLoading: boolean
  setSearchLoading: (v: boolean) => void
}

const SearchContext = createContext<SearchContextType | null>(null)

export function SearchProvider({ children }: { children: React.ReactNode }) {
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [onSearch, setOnSearch] = useState<((regNo: string) => void) | null>(null)
  const [searchLoading, setSearchLoading] = useState(false)
  return (
    <SearchContext.Provider
      value={{
        registrationNumber,
        setRegistrationNumber,
        onSearch,
        setOnSearch,
        searchLoading,
        setSearchLoading,
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
