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
        return { status: 0, error: 'Request timeout - please check your connection and try again' };
      }
      if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
        return { status: 0, error: 'Network error - unable to connect to server' };
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

  if (!res.ok && !error) {
    if (res.status === 401) error = 'Unauthorized';
    else if (res.status === 429) error = 'Too many requests - please slow down';
    else if (res.status === 500) error = 'Server error - the service is temporarily unavailable';
    else if (res.status === 502 || res.status === 503 || res.status === 504) error = 'Service unavailable - please try again later';
    else error = `Request failed (${res.status})`;
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

export type RateLimitInfo = { remainingSearchesToday: number };
