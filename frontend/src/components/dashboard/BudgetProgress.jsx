import { DashboardMascot } from './DashboardMascot'

const WARNING_THRESHOLD = 85

const DEFAULT_CATEGORIES = [
    { label: 'Food', used: 6240, budget: 8000 },
    { label: 'Travel', used: 4120, budget: 5000 },
    { label: 'Shopping', used: 3080, budget: 4500 },
    { label: 'Bills', used: 5010, budget: 6000 },
]

function normalizeAmount(value) {
    return Number.isFinite(value) ? Math.max(value, 0) : 0
}

function formatCurrency(value) {
    return `₹${value.toLocaleString('en-IN', {
        maximumFractionDigits: 2,
    })}`
}

function getUsagePercent(used, budget) {
    if (budget <= 0) {
        return null
    }

    return (used / budget) * 100
}

function ProgressBar({ percent, warning, label, valueText }) {
    const safePercent = Number.isFinite(percent)
        ? Math.min(Math.max(percent, 0), 100)
        : 0

    return (
        <div
            role="progressbar"
            aria-label={label}
            aria-valuenow={Math.round(safePercent)}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuetext={valueText}
            className="h-3 w-full overflow-hidden rounded-full bg-slate-100"
        >
            <div
                className={`budget-progress-fill h-full rounded-full ${
                    warning
                        ? 'bg-linear-to-r from-amber-500 to-rose-500'
                        : 'bg-linear-to-r from-brand-600 to-brand-400'
                }`}
                style={{ width: `${safePercent}%` }}
            />
        </div>
    )
}

export function BudgetProgress({
                                   budget = 30000,
                                   used = 18450,
                                   categories = DEFAULT_CATEGORIES,
                               }) {
    const safeBudget = normalizeAmount(budget)
    const safeUsed = normalizeAmount(used)

    const hasBudget = safeBudget > 0
    const remaining = Math.max(safeBudget - safeUsed, 0)
    const overspent = Math.max(safeUsed - safeBudget, 0)

    const percent = getUsagePercent(safeUsed, safeBudget)
    const isWarning =
        (percent !== null && percent >= WARNING_THRESHOLD) ||
        (!hasBudget && safeUsed > 0)

    const overallValueText = hasBudget
        ? `${formatCurrency(safeUsed)} used out of ${formatCurrency(safeBudget)}; ${Math.round(percent)} percent used`
        : `${formatCurrency(safeUsed)} spent; no budget set`

    return (
        <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h2 className="text-xl font-bold text-heading">
                        Budget progress
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        This month&rsquo;s spending against your budget.
                    </p>
                </div>

                <div aria-hidden="true" className="hidden sm:block">
                    <DashboardMascot variant="piggy" />
                </div>
            </div>

            <div className="mt-5 flex flex-wrap items-baseline justify-between gap-2">
                <p className="text-2xl font-black text-heading">
                    {formatCurrency(safeUsed)}
                    <span className="ml-1 text-sm font-semibold text-slate-400">
                        of {formatCurrency(safeBudget)}
                    </span>
                </p>

                <p
                    className={`text-sm font-bold ${
                        isWarning ? 'text-rose-600' : 'text-brand-700'
                    }`}
                >
                    {!hasBudget
                        ? 'No budget set'
                        : overspent > 0
                            ? `${formatCurrency(overspent)} over budget`
                            : `${formatCurrency(remaining)} remaining`}
                </p>
            </div>

            <div className="mt-3">
                <ProgressBar
                    percent={percent}
                    warning={isWarning}
                    label="Monthly budget used"
                    valueText={overallValueText}
                />
            </div>

            {hasBudget && isWarning && (
                <p className="mt-2 text-xs font-semibold text-rose-600">
                    Heads up — you&rsquo;ve used {Math.round(percent)}% of
                    this month&rsquo;s budget.
                </p>
            )}

            {!hasBudget && safeUsed > 0 && (
                <p className="mt-2 text-xs font-semibold text-slate-500">
                    Set a budget to compare your spending against a limit.
                </p>
            )}

            <ul className="mt-6 space-y-4">
                {categories.map((category) => {
                    const categoryUsed = normalizeAmount(category.used)
                    const categoryBudget = normalizeAmount(category.budget)
                    const categoryPercent = getUsagePercent(
                        categoryUsed,
                        categoryBudget,
                    )

                    const categoryWarning =
                        (categoryPercent !== null &&
                            categoryPercent >= WARNING_THRESHOLD) ||
                        (categoryBudget === 0 && categoryUsed > 0)

                    const categoryValueText =
                        categoryPercent === null
                            ? `${formatCurrency(categoryUsed)} spent; no budget set`
                            : `${formatCurrency(categoryUsed)} used out of ${formatCurrency(categoryBudget)}; ${Math.round(categoryPercent)} percent used`

                    return (
                        <li key={category.label}>
                            <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                                <span className="font-semibold text-heading">
                                    {category.label}
                                </span>

                                <span className="text-slate-500">
                                    {formatCurrency(categoryUsed)} /{' '}
                                    {formatCurrency(categoryBudget)}
                                </span>
                            </div>

                            <div className="mt-1.5">
                                <ProgressBar
                                    percent={categoryPercent}
                                    warning={categoryWarning}
                                    label={`${category.label} budget used`}
                                    valueText={categoryValueText}
                                />
                            </div>
                        </li>
                    )
                })}
            </ul>
        </article>
    )
}