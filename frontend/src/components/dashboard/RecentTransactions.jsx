import {
    Bus,
    MoreHorizontal,
    Receipt,
    ShoppingBag,
    Split,
    Utensils,
} from 'lucide-react'

import { DashboardEmptyState } from './DashboardEmptyState'

const categoryIcon = {
    Food: Utensils,
    Travel: Bus,
    Shopping: ShoppingBag,
    Bills: Receipt,
}

const categoryTint = {
    Food: 'bg-brand-50 text-brand-700',
    Travel: 'bg-sky-50 text-sky-700',
    Shopping: 'bg-amber-50 text-amber-700',
    Bills: 'bg-emerald-50 text-emerald-700',
}

function formatCurrency(value) {
    return `₹${Math.abs(value).toLocaleString('en-IN')}`
}

export function RecentTransactions({ transactions = [], onViewAll }) {
    return (
        <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-heading">
                        Recent transactions
                    </h2>
                    <p className="mt-1 text-sm text-slate-500">
                        Your latest financial actions.
                    </p>
                </div>

                {transactions.length > 0 && (
                    <button
                        type="button"
                        onClick={onViewAll}
                        className="text-sm font-bold text-brand-700 hover:text-brand-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                    >
                        View all
                    </button>
                )}
            </div>

            {transactions.length === 0 ? (
                <div className="mt-6">
                    <DashboardEmptyState
                        variant="receipt"
                        title="No expenses yet"
                        description="Add your first expense and ExpenseMate will organize it here."
                    />
                </div>
            ) : (
                <ul className="mt-5 divide-y divide-slate-100">
                    {transactions.map((transaction) => {
                        const Icon = categoryIcon[transaction.category] ?? Receipt
                        const tint =
                            categoryTint[transaction.category] ??
                            'bg-slate-100 text-slate-600'

                        return (
                            <li
                                key={transaction.id}
                                className="group flex items-center gap-4 rounded-2xl px-2 py-3 transition hover:bg-brand-50/60"
                            >
                                <span
                                    className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl ${tint}`}
                                >
                                    <Icon size={18} aria-hidden="true" />
                                </span>

                                <div className="min-w-0 flex-1">
                                    <p className="truncate font-semibold text-heading">
                                        {transaction.title}
                                    </p>
                                    <p className="text-xs text-slate-500">
                                        {transaction.category} &middot; {transaction.date}
                                    </p>
                                </div>

                                {transaction.split && (
                                    <span
                                        className="hidden items-center gap-1 rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-500 sm:inline-flex"
                                        title="Split with group"
                                    >
                                        <Split size={12} aria-hidden="true" />
                                        Split
                                    </span>
                                )}

                                <p
                                    className={`shrink-0 text-right font-bold ${
                                        transaction.amount < 0
                                            ? 'text-rose-600'
                                            : 'text-emerald-600'
                                    }`}
                                >
                                    {transaction.amount < 0 ? '-' : '+'}
                                    {formatCurrency(transaction.amount)}
                                </p>

                                <button
                                    type="button"
                                    aria-label={`More options for ${transaction.title}`}
                                    className="grid h-8 w-8 shrink-0 place-items-center rounded-lg text-slate-400 opacity-0 transition hover:bg-white hover:text-brand-700 focus-visible:opacity-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 group-hover:opacity-100"
                                >
                                    <MoreHorizontal size={16} aria-hidden="true" />
                                </button>
                            </li>
                        )
                    })}
                </ul>
            )}
        </article>
    )
}
