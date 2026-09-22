import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'sonner'

import ErrorBoundary from '../components/common/ErrorBoundary'
import { UiProvider } from '../context/UiProvider'
import { queryClient } from '../lib/queryClient'

export function AppProviders({ children }) {
    return (
        <ErrorBoundary>
            <QueryClientProvider client={queryClient}>
                <UiProvider>
                    <BrowserRouter>
                        {children}

                        <Toaster
                            position="top-right"
                            richColors
                            closeButton
                        />
                    </BrowserRouter>
                </UiProvider>
            </QueryClientProvider>
        </ErrorBoundary>
    )
}