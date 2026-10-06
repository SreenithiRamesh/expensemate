import {
    CalendarCheck,
    CalendarClock,
    CirclePause,
    CirclePlay,
    Pencil,
    ReceiptText,
    Trash2,
} from 'lucide-react'

import {
    getCategoryLabel,
    getFrequencyLabel,
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

function parseLocalDate(value) {
    if (
        typeof value !== 'string' ||
        !value
    ) {
        return null
    }

    const date = new Date(
        `${value}T00:00:00`,
    )

    return Number.isNaN(date.getTime())
        ? null
        : date
}

function formatDate(value) {
    const date = parseLocalDate(value)

    if (!date) {
        return 'Date unavailable'
    }

    return date.toLocaleDateString(
        'en-IN',
        {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
        },
    )
}

function getDueState(
    nextDueDate,
    active,
) {
    if (!active) {
        return {
            label: 'Paused',
            detail:
                'Payment generation is currently paused.',
            badgeClass:
                'bg-slate-100 text-slate-600',
            iconClass:
                'bg-slate-100 text-slate-600',
            icon: CirclePause,
        }
    }

    const dueDate =
        parseLocalDate(nextDueDate)

    if (!dueDate) {
        return {
            label: 'Active',
            detail:
                'The schedule is active.',
            badgeClass:
                'bg-emerald-100 text-emerald-700',
            iconClass:
                'bg-emerald-100 text-emerald-700',
            icon: CalendarCheck,
        }
    }

    const today = new Date()
    today.setHours(0, 0, 0, 0)

    const differenceInDays =
        Math.round(
            (dueDate.getTime() -
                today.getTime()) /
            86_400_000,
        )

    if (differenceInDays < 0) {
        return {
            label: 'Overdue',
            detail: `${Math.abs(
                differenceInDays,
            )} ${
                Math.abs(
                    differenceInDays,
                ) === 1
                    ? 'day'
                    : 'days'
            } overdue`,
            badgeClass:
                'bg-rose-100 text-rose-700',
            iconClass:
                'bg-rose-100 text-rose-700',
            icon: CalendarClock,
        }
    }

    if (differenceInDays === 0) {
        return {
            label: 'Due today',
            detail:
                'This payment is due today.',
            badgeClass:
                'bg-amber-100 text-amber-800',
            iconClass:
                'bg-amber-100 text-amber-700',
            icon: CalendarClock,
        }
    }

    if (differenceInDays <= 7) {
        return {
            label: 'Due soon',
            detail: `Due in ${differenceInDays} ${
                differenceInDays === 1
                    ? 'day'
                    : 'days'
            }`,
            badgeClass:
                'bg-amber-100 text-amber-800',
            iconClass:
                'bg-amber-100 text-amber-700',
            icon: CalendarClock,
        }
    }

    return {
        label: 'Upcoming',
        detail: `Due in ${differenceInDays} days`,
        badgeClass:
            'bg-emerald-100 text-emerald-700',
        iconClass:
            'bg-brand-100 text-brand-700',
        icon: CalendarCheck,
    }
}

export function RecurringExpenseCard({
                                         recurringExpense,
                                         onEdit,
                                         onDelete,
                                         onToggleActive,
                                         onRecordPayment,
                                         toggling = false,
                                     }) {
    const dueState = getDueState(
        recurringExpense.nextDueDate,
        recurringExpense.active,
    )

    const DueIcon = dueState.icon

    return (
        <article className="rounded-3xl border border-white/80 bg-white/85 p-5 shadow-sm backdrop-blur-xl transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-900/5 motion-reduce:hover:translate-y-0">
            <div className="flex items-start justify-between gap-4">
                <div className="flex min-w-0 items-start gap-3">
                    <span
                        className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl ${dueState.iconClass}`}
                    >
                        <DueIcon
                            size={21}
                            aria-hidden="true"
                        />
                    </span>

                    <div className="min-w-0">
                        <h2 className="truncate text-lg font-black text-heading">
                            {
                                recurringExpense.title
                            }
                        </h2>

                        <p className="mt-1 text-sm font-semibold text-slate-500">
                            {getCategoryLabel(
                                recurringExpense.category,
                            )}
                        </p>
                    </div>
                </div>

                <span
                    className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${dueState.badgeClass}`}
                >
                    {dueState.label}
                </span>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-4">
                <div className="rounded-2xl bg-brand-50/70 p-3">
                    <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                        Amount
                    </p>

                    <p className="mt-1 text-lg font-black text-heading">
                        {formatCurrency(
                            recurringExpense.amount,
                        )}
                    </p>
                </div>

                <div className="rounded-2xl bg-sky-50/70 p-3">
                    <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                        Frequency
                    </p>

                    <p className="mt-1 text-lg font-black text-heading">
                        {getFrequencyLabel(
                            recurringExpense.frequency,
                        )}
                    </p>
                </div>
            </div>

            <div className="mt-4 rounded-2xl border border-slate-100 bg-white p-3">
                <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                    Next due date
                </p>

                <div className="mt-1 flex items-center justify-between gap-3">
                    <p className="font-black text-heading">
                        {formatDate(
                            recurringExpense.nextDueDate,
                        )}
                    </p>

                    <p className="text-xs font-bold text-slate-500">
                        {dueState.detail}
                    </p>
                </div>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-3">
                <button
                    type="button"
                    aria-label={`Edit ${recurringExpense.title}`}
                    onClick={() =>
                        onEdit(
                            recurringExpense,
                        )
                    }
                    className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-3 text-sm font-bold text-brand-700 transition hover:border-brand-300 hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                >
                    <Pencil
                        size={16}
                        aria-hidden="true"
                    />
                    Edit
                </button>

                <button
                    type="button"
                    aria-label={`Delete ${recurringExpense.title}`}
                    onClick={() =>
                        onDelete(
                            recurringExpense,
                        )
                    }
                    className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 text-sm font-bold text-rose-600 transition hover:border-rose-300 hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-rose-100"
                >
                    <Trash2
                        size={16}
                        aria-hidden="true"
                    />
                    Delete
                </button>

                <button
                    type="button"
                    disabled={toggling}
                    aria-label={`${
                        recurringExpense.active
                            ? 'Pause'
                            : 'Activate'
                    } ${recurringExpense.title}`}
                    onClick={() =>
                        onToggleActive(
                            recurringExpense,
                        )
                    }
                    className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-sm font-bold text-slate-600 transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {recurringExpense.active ? (
                        <CirclePause
                            size={16}
                            aria-hidden="true"
                        />
                    ) : (
                        <CirclePlay
                            size={16}
                            aria-hidden="true"
                        />
                    )}

                    {recurringExpense.active
                        ? 'Pause'
                        : 'Activate'}
                </button>

                <button
                    type="button"
                    disabled={
                        !recurringExpense.active
                    }
                    aria-label={`Record payment for ${recurringExpense.title}`}
                    onClick={() =>
                        onRecordPayment(
                            recurringExpense,
                        )
                    }
                    className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-linear-to-r from-brand-600 to-brand-500 px-3 text-sm font-bold text-white shadow-sm transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
                >
                    <ReceiptText
                        size={16}
                        aria-hidden="true"
                    />
                    Record payment
                </button>
            </div>
        </article>
    )
}