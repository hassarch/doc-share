"use client";

import { createContext, useContext, useState, ReactNode } from "react";
import { useRouter } from "next/navigation";
import { getAccessToken, getRefreshToken, clearTokens } from "@/lib/api";
import { login as apiLogin, logout as apiLogout, register as apiRegister } from "@/lib/auth-api";

interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Use lazy initializer to compute initial auth state without an effect
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()));
  const [isLoading] = useState(false); // Always false - auth check is synchronous
  const router = useRouter();

  async function login(email: string, password: string) {
    await apiLogin(email, password);
    setIsAuthenticated(true);
    router.push("/dashboard");
  }

  async function register(email: string, password: string, name: string) {
    await apiRegister(email, password, name);
    await login(email, password);
  }

  async function logout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      await apiLogout(refreshToken);
    } else {
      clearTokens();
    }
    setIsAuthenticated(false);
    router.push("/login");
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
