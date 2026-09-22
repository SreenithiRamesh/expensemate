import {
    Bell,
    Menu,
    Search,
} from 'lucide-react'

import { useUi } from '../../hooks/useUi'

export function Header() {
    const { openSidebar } = useUi()

    return (
        <header className="sticky top-0 z-30 flex h-20 items-center gap-4 border-b border-white/70 bg-white/65 px-5 backdrop-blur-2xl sm:px-7">
            <button
                type="button"
                aria-label="Open navigation"
                onClick={openSidebar}
                className="grid h-11 w-11 shrink-0 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm lg:hidden"
            >
                <Menu size={21} />
            </button>

            <div className="relative hidden max-w-md flex-1 md:block">
                <Search
                    size={18}
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
                <button
                    type="button"
                    aria-label="Notifications"
                    className="relative grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-brand-200 hover:text-brand-700"
                >
                    <Bell size={20} />
                </button>

                <button
                    type="button"
                    className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white py-2 pr-4 pl-2 shadow-sm"
                >
                    <span className="grid h-8 w-8 place-items-center rounded-xl bg-linear-to-br from-brand-600 to-sky-500 text-sm font-black text-white">
                        S
                    </span>

                    <span className="hidden text-left sm:block">
                        <span className="block text-sm font-bold text-heading">
                            Sree
                        </span>

                        <span className="block text-xs text-slate-500">
                            ExpenseMate user
                        </span>
                    </span>
                </button>
            </div>
        </header>
    )
}