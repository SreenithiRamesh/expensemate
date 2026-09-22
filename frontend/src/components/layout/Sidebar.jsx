import {
    Activity,
    BarChart3,
    Bot,
    ChevronLeft,
    ChevronRight,
    LayoutDashboard,
    ReceiptText,
    Repeat2,
    Users,
    WalletCards,
    X,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

import { useUi } from '../../hooks/useUi'
import { cn } from '../../utils/cn'

const navigationItems = [
    {
        label: 'Dashboard',
        to: '/app/dashboard',
        icon: LayoutDashboard,
    },
    {
        label: 'Expenses',
        to: '/app/expenses',
        icon: ReceiptText,
    },
    {
        label: 'Budgets',
        to: '/app/budgets',
        icon: BarChart3,
    },
    {
        label: 'Recurring',
        to: '/app/recurring',
        icon: Repeat2,
    },
    {
        label: 'Groups',
        to: '/app/groups',
        icon: Users,
    },
    {
        label: 'Activity',
        to: '/app/activity',
        icon: Activity,
    },
    {
        label: 'AI insights',
        to: '/app/insights',
        icon: Bot,
    },
]

export function Sidebar() {
    const {
        isSidebarOpen,
        isSidebarCollapsed,
        closeSidebar,
        toggleSidebarCollapsed,
    } = useUi()

    return (
        <>
            {isSidebarOpen && (
                <button
                    type="button"
                    aria-label="Close navigation"
                    onClick={closeSidebar}
                    className="fixed inset-0 z-40 bg-slate-950/35 backdrop-blur-sm lg:hidden"
                />
            )}

            <aside
                className={cn(
                    'fixed inset-y-0 left-0 z-50 flex flex-col border-r border-white/70 bg-white/85 shadow-xl shadow-brand-900/5 backdrop-blur-2xl transition-all duration-300 lg:translate-x-0',
                    isSidebarOpen
                        ? 'translate-x-0'
                        : '-translate-x-full',
                    isSidebarCollapsed
                        ? 'w-24'
                        : 'w-72',
                )}
            >
                <div className="flex h-20 items-center justify-between px-5">
                    <NavLink
                        to="/"
                        className="flex items-center gap-3"
                        onClick={closeSidebar}
                    >
                        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-brand-500 via-brand-600 to-brand-800 text-white shadow-lg shadow-brand-600/25">
                            <WalletCards size={22} />
                        </span>

                        {!isSidebarCollapsed && (
                            <span className="text-xl font-black text-heading">
                                Expense
                                <span className="text-brand-600">
                                    Mate
                                </span>
                            </span>
                        )}
                    </NavLink>

                    <button
                        type="button"
                        aria-label="Close sidebar"
                        onClick={closeSidebar}
                        className="grid h-10 w-10 place-items-center rounded-xl text-slate-500 hover:bg-brand-50 lg:hidden"
                    >
                        <X size={20} />
                    </button>
                </div>

                <nav
                    aria-label="Primary navigation"
                    className="flex-1 space-y-2 overflow-y-auto px-4 py-5"
                >
                    {navigationItems.map((item) => {
                        const Icon = item.icon

                        return (
                            <NavLink
                                key={item.to}
                                to={item.to}
                                onClick={closeSidebar}
                                className={({ isActive }) =>
                                    cn(
                                        'flex min-h-12 items-center gap-3 rounded-2xl px-4 font-semibold transition-all',
                                        isActive
                                            ? 'bg-linear-to-r from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/20'
                                            : 'text-slate-600 hover:bg-brand-50 hover:text-brand-800',
                                        isSidebarCollapsed &&
                                        'justify-center px-0',
                                    )
                                }
                            >
                                <Icon
                                    size={20}
                                    className="shrink-0"
                                />

                                {!isSidebarCollapsed && (
                                    <span>{item.label}</span>
                                )}
                            </NavLink>
                        )
                    })}
                </nav>

                <div className="hidden border-t border-slate-100 p-4 lg:block">
                    <button
                        type="button"
                        onClick={toggleSidebarCollapsed}
                        className={cn(
                            'flex min-h-11 w-full items-center gap-3 rounded-xl px-4 font-semibold text-slate-500 transition hover:bg-brand-50 hover:text-brand-700',
                            isSidebarCollapsed &&
                            'justify-center px-0',
                        )}
                    >
                        {isSidebarCollapsed ? (
                            <ChevronRight size={20} />
                        ) : (
                            <>
                                <ChevronLeft size={20} />
                                Collapse sidebar
                            </>
                        )}
                    </button>
                </div>
            </aside>
        </>
    )
}