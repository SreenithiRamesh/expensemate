import { useId, useState } from 'react'

import { DashboardEmptyState } from './DashboardEmptyState'

// UI-preview data only — replaced with real TanStack Query data in M32.
// Values are illustrative and clearly not wired to a backend.
const sampleSeries = {
    weekly: {
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        income: [0, 0, 4200, 0, 0, 0, 6000],
        expense: [820, 640, 1450, 900, 2100, 3200, 1180],
    },
    monthly: {
        labels: ['Wk 1', 'Wk 2', 'Wk 3', 'Wk 4'],
        income: [10200, 0, 0, 6000],
        expense: [5230, 6110, 7440, 4390],
    },
}

const categorySummary = [
    { label: 'Food & dining', value: 6240, color: '#159FA4' },
    { label: 'Travel', value: 4120, color: '#3E8EF7' },
    { label: 'Shopping', value: 3080, color: '#E9B949' },
    { label: 'Bills', value: 5010, color: '#2F9E6F' },
]

function buildPath(values, width, height, max) {
    return values
        .map((value, index) => {
            const x = (index / (values.length - 1 || 1)) * width
            const y = height - (value / max) * height
            return `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`
        })
        .join(' ')
}

export function SpendingChart({ hasData = true }) {
    const [period, setPeriod] = useState('monthly')
    const gradientId = useId()
    const [hoverIndex, setHoverIndex] = useState(null)

    if (!hasData) {
        return (
            <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
                <SpendingChartHeader period={period} onChange={setPeriod} />
                <div className="mt-6">
                    <DashboardEmptyState
                        variant="receipt"
                        title="Nothing to chart yet"
                        description="Add a few expenses and your spending trend will show up here."
                    />
                </div>
            </article>
        )
    }

    const { labels, income, expense } = sampleSeries[period]
    const max = Math.max(...income, ...expense, 1)
    const width = 560
    const height = 200

    const incomePath = buildPath(income, width, height, max)
    const expensePath = buildPath(expense, width, height, max)

    return (
        <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <SpendingChartHeader period={period} onChange={setPeriod} />

            <div className="mt-4 flex flex-wrap items-center gap-4 text-sm font-semibold text-slate-500">
                <span className="inline-flex items-center gap-2">
                    <span className="h-2.5 w-2.5 rounded-full bg-brand-600" aria-hidden="true" />
                    Income
                </span>
                <span className="inline-flex items-center gap-2">
                    <span className="h-2.5 w-2.5 rounded-full bg-[#E2665A]" aria-hidden="true" />
                    Expense
                </span>
                <span className="ml-auto rounded-full bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700">
                    Sample data — preview only
                </span>
            </div>

            <div className="relative mt-4">
                <svg
                    viewBox={`0 0 ${width} ${height}`}
                    className="h-56 w-full"
                    role="img"
                    aria-label={`${period === 'monthly' ? 'Monthly' : 'Weekly'} income versus expense trend`}
                >
                    <defs>
                        <linearGradient id={`${gradientId}-expense`} x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="#E2665A" stopOpacity="0.25" />
                            <stop offset="100%" stopColor="#E2665A" stopOpacity="0" />
                        </linearGradient>
                    </defs>

                    <path
                        d={`${expensePath} L${width},${height} L0,${height} Z`}
                        fill={`url(#${gradientId}-expense)`}
                        stroke="none"
                    />
                    <path d={expensePath} fill="none" stroke="#E2665A" strokeWidth="2.5" strokeLinecap="round" />
                    <path d={incomePath} fill="none" stroke="#159FA4" strokeWidth="2.5" strokeLinecap="round" />

                    {labels.map((_, index) => {
                        const x = (index / (labels.length - 1 || 1)) * width
                        return (
                            <rect
                                key={index}
                                x={x - width / labels.length / 2}
                                y={0}
                                width={width / labels.length}
                                height={height}
                                fill="transparent"
                                onMouseEnter={() => setHoverIndex(index)}
                                onMouseLeave={() => setHoverIndex(null)}
                                onFocus={() => setHoverIndex(index)}
                                onBlur={() => setHoverIndex(null)}
                                tabIndex={0}
                                role="img"
                                aria-label={`${labels[index]}: income ₹${income[index].toLocaleString('en-IN')}, expense ₹${expense[index].toLocaleString('en-IN')}`}
                            />
                        )
                    })}

                    {hoverIndex !== null && (
                        <line
                            x1={(hoverIndex / (labels.length - 1 || 1)) * width}
                            x2={(hoverIndex / (labels.length - 1 || 1)) * width}
                            y1={0}
                            y2={height}
                            stroke="#8A929C"
                            strokeDasharray="4 4"
                            strokeWidth="1"
                        />
                    )}
                </svg>

                {hoverIndex !== null && (
                    <div
                        className="pointer-events-none absolute top-0 rounded-xl border border-slate-100 bg-white px-3 py-2 text-xs font-semibold text-heading shadow-lg"
                        style={{
                            left: `${(hoverIndex / (labels.length - 1 || 1)) * 100}%`,
                            transform: 'translate(-50%, -110%)',
                        }}
                    >
                        <p className="text-slate-500">{labels[hoverIndex]}</p>
                        <p className="text-brand-700">
                            Income ₹{income[hoverIndex].toLocaleString('en-IN')}
                        </p>
                        <p className="text-[#C7513F]">
                            Expense ₹{expense[hoverIndex].toLocaleString('en-IN')}
                        </p>
                    </div>
                )}

                <div className="mt-1 flex justify-between text-xs font-semibold text-slate-400">
                    {labels.map((label) => (
                        <span key={label}>{label}</span>
                    ))}
                </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {categorySummary.map((category) => (
                    <div
                        key={category.label}
                        className="rounded-2xl border border-slate-100 bg-white/70 p-3"
                    >
                        <span
                            className="inline-block h-2 w-2 rounded-full"
                            style={{ backgroundColor: category.color }}
                            aria-hidden="true"
                        />
                        <p className="mt-2 text-xs font-semibold text-slate-500">
                            {category.label}
                        </p>
                        <p className="text-sm font-bold text-heading">
                            ₹{category.value.toLocaleString('en-IN')}
                        </p>
                    </div>
                ))}
            </div>
        </article>
    )
}

function SpendingChartHeader({ period, onChange }) {
    return (
        <div className="flex items-center justify-between gap-4">
            <div>
                <h2 className="text-xl font-bold text-heading">
                    Spending overview
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                    Income versus expense trend.
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
                        onClick={() => onChange(option)}
                        className={`min-h-9 rounded-lg px-3 capitalize transition ${
                            period === option
                                ? 'bg-brand-600 text-white shadow-sm'
                                : 'text-slate-500 hover:text-brand-700'
                        }`}
                    >
                        {option}
                    </button>
                ))}
            </div>
        </div>
    )
}
