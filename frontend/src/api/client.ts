const API_BASE = '/api';
const DEFAULT_TIMEOUT_MS = 15000;

function getToken(): string | null {
  return sessionStorage.getItem('token');
}

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<{ data?: T; status: number; error?: string }> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }
  const timeoutMs = (options as RequestInit & { timeout?: number }).timeout ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  
  let res: Response;
  try {
    res = await fetch(API_BASE + path, {
      ...options,
      headers,
      credentials: 'include',
      signal: options.signal ?? controller.signal,
    });
  } catch (err) {
    clearTimeout(timeoutId);
    if (err instanceof Error) {
      if (err.name === 'AbortError') {
        return { status: 0, error: 'Request timed out. Check that the backend is running (default: http://localhost:8081) and try again.' };
      }
      if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
        return { status: 0, error: 'Cannot reach the backend. Start it with: cd backend && mvn spring-boot:run (see RUN.md)' };
      }
      return { status: 0, error: `Connection error: ${err.message}` };
    }
    return { status: 0, error: 'An unexpected error occurred' };
  } finally {
    clearTimeout(timeoutId);
  }

  let data: T | undefined;
  let error: string | undefined;
  
  try {
    const text = await res.text();
    if (text) {
      try {
        data = JSON.parse(text) as T;
        if (data && typeof data === 'object' && 'errorMessage' in data) {
          error = (data as { errorMessage?: string }).errorMessage;
        }
        if (data && typeof data === 'object' && 'error' in data) {
          error = (data as { error?: string }).error;
        }
      } catch {
        error = text || 'Request failed';
      }
    }
  } catch {
    error = 'Failed to read server response';
  }

  if (!res.ok) {
    if (!error) {
      if (res.status === 401) error = 'Unauthorized';
      else if (res.status === 403) error = 'Forbidden - you do not have permission';
      else if (res.status === 429) error = 'Too many requests - please slow down';
      else if (res.status === 500) error = 'Server error - the service is temporarily unavailable';
      else if (res.status === 502 || res.status === 503 || res.status === 504) error = 'Service unavailable - please try again later';
      else error = `Request failed (${res.status})`;
    }
    // Don't return error response bodies as data — callers expect the success type
    data = undefined;
  }
  return { data, status: res.status, error };
}

export type UserInfo = {
  id: string;
  email: string;
  name: string | null;
  pictureUrl: string | null;
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN';
  ssoEnabled: boolean;
};

export type VehicleSearchResponse = {
  success: boolean;
  fromCache?: boolean;
  registrationNumber?: string;
  data?: Record<string, unknown>;
  errorMessage?: string;
};

export type RateLimitInfo = {
  remainingSearchesToday: number;
  dailyLimit: number;
  adminConfigured: boolean;
};

export type UnmaskResponse = {
  registrationNumber: string;
};

// ── Admin search dashboard types ──────────────────────────────────────

export type TopSearcher = {
  email: string;
  count: number;
};

export type SearchStats = {
  totalSearches: number;
  todaySearches: number;
  uniqueUsers: number;
  uniqueRegNumbers: number;
  topSearchers: TopSearcher[];
};

export type SearchLogEntry = {
  id: string;
  userId: string;
  userEmail: string;
  registrationNumber: string;
  details: string; // outcome: SUCCESS, CACHE_HIT, NO_DATA, API_ERROR, RATE_LIMITED, etc.
  fromCache: boolean;
  createdAt: string;
};

export type PagedSearchLogs = {
  content: SearchLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-based)
  size: number;
  last: boolean;
};

/**
 * Unmask a registration number. This is an audited action – the user must
 * acknowledge the sensitive-data warning before this is called.
 */
export async function unmaskRegistrationNumber(
  registrationNumber: string
): Promise<{ data?: UnmaskResponse; error?: string }> {
  const { data, error } = await api<UnmaskResponse>('/vehicle/unmask', {
    method: 'POST',
    body: JSON.stringify({ registrationNumber }),
  });
  return { data, error };
}
