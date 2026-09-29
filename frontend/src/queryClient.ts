import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 minutes: fresh data without refetch spam
      gcTime: 1000 * 60 * 10,   // 10 minutes cache retention
      refetchOnWindowFocus: false, // avoid jarring refetches on window focus
      retry: 1,
    },
    mutations: {
      retry: 0,
    },
  },
});
