import {
    Bell,
    Menu,
    Search,
} from 'lucide-react'
import { useLocation } from 'react-router-dom'

import { SERVER_HEALTH } from '../../features/server-health/serverHealth'
import { useUi } from '../../hooks/useUi'
import { cn } from '../../utils/cn'

const sectionTitles = {
    '/app/dashboard': 'Dashboard',
    '/app/expenses': 'Expenses',
    '/app/budgets': 'Budgets',
    '/app/recurring': 'Recurring',
    '/app/groups': 'Groups',
    '/app/activity': 'Activity',
    '/app/insights': 'AI insights',
}

function getSectionTitle(pathname) {
    return sectionTitles[pathname] ?? 'Dashboard'
}

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

function getHealthPresentation(status) {
    switch (status) {
        case SERVER_HEALTH.AVAILABLE:
            return {
                label: 'Backend online',
                containerClass:
                    'border-emerald-100 bg-emerald-50 text-emerald-700',
                dotClass:
                    'animate-pulse bg-emerald-500 motion-reduce:animate-none',
            }

        case SERVER_HEALTH.CHECKING:
            return {
                label: 'Checking…',
                containerClass:
                    'border-slate-200 bg-slate-50 text-slate-600',
                dotClass:
                    'animate-pulse bg-slate-400 motion-reduce:animate-none',
            }

        case SERVER_HEALTH.WAKING:
            return {
                label: 'Backend waking up…',
                containerClass:
                    'border-amber-200 bg-amber-50 text-amber-700',
                dotClass:
                    'animate-pulse bg-amber-500 motion-reduce:animate-none',
            }

        case SERVER_HEALTH.UNAVAILABLE:
        default:
            return {
                label: 'Backend offline',
                containerClass:
                    'border-rose-100 bg-rose-50 text-rose-600',
                dotClass:
                    'bg-rose-500',
            }
    }
}

export function Header({
                           user,
                           status = SERVER_HEALTH.CHECKING,
                       }) {
    const { openSidebar } = useUi()
    const location = useLocation()

    const userName = getUserName(user)
    const userEmail = getUserEmail(user)
    const initial = getInitial(userName)

    const health =
        getHealthPresentation(status)

    return (
        <header className="sticky top-0 z-30 flex h-20 items-center gap-4 border-b border-white/70 bg-white/65 px-5 backdrop-blur-2xl sm:px-7">
            <button
                type="button"
                aria-label="Open navigation"
                onClick={openSidebar}
                className="grid h-11 w-11 shrink-0 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-brand-200 hover:text-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 lg:hidden"
            >
                <Menu
                    size={21}
                    aria-hidden="true"
                />
            </button>

            <div className="hidden shrink-0 lg:block">
                <h1 className="text-lg font-black text-heading">
                    {getSectionTitle(
                        location.pathname,
                    )}
                </h1>
            </div>

            <div className="relative hidden max-w-md flex-1 md:block">
                <Search
                    size={18}
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-slate-400"
                />

                <input
                    type="search"
                    aria-label="Search ExpenseMate"
                    placeholder="Search expenses, groups, activity..."
                    className="h-11 w-full rounded-2xl border border-slate-200 bg-white/80 pr-4 pl-11 text-sm outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                />
            </div>

            <div className="ml-auto flex items-center gap-3">
                <span
                    role="status"
                    aria-live="polite"
                    className={cn(
                        'hidden items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-bold sm:inline-flex',
                        health.containerClass,
                    )}
                >
                    <span
                        aria-hidden="true"
                        className={cn(
                            'h-2 w-2 rounded-full',
                            health.dotClass,
                        )}
                    />

                    {health.label}
                </span>

                <button
                    type="button"
                    aria-label="Notifications, 1 unread"
                    className="relative grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-brand-200 hover:text-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                >
                    <Bell
                        size={20}
                        aria-hidden="true"
                    />

                    <span
                        aria-hidden="true"
                        className="absolute top-2.5 right-2.5 h-2 w-2 animate-pulse rounded-full bg-rose-500 motion-reduce:animate-none"
                    />
                </button>

                <div
                    aria-label={`Signed in as ${userName}`}
                    title={userEmail}
                    className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white py-2 pr-4 pl-2 shadow-sm"
                >
                    <span className="grid h-8 w-8 place-items-center rounded-xl bg-linear-to-br from-brand-600 to-sky-500 text-sm font-black text-white">
                        {initial}
                    </span>

                    <span className="hidden min-w-0 text-left sm:block">
                        <span className="block max-w-36 truncate text-sm font-bold text-heading">
                            {userName}
                        </span>

                        <span className="block max-w-40 truncate text-xs text-slate-500">
                            {userEmail}
                        </span>
                    </span>
                </div>
            </div>
        </header>
    )
}