import {
    AlertTriangle,
    CalendarClock,
    CirclePause,
    CirclePlay,
    Plus,
    RefreshCw,
    Repeat2,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import {
    getApiErrorMessage,
} from '../api/apiErrors'
import { Button } from '../components/common/Button'
import {
    DeleteRecurringExpenseDialog,
} from '../components/recurring/DeleteRecurringExpenseDialog'
import {
    RecordPaymentDialog,
} from '../components/recurring/RecordPaymentDialog'
import {
    RecurringExpenseCard,
} from '../components/recurring/RecurringExpenseCard'
import {
    RecurringExpenseFormDialog,
} from '../components/recurring/RecurringExpenseFormDialog'
import {
    useRecurringExpenses,
    useSetRecurringExpenseActive,
} from '../features/recurring-expenses/useRecurringExpenseQueries'

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

function isDueWithinDays(
    value,
    days,
) {
    const dueDate =
        parseLocalDate(value)

    if (!dueDate) {
        return false
    }

    const today = new Date()
    today.setHours(0, 0, 0, 0)

    const endDate = new Date(today)
    endDate.setDate(
        endDate.getDate() + days,
    )

    return (
        dueDate >= today &&
        dueDate <= endDate
    )
}

function RecurringLoadingState() {
    return (
        <div
            role="status"
            aria-label="Loading recurring expenses"
            className="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
        >
            {Array.from({ length: 6 }).map(
                (_, index) => (
                    <div
                        key={index}
                        className="h-80 animate-pulse rounded-3xl bg-linear-to-r from-slate-100 via-brand-50/70 to-slate-100 motion-reduce:animate-none"
                    />
                ),
            )}
        </div>
    )
}

function RecurringSummary({
                              recurringExpenses,
                          }) {
    const totalCount =
        recurringExpenses.length

    const activeCount =
        recurringExpenses.filter(
            (item) => item.active,
        ).length

    const upcomingCount =
        recurringExpenses.filter(
            (item) =>
                item.active &&
                isDueWithinDays(
                    item.nextDueDate,
                    30,
                ),
        ).length

    const summaryItems = [
        {
            label: 'Total schedules',
            value: totalCount,
            detail:
                totalCount === 1
                    ? 'Recurring expense'
                    : 'Recurring expenses',
            icon: Repeat2,
            iconClass:
                'bg-brand-100 text-brand-700',
        },
        {
            label: 'Active schedules',
            value: activeCount,
            detail: `${
                totalCount - activeCount
            } paused`,
            icon: CirclePlay,
            iconClass:
                'bg-emerald-100 text-emerald-700',
        },
        {
            label: 'Due within 30 days',
            value: upcomingCount,
            detail:
                upcomingCount === 1
                    ? 'Upcoming payment'
                    : 'Upcoming payments',
            icon: CalendarClock,
            iconClass:
                upcomingCount > 0
                    ? 'bg-amber-100 text-amber-700'
                    : 'bg-sky-100 text-sky-700',
        },
    ]

    return (
        <section
            aria-label="Recurring expense summary"
            className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3"
        >
            {summaryItems.map((item) => {
                const Icon = item.icon

                return (
                    <article
                        key={item.label}
                        className="rounded-3xl border border-white/80 bg-white/80 p-5 shadow-sm backdrop-blur-xl"
                    >
                        <div className="flex items-center gap-4">
                            <span
                                className={`grid h-12 w-12 shrink-0 place-items-center rounded-2xl ${item.iconClass}`}
                            >
                                <Icon
                                    size={22}
                                    aria-hidden="true"
                                />
                            </span>

                            <div>
                                <p className="text-sm font-bold text-slate-500">
                                    {item.label}
                                </p>

                                <p className="mt-1 text-2xl font-black text-heading">
                                    {item.value}
                                </p>

                                <p className="mt-1 text-xs font-semibold text-slate-400">
                                    {item.detail}
                                </p>
                            </div>
                        </div>
                    </article>
                )
            })}
        </section>
    )
}

function RecurringEmptyState({
                                 filtered,
                                 onReset,
                                 onCreate,
                             }) {
    return (
        <section className="flex min-h-80 flex-col items-center justify-center rounded-3xl border border-dashed border-brand-200 bg-white/70 px-6 text-center shadow-sm">
            <span className="grid h-16 w-16 place-items-center rounded-3xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/20">
                <Repeat2
                    size={30}
                    aria-hidden="true"
                />
            </span>

            <h2 className="mt-5 text-xl font-black text-heading">
                {filtered
                    ? 'No matching schedules'
                    : 'No recurring expenses yet'}
            </h2>

            <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                {filtered
                    ? 'Try viewing all recurring schedules.'
                    : 'Create a repeating schedule for rent, bills, subscriptions or other regular expenses.'}
            </p>

            <div className="mt-5">
                {filtered ? (
                    <Button
                        variant="secondary"
                        onClick={onReset}
                    >
                        View all schedules
                    </Button>
                ) : (
                    <Button
                        onClick={onCreate}
                    >
                        <Plus
                            size={17}
                            aria-hidden="true"
                        />
                        Create first schedule
                    </Button>
                )}
            </div>
        </section>
    )
}

const FILTER_OPTIONS = [
    {
        value: 'all',
        label: 'All schedules',
        icon: Repeat2,
    },
    {
        value: 'active',
        label: 'Active',
        icon: CirclePlay,
    },
    {
        value: 'paused',
        label: 'Paused',
        icon: CirclePause,
    },
]

export default function RecurringExpensesPage() {
    const [filter, setFilter] =
        useState('all')

    const [dialog, setDialog] =
        useState(null)

    const [togglingId, setTogglingId] =
        useState(null)

    const [actionError, setActionError] =
        useState(null)

    const {
        data,
        error,
        isError,
        isFetching,
        isLoading,
        refetch,
    } = useRecurringExpenses()

    const activeMutation =
        useSetRecurringExpenseActive()

    const recurringExpenses =
        Array.isArray(data)
            ? data
            : []

    const filteredExpenses =
        recurringExpenses.filter(
            (item) => {
                if (filter === 'active') {
                    return item.active
                }

                if (filter === 'paused') {
                    return !item.active
                }

                return true
            },
        )

    function openCreateDialog() {
        setDialog({
            type: 'create',
        })
    }

    function openEditDialog(
        recurringExpense,
    ) {
        setDialog({
            type: 'edit',
            recurringExpense,
        })
    }

    function openDeleteDialog(
        recurringExpense,
    ) {
        setDialog({
            type: 'delete',
            recurringExpense,
        })
    }

    function openPaymentDialog(
        recurringExpense,
    ) {
        if (!recurringExpense.active) {
            return
        }

        setDialog({
            type: 'payment',
            recurringExpense,
        })
    }

    function closeDialog() {
        setDialog(null)
    }

    async function handleToggleActive(
        recurringExpense,
    ) {
        setActionError(null)
        setTogglingId(
            recurringExpense.id,
        )

        try {
            const nextActive =
                !recurringExpense.active

            await activeMutation.mutateAsync({
                recurringExpense,
                active: nextActive,
            })

            toast.success(
                nextActive
                    ? 'Recurring schedule activated.'
                    : 'Recurring schedule paused.',
            )
        } catch (toggleError) {
            setActionError(
                getApiErrorMessage(
                    toggleError,
                    'Schedule status could not be updated.',
                ),
            )
        } finally {
            setTogglingId(null)
        }
    }

    return (
        <>
            <div className="space-y-6">
                <header className="flex flex-col justify-between gap-5 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl sm:flex-row sm:items-center">
                    <div className="flex items-start gap-4">
                        <span className="hidden h-12 w-12 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/20 sm:grid">
                            <Repeat2
                                size={22}
                                aria-hidden="true"
                            />
                        </span>

                        <div>
                            <p className="text-sm font-bold text-brand-700">
                                Personal finance
                            </p>

                            <h1 className="mt-1 text-3xl font-black tracking-tight text-heading">
                                Recurring expenses
                            </h1>

                            <p className="mt-2 text-sm text-slate-500">
                                Manage repeating payments
                                and upcoming due dates.
                            </p>
                        </div>
                    </div>

                    <Button
                        onClick={
                            openCreateDialog
                        }
                    >
                        <Plus
                            size={18}
                            aria-hidden="true"
                        />
                        Create schedule
                    </Button>
                </header>

                {!isLoading &&
                    !isError &&
                    recurringExpenses.length >
                    0 && (
                        <RecurringSummary
                            recurringExpenses={
                                recurringExpenses
                            }
                        />
                    )}

                {!isLoading &&
                    !isError &&
                    recurringExpenses.length >
                    0 && (
                        <section
                            aria-label="Filter recurring expenses"
                            className="flex flex-col justify-between gap-4 rounded-3xl border border-white/80 bg-white/80 p-4 shadow-sm backdrop-blur-xl sm:flex-row sm:items-center"
                        >
                            <div className="flex flex-wrap gap-2">
                                {FILTER_OPTIONS.map(
                                    (option) => {
                                        const Icon =
                                            option.icon

                                        const selected =
                                            filter ===
                                            option.value

                                        return (
                                            <button
                                                key={
                                                    option.value
                                                }
                                                type="button"
                                                aria-pressed={
                                                    selected
                                                }
                                                onClick={() =>
                                                    setFilter(
                                                        option.value,
                                                    )
                                                }
                                                className={`inline-flex min-h-10 items-center gap-2 rounded-xl px-4 text-sm font-bold transition ${
                                                    selected
                                                        ? 'bg-brand-600 text-white shadow-sm'
                                                        : 'border border-slate-200 bg-white text-slate-600 hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700'
                                                }`}
                                            >
                                                <Icon
                                                    size={
                                                        16
                                                    }
                                                    aria-hidden="true"
                                                />
                                                {
                                                    option.label
                                                }
                                            </button>
                                        )
                                    },
                                )}
                            </div>

                            {isFetching && (
                                <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
                                    <RefreshCw
                                        size={13}
                                        className="animate-spin motion-reduce:animate-none"
                                        aria-hidden="true"
                                    />
                                    Updating
                                </span>
                            )}
                        </section>
                    )}

                {actionError && (
                    <div
                        role="alert"
                        className="flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700"
                    >
                        <AlertTriangle
                            size={19}
                            className="mt-0.5 shrink-0"
                            aria-hidden="true"
                        />

                        <span>
                            {actionError}
                        </span>
                    </div>
                )}

                {isLoading && (
                    <RecurringLoadingState />
                )}

                {isError && (
                    <section
                        role="alert"
                        className="flex min-h-80 flex-col items-center justify-center rounded-3xl border border-rose-100 bg-white/80 px-6 text-center shadow-sm"
                    >
                        <span className="grid h-14 w-14 place-items-center rounded-2xl bg-rose-100 text-rose-600">
                            <AlertTriangle
                                size={26}
                                aria-hidden="true"
                            />
                        </span>

                        <h2 className="mt-4 text-xl font-black text-heading">
                            Recurring expenses could
                            not be loaded
                        </h2>

                        <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                            {getApiErrorMessage(
                                error,
                                'Please check the backend connection and try again.',
                            )}
                        </p>

                        <Button
                            variant="secondary"
                            className="mt-5"
                            onClick={() =>
                                refetch()
                            }
                        >
                            <RefreshCw
                                size={17}
                                aria-hidden="true"
                            />
                            Try again
                        </Button>
                    </section>
                )}

                {!isLoading &&
                    !isError &&
                    recurringExpenses.length ===
                    0 && (
                        <RecurringEmptyState
                            filtered={false}
                            onCreate={
                                openCreateDialog
                            }
                        />
                    )}

                {!isLoading &&
                    !isError &&
                    recurringExpenses.length >
                    0 &&
                    filteredExpenses.length ===
                    0 && (
                        <RecurringEmptyState
                            filtered
                            onReset={() =>
                                setFilter('all')
                            }
                            onCreate={
                                openCreateDialog
                            }
                        />
                    )}

                {!isLoading &&
                    !isError &&
                    filteredExpenses.length >
                    0 && (
                        <section
                            aria-label="Recurring expense schedules"
                            className="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
                        >
                            {filteredExpenses.map(
                                (
                                    recurringExpense,
                                ) => (
                                    <RecurringExpenseCard
                                        key={
                                            recurringExpense.id
                                        }
                                        recurringExpense={
                                            recurringExpense
                                        }
                                        toggling={
                                            togglingId ===
                                            recurringExpense.id
                                        }
                                        onEdit={
                                            openEditDialog
                                        }
                                        onDelete={
                                            openDeleteDialog
                                        }
                                        onToggleActive={
                                            handleToggleActive
                                        }
                                        onRecordPayment={
                                            openPaymentDialog
                                        }
                                    />
                                ),
                            )}
                        </section>
                    )}
            </div>

            {dialog?.type ===
                'create' && (
                    <RecurringExpenseFormDialog
                        onClose={closeDialog}
                    />
                )}

            {dialog?.type ===
                'edit' && (
                    <RecurringExpenseFormDialog
                        recurringExpense={
                            dialog.recurringExpense
                        }
                        onClose={closeDialog}
                    />
                )}

            {dialog?.type ===
                'payment' && (
                    <RecordPaymentDialog
                        recurringExpense={
                            dialog.recurringExpense
                        }
                        onClose={closeDialog}
                    />
                )}

            {dialog?.type ===
                'delete' && (
                    <DeleteRecurringExpenseDialog
                        recurringExpense={
                            dialog.recurringExpense
                        }
                        onClose={closeDialog}
                    />
                )}
        </>
    )
}