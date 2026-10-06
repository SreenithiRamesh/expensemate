import {
    AlertTriangle,
    CheckCircle2,
    Pencil,
    Trash2,
} from 'lucide-react'

import {
    getCategoryLabel,
} from '../../constants/finance'

const currencyFormatter =
    new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    })

function formatCurrency(value) {
    const amount = Number(value)

    return currencyFormatter.format(
        Number.isFinite(amount)
            ? amount
            : 0,
    )
}

function formatBudgetPeriod(month, year) {
    const monthNumber = Number(month)
    const yearNumber = Number(year)

    if (
        !Number.isInteger(monthNumber) ||
        monthNumber < 1 ||
        monthNumber > 12 ||
        !Number.isInteger(yearNumber)
    ) {
        return 'Budget period'
    }

    return new Date(
        yearNumber,
        monthNumber - 1,
        1,
    ).toLocaleDateString('en-IN', {
        month: 'long',
        year: 'numeric',
    })
}

function getBudgetStatus(percentageUsed) {
    if (percentageUsed >= 100) {
        return {
            label: 'Budget exceeded',
            description:
                'Spending has crossed this budget limit.',
            badgeClass:
                'bg-rose-100 text-rose-700',
            iconClass:
                'bg-rose-100 text-rose-700',
            progressClass:
                'from-rose-600 to-red-400',
            icon: AlertTriangle,
        }
    }

    if (percentageUsed >= 80) {
        return {
            label: 'Approaching limit',
            description:
                'Spending is close to this budget limit.',
            badgeClass:
                'bg-amber-100 text-amber-800',
            iconClass:
                'bg-amber-100 text-amber-700',
            progressClass:
                'from-amber-500 to-orange-400',
            icon: AlertTriangle,
        }
    }

    return {
        label: 'On track',
        description:
            'Spending is currently within this budget.',
        badgeClass:
            'bg-emerald-100 text-emerald-700',
        iconClass:
            'bg-emerald-100 text-emerald-700',
        progressClass:
            'from-brand-600 to-brand-400',
        icon: CheckCircle2,
    }
}

export function BudgetCard({
                               budget,
                               onEdit,
                               onDelete,
                           }) {
    const percentageUsed = Math.max(
        Number(budget.percentageUsed) || 0,
        0,
    )

    const displayedProgress =
        Math.min(percentageUsed, 100)

    const remaining =
        Number(budget.remaining) || 0

    const status =
        getBudgetStatus(percentageUsed)

    const StatusIcon = status.icon

    return (
        <article className="group rounded-3xl border border-white/80 bg-white/85 p-5 shadow-sm backdrop-blur-xl transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-900/5 motion-reduce:hover:translate-y-0">
            <div className="flex items-start justify-between gap-4">
                <div className="flex min-w-0 items-start gap-3">
                    <span
                        className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl ${status.iconClass}`}
                    >
                        <StatusIcon
                            size={21}
                            aria-hidden="true"
                        />
                    </span>

                    <div className="min-w-0">
                        <h2 className="truncate text-lg font-black text-heading">
                            {getCategoryLabel(
                                budget.category,
                            )}
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            {formatBudgetPeriod(
                                budget.month,
                                budget.year,
                            )}
                        </p>
                    </div>
                </div>

                <span
                    className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${status.badgeClass}`}
                >
                    {status.label}
                </span>
            </div>

            <div className="mt-6 flex items-end justify-between gap-4">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Spent
                    </p>

                    <p className="mt-1 text-2xl font-black text-heading">
                        {formatCurrency(
                            budget.spent,
                        )}
                    </p>
                </div>

                <div className="text-right">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                        Limit
                    </p>

                    <p className="mt-1 font-bold text-slate-600">
                        {formatCurrency(
                            budget.monthlyLimit,
                        )}
                    </p>
                </div>
            </div>

            <div className="mt-4">
                <div className="mb-2 flex items-center justify-between gap-3 text-xs font-bold">
                    <span className="text-slate-500">
                        {percentageUsed.toFixed(1)}% used
                    </span>

                    <span
                        className={
                            remaining < 0
                                ? 'text-rose-600'
                                : 'text-brand-700'
                        }
                    >
                        {remaining < 0
                            ? `${formatCurrency(
                                Math.abs(remaining),
                            )} over`
                            : `${formatCurrency(
                                remaining,
                            )} remaining`}
                    </span>
                </div>

                <div
                    role="progressbar"
                    aria-label={`${getCategoryLabel(
                        budget.category,
                    )} budget usage`}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={Math.round(
                        displayedProgress,
                    )}
                    aria-valuetext={`${percentageUsed.toFixed(
                        1,
                    )}% used`}
                    className="h-3 overflow-hidden rounded-full bg-slate-100"
                >
                    <div
                        className={`h-full rounded-full bg-linear-to-r transition-[width] duration-700 motion-reduce:transition-none ${status.progressClass}`}
                        style={{
                            width: `${displayedProgress}%`,
                        }}
                    />
                </div>
            </div>

            <p className="mt-3 text-sm leading-6 text-slate-500">
                {status.description}
            </p>

            <div className="mt-5 flex gap-3 border-t border-slate-100 pt-4">
                <button
                    type="button"
                    onClick={() =>
                        onEdit(budget)
                    }
                    className="inline-flex min-h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-3 text-sm font-bold text-brand-700 transition hover:border-brand-300 hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                >
                    <Pencil
                        size={16}
                        aria-hidden="true"
                    />
                    Edit
                </button>

                <button
                    type="button"
                    onClick={() =>
                        onDelete(budget)
                    }
                    className="inline-flex min-h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 text-sm font-bold text-rose-600 transition hover:border-rose-300 hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-rose-100"
                >
                    <Trash2
                        size={16}
                        aria-hidden="true"
                    />
                    Delete
                </button>
            </div>
        </article>
    )
}