import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react'

import { cn } from '../../utils/cn'

const variantStyles = {
    teal: 'from-brand-600 to-brand-500',
    sky: 'from-cyan-600 to-sky-500',
    green: 'from-emerald-600 to-brand-500',
    rose: 'from-rose-500 to-amber-500',
}

const trendStyles = {
    up: 'text-brand-700 bg-brand-50',
    down: 'text-rose-600 bg-rose-50',
    neutral: 'text-slate-500 bg-slate-100',
}

const trendIcon = {
    up: ArrowUpRight,
    down: ArrowDownRight,
    neutral: Minus,
}

/**
 * Builds a tiny inline sparkline path from an array of numbers.
 * Pure presentational — no chart library required.
 */
function Sparkline({ data = [], stroke = '#159FA4' }) {
    if (!data.length) return null

    const width = 96
    const height = 28
    const max = Math.max(...data)
    const min = Math.min(...data)
    const range = max - min || 1

    const points = data
        .map((value, index) => {
            const x = (index / (data.length - 1 || 1)) * width
            const y = height - ((value - min) / range) * height
            return `${x.toFixed(1)},${y.toFixed(1)}`
        })
        .join(' ')

    return (
        <svg
            viewBox={`0 0 ${width} ${height}`}
            className="h-7 w-24"
            aria-hidden="true"
            focusable="false"
        >
            <polyline
                points={points}
                fill="none"
                stroke={stroke}
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    )
}

export function SummaryCard({
                                title,
                                value,
                                change,
                                trend = 'neutral',
                                icon: Icon,
                                variant = 'teal',
                                sparklineData,
                            }) {
    const TrendIcon = trendIcon[trend] ?? Minus
    const trendLabel =
        trend === 'up'
            ? 'Trending up'
            : trend === 'down'
                ? 'Trending down'
                : 'No change'

    return (
        <article
            className={cn(
                'group relative overflow-hidden rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl transition duration-300',
                'hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-800/10',
                'motion-reduce:transition-none motion-reduce:hover:translate-y-0',
            )}
        >
            <div
                aria-hidden="true"
                className={cn(
                    'absolute -top-12 -right-12 h-32 w-32 rounded-full bg-linear-to-br opacity-0 blur-2xl transition-opacity duration-500 group-hover:opacity-20',
                    variantStyles[variant],
                )}
            />

            <div className="relative flex items-start justify-between">
                <div className="min-w-0">
                    <p className="text-sm font-semibold text-slate-500">
                        {title}
                    </p>

                    <p className="mt-3 truncate text-3xl font-black tracking-tight text-heading">
                        {value}
                    </p>

                    {change && (
                        <span
                            className={cn(
                                'mt-3 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-bold',
                                trendStyles[trend],
                            )}
                        >
                            <TrendIcon size={13} aria-hidden="true" />
                            <span>{change}</span>
                            <span className="sr-only">{trendLabel}</span>
                        </span>
                    )}
                </div>

                {Icon && (
                    <span
                        className={cn(
                            'grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-linear-to-br text-white shadow-lg',
                            variantStyles[variant],
                        )}
                    >
                        <Icon size={22} aria-hidden="true" />
                    </span>
                )}
            </div>

            {sparklineData && (
                <div className="relative mt-4 flex justify-end">
                    <Sparkline
                        data={sparklineData}
                        stroke={
                            trend === 'down' ? '#E2665A' : '#159FA4'
                        }
                    />
                </div>
            )}
        </article>
    )
}
