import { useState } from 'react'
import {
    Outlet,
    useNavigate,
} from 'react-router-dom'
import { toast } from 'sonner'

import { useServerHealth } from '../../features/server-health/useServerHealth'
import { useAuth } from '../../hooks/useAuth'
import { useUi } from '../../hooks/useUi'
import { cn } from '../../utils/cn'
import { ServerStatusBanner } from '../common/ServerStatusBanner'
import { Header } from './Header'
import { Sidebar } from './Sidebar'

export function DashboardShell() {
    const navigate = useNavigate()

    const { isSidebarCollapsed } = useUi()

    /*
     * AuthProvider exposes `user`, not `session`.
     */
    const {
        user,
        logout,
    } = useAuth()

    const {
        status,
        retry,
    } = useServerHealth()

    const [isLoggingOut, setIsLoggingOut] =
        useState(false)

    async function handleLogout() {
        if (isLoggingOut) {
            return
        }

        setIsLoggingOut(true)

        try {
            await logout()
        } catch {
            /*
             * AuthProvider clears the local session before calling
             * the backend. Therefore, the user is still securely
             * signed out locally if backend revocation fails.
             */
            toast.warning(
                'Signed out locally, but the server session could not be revoked.',
            )
        } finally {
            navigate(
                '/login',
                {
                    replace: true,
                },
            )
        }
    }

    return (
        <div className="min-h-screen">
            <Sidebar
                user={user}
                isLoggingOut={isLoggingOut}
                onLogout={handleLogout}
            />

            <div
                className={cn(
                    'min-h-screen transition-[margin] duration-300',
                    isSidebarCollapsed
                        ? 'lg:ml-24'
                        : 'lg:ml-72',
                )}
            >
                <ServerStatusBanner
                    status={status}
                    onRetry={retry}
                />

                <Header
                    user={user}
                    status={status}
                />

                <main
                    id="main-content"
                    aria-label="Dashboard content"
                    className="px-5 py-7 sm:px-7 lg:px-9"
                >
                    <Outlet
                        context={{
                            user,
                        }}
                    />
                </main>
            </div>
        </div>
    )
}