import {
    Activity,
    BarChart3,
    Bot,
    ChevronLeft,
    ChevronRight,
    LayoutDashboard,
    LoaderCircle,
    LogOut,
    ReceiptText,
    Repeat2,
    Users,
    WalletCards,
    X,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

import { useUi } from '../../hooks/useUi'
import { cn } from '../../utils/cn'
import { DashboardMascot } from '../dashboard/DashboardMascot'

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

function getUserName(user) {
    const name = user?.name?.trim()

    return name || 'ExpenseMate user'
}

function getUserEmail(user) {
    const email = user?.email?.trim()

    return email || 'Signed-in user'
}

function getInitial(name) {
    return name
        .charAt(0)
        .toUpperCase()
}

export function Sidebar({
                            user,
                            onLogout,
                            isLoggingOut = false,
                        }) {
    const {
        isSidebarOpen,
        isSidebarCollapsed,
        closeSidebar,
        toggleSidebarCollapsed,
    } = useUi()

    const userName = getUserName(user)
    const userEmail = getUserEmail(user)
    const initial = getInitial(userName)

    async function handleLogout() {
        closeSidebar()
        await onLogout?.()
    }

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
                        aria-label="ExpenseMate home"
                        className="flex items-center gap-3"
                        onClick={closeSidebar}
                    >
                        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-brand-500 via-brand-600 to-brand-800 text-white shadow-lg shadow-brand-600/25">
                            <WalletCards
                                size={22}
                                aria-hidden="true"
                            />
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
                        <X
                            size={20}
                            aria-hidden="true"
                        />
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
                                title={
                                    isSidebarCollapsed
                                        ? item.label
                                        : undefined
                                }
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
                                {({ isActive }) => (
                                    <>
                                        <Icon
                                            size={20}
                                            className="shrink-0"
                                            aria-hidden="true"
                                        />

                                        {!isSidebarCollapsed && (
                                            <span>
                                                {item.label}
                                            </span>
                                        )}

                                        <span className="sr-only">
                                            {isActive
                                                ? ' (current page)'
                                                : ''}
                                        </span>
                                    </>
                                )}
                            </NavLink>
                        )
                    })}
                </nav>

                {!isSidebarCollapsed && (
                    <div className="px-4 pb-4">
                        <div className="relative overflow-hidden rounded-2xl bg-linear-to-br from-brand-50 to-sky-50 p-4">
                            <DashboardMascot
                                variant="wallet"
                                className="absolute -right-3 -bottom-3 h-16 w-20 opacity-70"
                            />

                            <p className="relative max-w-[10rem] text-sm font-bold text-heading">
                                Your finances, organized
                            </p>

                            <p className="relative mt-1 max-w-[10rem] text-xs text-slate-500">
                                Track, split, and budget in one place.
                            </p>
                        </div>
                    </div>
                )}

                <div className="border-t border-slate-100 p-4">
                    <div
                        className={cn(
                            'flex items-center gap-3 rounded-xl px-2 py-2',
                            isSidebarCollapsed &&
                            'justify-center px-0',
                        )}
                    >
                        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-linear-to-br from-brand-600 to-sky-500 text-sm font-black text-white">
                            {initial}
                        </span>

                        {!isSidebarCollapsed && (
                            <div className="min-w-0">
                                <p className="truncate text-sm font-bold text-heading">
                                    {userName}
                                </p>

                                <p
                                    title={userEmail}
                                    className="truncate text-xs text-slate-500"
                                >
                                    {userEmail}
                                </p>
                            </div>
                        )}
                    </div>

                    <button
                        type="button"
                        aria-label={
                            isLoggingOut
                                ? 'Logging out'
                                : 'Log out'
                        }
                        title={
                            isSidebarCollapsed
                                ? 'Log out'
                                : undefined
                        }
                        disabled={isLoggingOut}
                        onClick={handleLogout}
                        className={cn(
                            'mt-2 flex min-h-11 w-full items-center gap-3 rounded-xl px-4 font-semibold text-slate-500 transition hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-60',
                            isSidebarCollapsed &&
                            'justify-center px-0',
                        )}
                    >
                        {isLoggingOut ? (
                            <LoaderCircle
                                size={18}
                                aria-hidden="true"
                                className="shrink-0 animate-spin motion-reduce:animate-none"
                            />
                        ) : (
                            <LogOut
                                size={18}
                                className="shrink-0"
                                aria-hidden="true"
                            />
                        )}

                        {!isSidebarCollapsed && (
                            <span>
                                {isLoggingOut
                                    ? 'Logging out…'
                                    : 'Log out'}
                            </span>
                        )}
                    </button>

                    <button
                        type="button"
                        onClick={toggleSidebarCollapsed}
                        aria-label={
                            isSidebarCollapsed
                                ? 'Expand sidebar'
                                : 'Collapse sidebar'
                        }
                        className={cn(
                            'mt-1 hidden min-h-11 w-full items-center gap-3 rounded-xl px-4 font-semibold text-slate-500 transition hover:bg-brand-50 hover:text-brand-700 lg:flex',
                            isSidebarCollapsed &&
                            'justify-center px-0',
                        )}
                    >
                        {isSidebarCollapsed ? (
                            <ChevronRight
                                size={20}
                                aria-hidden="true"
                            />
                        ) : (
                            <>
                                <ChevronLeft
                                    size={20}
                                    aria-hidden="true"
                                />
                                <span>
                                    Collapse sidebar
                                </span>
                            </>
                        )}
                    </button>
                </div>
            </aside>
        </>
    )
}