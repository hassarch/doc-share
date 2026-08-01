import { QueryClient } from "@tanstack/react-query";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 seconds
      retry: 1,
      refetchOnWindowFocus: false,
      // Don't throw errors to error boundaries, let components handle them
      throwOnError: false,
    },
    mutations: {
      retry: false,
      // Don't throw errors to error boundaries, let components handle them
      throwOnError: false,
    },
  },
});
