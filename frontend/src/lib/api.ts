const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const ACCESS_TOKEN_KEY = "docshare_access_token";
const REFRESH_TOKEN_KEY = "docshare_refresh_token";

// SECURITY NOTE: tokens are kept in localStorage for simplicity in this
// phase. That's readable by any script running on the page (XSS risk) -
// the more robust pattern (refresh token in an httpOnly cookie, invisible
// to JS) needs backend cookie support that doesn't exist yet. Flagged here
// as a Production Readiness hardening item, not a design endorsement.

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken: string) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export class ApiError extends Error {
  constructor(
    message: string,
    public code: string,
    public status: number,
    public traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

interface ErrorEnvelope {
  error: { code: string; message: string; traceId: string };
}

let refreshInFlight: Promise<boolean> | null = null;

/** Exchanges the stored refresh token for a new pair. Single-flight: if
 * several requests 401 at once, only one refresh call actually happens. */
async function refreshAccessToken(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;

    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      clearTokens();
      return false;
    }

    const body = await response.json();
    setTokens(body.accessToken, body.refreshToken);
    return true;
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

interface RequestOptions extends RequestInit {
  /** Set true to skip attaching the Authorization header (auth endpoints). */
  skipAuth?: boolean;
}

/**
 * Fetch wrapper used by every API call in this app. Attaches the bearer
 * token automatically, and on a 401 (except for auth endpoints themselves)
 * attempts exactly one silent refresh-and-retry before giving up - this is
 * what lets a 15-minute access token feel invisible to the person using
 * the app instead of logging them out constantly.
 */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth, headers, ...rest } = options;

  const doFetch = async (): Promise<Response> => {
    const finalHeaders = new Headers(headers);
    if (!skipAuth) {
      const token = getAccessToken();
      if (token) finalHeaders.set("Authorization", `Bearer ${token}`);
    }
    return fetch(`${API_BASE_URL}${path}`, { ...rest, headers: finalHeaders });
  };

  let response = await doFetch();

  if (response.status === 401 && !skipAuth) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      response = await doFetch();
    }
  }

  if (!response.ok) {
    let envelope: ErrorEnvelope | null = null;
    try {
      envelope = await response.json();
    } catch {
      // Non-JSON error body (e.g. a proxy/network error page) - fall
      // through to the generic message below.
    }
    const error = envelope?.error;
    throw new ApiError(
      error?.message ?? `Request failed with status ${response.status}`,
      error?.code ?? "UNKNOWN_ERROR",
      response.status,
      error?.traceId,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

/** For multipart uploads, where the body must not be JSON-stringified. */
export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  return apiFetch<T>(path, { method: "POST", body: formData });
}
