/**
 * GA events for search.
 * Set VITE_GA_MEASUREMENT_ID in .env to enable.
 */

const MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID as string | undefined

export function trackSearch(registrationNumber: string, success: boolean, fromCache: boolean) {
  if (typeof window === 'undefined' || !MEASUREMENT_ID) return
  const gtag = window.gtag
  if (gtag) {
    gtag('event', 'search', {
      search_term: registrationNumber,
      success,
      from_cache: fromCache,
    })
  }
}
