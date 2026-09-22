import { Outlet } from 'react-router-dom'

import { useServerHealth } from '../../features/server-health/useServerHealth'
import { useUi } from '../../hooks/useUi'
import { cn } from '../../utils/cn'
import { ServerStatusBanner } from '../common/ServerStatusBanner'
import { Header } from './Header'
import { Sidebar } from './Sidebar'

export function DashboardShell() {
    const { isSidebarCollapsed } = useUi()

    const {
        status,
        retry,
    } = useServerHealth()

    return (
        <div className="min-h-screen">
            <Sidebar />

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

                <Header />

                <main
                    id="main-content"
                    aria-label="Dashboard content"
                    className="px-5 py-7 sm:px-7 lg:px-9"
                >
                    <Outlet />
                </main>
            </div>
        </div>
    )
}
