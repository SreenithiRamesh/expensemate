import {
    AlertTriangle,
    CalendarDays,
    ChevronLeft,
    ChevronRight,
    CircleGauge,
    Plus,
    RefreshCw,
    WalletCards,
} from 'lucide-react'
import { useState } from 'react'

import {
    getApiErrorMessage,
} from '../api/apiErrors'
import {
    BudgetCard,
} from '../components/budgets/BudgetCard'
import {
    BudgetFormDialog,
} from '../components/budgets/BudgetFormDialog'
import {
    DeleteBudgetDialog,
} from '../components/budgets/DeleteBudgetDialog'
import { Button } from '../components/common/Button'
import {
    useBudgets,
} from '../features/budgets/useBudgetQueries'

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

function formatPeriod(month, year) {
    return new Date(
        year,
        month - 1,
        1,
    ).toLocaleDateString('en-IN', {
        month: 'long',
        year: 'numeric',
    })
}

function changePeriod(
    currentMonth,
    currentYear,
    change,
) {
    const date = new Date(
        currentYear,
        currentMonth - 1 + change,
        1,
    )

    return {
        month: date.getMonth() + 1,
        year: date.getFullYear(),
    }
}

function BudgetLoadingState() {
    return (
        <div
            role="status"
            aria-label="Loading budgets"
            className="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
        >
            {Array.from({ length: 6 }).map(
                (_, index) => (
                    <div
                        key={index}
                        className="h-72 animate-pulse rounded-3xl bg-linear-to-r from-slate-100 via-brand-50/70 to-slate-100 motion-reduce:animate-none"
                    />
                ),
            )}
        </div>
    )
}

function BudgetSummary({
                           budgets,
                       }) {
    const totalLimit = budgets.reduce(
        (total, budget) =>
            total +
            (Number(
                budget.monthlyLimit,
            ) || 0),
        0,
    )

    const totalSpent = budgets.reduce(
        (total, budget) =>
            total +
            (Number(budget.spent) || 0),
        0,
    )

    const remaining =
        totalLimit - totalSpent

    const usage =
        totalLimit > 0
            ? (totalSpent / totalLimit) *
            100
            : 0

    const summaryItems = [
        {
            label: 'Total budget',
            value:
                formatCurrency(totalLimit),
            detail: `${budgets.length} ${
                budgets.length === 1
                    ? 'category'
                    : 'categories'
            }`,
            icon: WalletCards,
            iconClass:
                'bg-brand-100 text-brand-700',
        },
        {
            label: 'Total spent',
            value:
                formatCurrency(totalSpent),
            detail: `${usage.toFixed(
                1,
            )}% of limits used`,
            icon: CircleGauge,
            iconClass:
                usage >= 100
                    ? 'bg-rose-100 text-rose-700'
                    : usage >= 80
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-sky-100 text-sky-700',
        },
        {
            label:
                remaining < 0
                    ? 'Amount over'
                    : 'Remaining',
            value: formatCurrency(
                Math.abs(remaining),
            ),
            detail:
                remaining < 0
                    ? 'Combined limits exceeded'
                    : 'Available across budgets',
            icon:
                remaining < 0
                    ? AlertTriangle
                    : WalletCards,
            iconClass:
                remaining < 0
                    ? 'bg-rose-100 text-rose-700'
                    : 'bg-emerald-100 text-emerald-700',
        },
    ]

    return (
        <section
            aria-label="Budget summary"
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

                            <div className="min-w-0">
                                <p className="text-sm font-bold text-slate-500">
                                    {item.label}
                                </p>

                                <p className="mt-1 truncate text-2xl font-black text-heading">
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

function EmptyBudgetState({
                              periodLabel,
                              onCreate,
                          }) {
    return (
        <section className="flex min-h-80 flex-col items-center justify-center rounded-3xl border border-dashed border-brand-200 bg-white/70 px-6 text-center shadow-sm">
            <span className="grid h-16 w-16 place-items-center rounded-3xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/20">
                <WalletCards
                    size={30}
                    aria-hidden="true"
                />
            </span>

            <h2 className="mt-5 text-xl font-black text-heading">
                No budgets for {periodLabel}
            </h2>

            <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                Create a category budget to track
                spending and receive warning or
                exceeded-limit indicators.
            </p>

            <Button
                className="mt-5"
                onClick={onCreate}
            >
                <Plus
                    size={17}
                    aria-hidden="true"
                />
                Create first budget
            </Button>
        </section>
    )
}

export default function BudgetsPage() {
    const today = new Date()

    const [period, setPeriod] =
        useState({
            month:
                today.getMonth() + 1,
            year: today.getFullYear(),
        })

    const [dialog, setDialog] =
        useState(null)

    const {
        data,
        error,
        isError,
        isFetching,
        isLoading,
        refetch,
    } = useBudgets(period)

    const budgets =
        Array.isArray(data)
            ? data
            : []

    const periodLabel =
        formatPeriod(
            period.month,
            period.year,
        )

    function movePeriod(change) {
        setPeriod((current) =>
            changePeriod(
                current.month,
                current.year,
                change,
            ),
        )
    }

    function openCreateDialog() {
        setDialog({
            type: 'create',
        })
    }

    function openEditDialog(budget) {
        setDialog({
            type: 'edit',
            budget,
        })
    }

    function openDeleteDialog(budget) {
        setDialog({
            type: 'delete',
            budget,
        })
    }

    function closeDialog() {
        setDialog(null)
    }

    return (
        <>
            <div className="space-y-6">
                <header className="flex flex-col justify-between gap-5 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl sm:flex-row sm:items-center">
                    <div className="flex items-start gap-4">
                        <span className="hidden h-12 w-12 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/20 sm:grid">
                            <WalletCards
                                size={22}
                                aria-hidden="true"
                            />
                        </span>

                        <div>
                            <p className="text-sm font-bold text-brand-700">
                                Personal finance
                            </p>

                            <h1 className="mt-1 text-3xl font-black tracking-tight text-heading">
                                Budgets
                            </h1>

                            <p className="mt-2 text-sm text-slate-500">
                                Plan category limits and
                                monitor monthly spending.
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
                        Create budget
                    </Button>
                </header>

                <section
                    aria-label="Budget period"
                    className="flex flex-col justify-between gap-4 rounded-3xl border border-white/80 bg-white/80 p-5 shadow-sm backdrop-blur-xl sm:flex-row sm:items-center"
                >
                    <div className="flex items-center gap-3">
                        <span className="grid h-11 w-11 place-items-center rounded-2xl bg-brand-100 text-brand-700">
                            <CalendarDays
                                size={21}
                                aria-hidden="true"
                            />
                        </span>

                        <div>
                            <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                                Viewing period
                            </p>

                            <p
                                aria-live="polite"
                                className="mt-1 text-lg font-black text-heading"
                            >
                                {periodLabel}
                            </p>
                        </div>

                        {isFetching &&
                            !isLoading && (
                                <span className="ml-2 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
                                    <RefreshCw
                                        size={13}
                                        className="animate-spin motion-reduce:animate-none"
                                        aria-hidden="true"
                                    />
                                    Updating
                                </span>
                            )}
                    </div>

                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            aria-label="Previous month"
                            onClick={() =>
                                movePeriod(-1)
                            }
                            className="grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                        >
                            <ChevronLeft
                                size={20}
                                aria-hidden="true"
                            />
                        </button>

                        <button
                            type="button"
                            aria-label="Next month"
                            onClick={() =>
                                movePeriod(1)
                            }
                            className="grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
                        >
                            <ChevronRight
                                size={20}
                                aria-hidden="true"
                            />
                        </button>
                    </div>
                </section>

                {!isLoading &&
                    !isError &&
                    budgets.length > 0 && (
                        <BudgetSummary
                            budgets={budgets}
                        />
                    )}

                {isLoading && (
                    <BudgetLoadingState />
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
                            Budgets could not be loaded
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
                    budgets.length === 0 && (
                        <EmptyBudgetState
                            periodLabel={
                                periodLabel
                            }
                            onCreate={
                                openCreateDialog
                            }
                        />
                    )}

                {!isLoading &&
                    !isError &&
                    budgets.length > 0 && (
                        <section
                            aria-label={`${periodLabel} budgets`}
                            className="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
                        >
                            {budgets.map(
                                (budget) => (
                                    <BudgetCard
                                        key={
                                            budget.id
                                        }
                                        budget={
                                            budget
                                        }
                                        onEdit={
                                            openEditDialog
                                        }
                                        onDelete={
                                            openDeleteDialog
                                        }
                                    />
                                ),
                            )}
                        </section>
                    )}
            </div>

            {dialog?.type ===
                'create' && (
                    <BudgetFormDialog
                        onClose={closeDialog}
                    />
                )}

            {dialog?.type ===
                'edit' && (
                    <BudgetFormDialog
                        budget={dialog.budget}
                        onClose={closeDialog}
                    />
                )}

            {dialog?.type ===
                'delete' && (
                    <DeleteBudgetDialog
                        budget={dialog.budget}
                        onClose={closeDialog}
                    />
                )}
        </>
    )
}