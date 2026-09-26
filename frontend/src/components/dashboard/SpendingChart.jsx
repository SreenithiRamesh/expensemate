import { useState } from 'react'

import { DashboardEmptyState } from './DashboardEmptyState'

function formatCurrency(value) {
    return `₹${value.toLocaleString('en-IN', {
        maximumFractionDigits: 2,
    })}`
}

export function SpendingChart({
                                  series = {},
                                  monthlyBudget = 0,
                                  monthlySpending = 0,
                                  isPreview = false,
                                  hasData = true,
                              }) {
    const [period, setPeriod] = useState('monthly')
    const [selectedIndex, setSelectedIndex] = useState(null)

    const currentSeries = series[period]
    const points = currentSeries?.points ?? []
    const categories = currentSeries?.categories ?? []

    const totalSpending = points.reduce(
        (total, point) => total + point.amount,
        0,
    )

    const maximumAmount = Math.max(
        ...points.map((point) => point.amount),
        1,
    )

    // Round the chart scale up to a readable rupee amount.
    const axisMaximum = Math.max(
        1000,
        Math.ceil(maximumAmount / 1000) * 1000,
    )

    const remainingBudget = monthlyBudget - monthlySpending
    const hasBudget = monthlyBudget > 0
    const isOverBudget = hasBudget && remainingBudget < 0

    const selectedPoint =
        selectedIndex === null ? null : points[selectedIndex]

    function changePeriod(nextPeriod) {
        setPeriod(nextPeriod)
        setSelectedIndex(null)
    }

    return (
        <article className="min-w-0 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h2 className="text-xl font-bold text-heading">
                        Spending overview
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        {period === 'monthly'
                            ? 'Your expenses grouped by week.'
                            : 'Your expenses grouped by day.'}
                    </p>
                </div>

                <div
                    role="group"
                    aria-label="Select time period"
                    className="inline-flex rounded-xl border border-slate-200 bg-white p-1 text-sm font-semibold"
                >
                    {['weekly', 'monthly'].map((option) => (
                        <button
                            key={option}
                            type="button"
                            aria-pressed={period === option}
                            onClick={() => changePeriod(option)}
                            className={`min-h-10 rounded-lg px-3 capitalize transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 ${
                                period === option
                                    ? 'bg-brand-600 text-white shadow-sm'
                                    : 'text-slate-500 hover:bg-brand-50 hover:text-brand-700'
                            }`}
                        >
                            {option}
                        </button>
                    ))}
                </div>
            </div>

            {isPreview && (
                <p className="mt-4 inline-flex rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
                    Sample data — preview only
                </p>
            )}

            {!hasData || points.length === 0 ? (
                <div className="mt-6">
                    <DashboardEmptyState
                        variant="receipt"
                        title="Nothing to chart yet"
                        description="Add a few expenses and your spending trend will show up here."
                    />
                </div>
            ) : (
                <>
                    <div className="mt-5 grid gap-3 sm:grid-cols-3">
                        <div className="rounded-2xl bg-brand-50/80 p-3">
                            <p className="text-xs font-semibold text-brand-700">
                                {period === 'monthly'
                                    ? 'Spent this month'
                                    : 'Spent this week'}
                            </p>
                            <p className="mt-1 text-xl font-black text-heading">
                                {formatCurrency(totalSpending)}
                            </p>
                        </div>

                        <div className="rounded-2xl bg-slate-50 p-3">
                            <p className="text-xs font-semibold text-slate-500">
                                Monthly budget
                            </p>
                            <p className="mt-1 text-xl font-black text-heading">
                                {hasBudget
                                    ? formatCurrency(monthlyBudget)
                                    : 'Not set'}
                            </p>
                        </div>

                        <div
                            className={`rounded-2xl p-3 ${
                                isOverBudget
                                    ? 'bg-rose-50'
                                    : 'bg-emerald-50/70'
                            }`}
                        >
                            <p
                                className={`text-xs font-semibold ${
                                    isOverBudget
                                        ? 'text-rose-700'
                                        : 'text-emerald-700'
                                }`}
                            >
                                {isOverBudget
                                    ? 'Over monthly budget'
                                    : 'Remaining this month'}
                            </p>
                            <p
                                className={`mt-1 text-xl font-black ${
                                    isOverBudget
                                        ? 'text-rose-700'
                                        : 'text-heading'
                                }`}
                            >
                                {hasBudget
                                    ? formatCurrency(Math.abs(remainingBudget))
                                    : '—'}
                            </p>
                        </div>
                    </div>

                    <div className="mt-5 flex flex-wrap items-center justify-between gap-2 text-xs">
                        <span className="inline-flex items-center gap-2 font-semibold text-slate-600">
                            <span
                                aria-hidden="true"
                                className="h-2.5 w-2.5 rounded-full bg-brand-600"
                            />
                            Expenses
                        </span>

                        <span className="text-slate-500">
                            {currentSeries?.caption}
                        </span>
                    </div>

                    <div className="mt-5 flex gap-2 sm:gap-3">
                        <div
                            aria-hidden="true"
                            className="flex h-48 w-14 shrink-0 flex-col justify-between text-right text-[10px] font-semibold text-slate-500 sm:w-16 sm:text-xs"
                        >
                            <span>{formatCurrency(axisMaximum)}</span>
                            <span>{formatCurrency(axisMaximum / 2)}</span>
                            <span>₹0</span>
                        </div>

                        <div className="relative min-w-0 flex-1">
                            <div
                                aria-hidden="true"
                                className="pointer-events-none absolute inset-x-0 top-0 flex h-48 flex-col justify-between"
                            >
                                <div className="border-t border-dashed border-slate-200" />
                                <div className="border-t border-dashed border-slate-200" />
                                <div className="border-t border-slate-200" />
                            </div>

                            <div
                                role="group"
                                aria-label={
                                    period === 'monthly'
                                        ? 'Monthly expenses by week'
                                        : 'Weekly expenses by day'
                                }
                                className="relative grid gap-1 sm:gap-3"
                                style={{
                                    gridTemplateColumns: `repeat(${points.length}, minmax(0, 1fr))`,
                                }}
                            >
                                {points.map((point, index) => {
                                    const height =
                                        (point.amount / axisMaximum) * 100

                                    const isSelected =
                                        selectedIndex === index

                                    return (
                                        <button
                                            key={point.label}
                                            type="button"
                                            aria-label={`${point.label}: spent ${formatCurrency(point.amount)}`}
                                            aria-pressed={isSelected}
                                            onClick={() =>
                                                setSelectedIndex(index)
                                            }
                                            onFocus={() =>
                                                setSelectedIndex(index)
                                            }
                                            onMouseEnter={() =>
                                                setSelectedIndex(index)
                                            }
                                            className="group min-w-0 rounded-lg px-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                                        >
                                            <span
                                                aria-hidden="true"
                                                className="flex h-48 items-end justify-center"
                                            >
                                                <span
                                                    className={`block w-full max-w-14 rounded-t-lg bg-linear-to-t from-brand-600 to-brand-400 transition-opacity motion-reduce:transition-none ${
                                                        isSelected
                                                            ? 'opacity-100 ring-2 ring-brand-700 ring-offset-2'
                                                            : 'opacity-80 group-hover:opacity-100'
                                                    }`}
                                                    style={{
                                                        height: `${height}%`,
                                                    }}
                                                />
                                            </span>

                                            <span
                                                aria-hidden="true"
                                                className="mt-3 block text-[10px] font-semibold text-slate-500 sm:text-xs"
                                            >
                                                {point.label}
                                            </span>
                                        </button>
                                    )
                                })}
                            </div>
                        </div>
                    </div>

                    <div className="mt-4 min-h-10 rounded-xl bg-brand-50/60 px-3 py-2 text-sm">
                        {selectedPoint ? (
                            <p className="font-semibold text-brand-800">
                                {selectedPoint.label}
                                <span className="mx-2 text-brand-300">·</span>
                                Spent {formatCurrency(selectedPoint.amount)}
                            </p>
                        ) : (
                            <p className="text-slate-500">
                                Hover, tap, or focus a bar to see its amount.
                            </p>
                        )}
                    </div>

                    {categories.length > 0 && (
                        <div className="mt-6">
                            <h3 className="text-sm font-bold text-heading">
                                {period === 'monthly'
                                    ? 'Categories this month'
                                    : 'Categories this week'}
                            </h3>

                            <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                                {categories.map((category) => (
                                    <div
                                        key={category.label}
                                        className="rounded-2xl border border-slate-100 bg-white/70 p-3"
                                    >
                                        <span
                                            aria-hidden="true"
                                            className="inline-block h-2 w-2 rounded-full"
                                            style={{
                                                backgroundColor: category.color,
                                            }}
                                        />

                                        <p className="mt-2 text-xs font-semibold text-slate-500">
                                            {category.label}
                                        </p>

                                        <p className="mt-1 text-sm font-bold text-heading">
                                            {formatCurrency(category.value)}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </>
            )}
        </article>
    )
}