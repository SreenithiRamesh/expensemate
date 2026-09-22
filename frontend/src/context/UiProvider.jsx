import { useMemo, useState } from 'react'

import { UiContext } from './UiContext'

export function UiProvider({ children }) {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false)
    const [isSidebarCollapsed, setIsSidebarCollapsed] =
        useState(false)

    const value = useMemo(
        () => ({
            isSidebarOpen,
            isSidebarCollapsed,

            openSidebar: () => {
                setIsSidebarOpen(true)
            },

            closeSidebar: () => {
                setIsSidebarOpen(false)
            },

            toggleSidebar: () => {
                setIsSidebarOpen((current) => !current)
            },

            toggleSidebarCollapsed: () => {
                setIsSidebarCollapsed((current) => !current)
            },
        }),
        [
            isSidebarCollapsed,
            isSidebarOpen,
        ],
    )

    return (
        <UiContext.Provider value={value}>
            {children}
        </UiContext.Provider>
    )
}