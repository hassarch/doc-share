"use client";

import { useEffect, ReactNode } from "react";
import { useRouter } from "next/navigation";
import { QueryClientProvider } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { queryClient } from "@/lib/query-client";
import { AppShell } from "@/components/layout/AppShell";

export default function AppLayout({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <AppShell>{children}</AppShell>
    </QueryClientProvider>
  );
}
