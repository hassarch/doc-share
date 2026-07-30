import { apiFetch, setTokens, clearTokens } from "./api";

export interface UserResponse {
  id: string;
  email: string;
  name: string;
}

export interface TokenPairResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

export async function register(email: string, password: string, name: string) {
  return apiFetch<UserResponse>("/api/v1/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, name }),
    skipAuth: true,
  });
}

export async function login(email: string, password: string) {
  const tokens = await apiFetch<TokenPairResponse>("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  });
  setTokens(tokens.accessToken, tokens.refreshToken);
  return tokens;
}

export async function logout(refreshToken: string) {
  try {
    await apiFetch<void>("/api/v1/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } finally {
    // Clear local tokens even if the network call fails - the person
    // clicking "log out" expects to be logged out on this device
    // regardless of whether the server-side revocation round-trip
    // succeeded.
    clearTokens();
  }
}
