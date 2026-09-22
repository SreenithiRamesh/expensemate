import { QueryClient } from '@tanstack/react-query'

function shouldRetry(failureCount, error) {
    const status = error?.response?.status

    if (
        status === 400 ||
        status === 401 ||
        status === 403 ||
        status === 404 ||
        status === 409 ||
        status === 422
    ) {
        return false
    }

    return failureCount < 2
}

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            gcTime: 5 * 60_000,
            retry: shouldRetry,
            retryDelay: (attemptIndex) =>
                Math.min(1_000 * 2 ** attemptIndex, 8_000),
            refetchOnWindowFocus: false,
            refetchOnReconnect: true,
        },

        mutations: {
            retry: false,
        },
    },
})