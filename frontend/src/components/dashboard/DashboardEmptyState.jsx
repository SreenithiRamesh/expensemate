import { DashboardMascot } from './DashboardMascot'

/**
 * Small, presentational empty state used inside dashboard cards.
 * Not a replacement for the app-wide EmptyState.jsx — this one is
 * sized to sit inside a card rather than a full page.
 */
export function DashboardEmptyState({
                                        title,
                                        description,
                                        variant = 'receipt',
                                        action,
                                    }) {
    return (
        <div className="grid min-h-52 place-items-center rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 px-5 py-8 text-center">
            <div className="flex flex-col items-center">
                <DashboardMascot variant={variant} />

                <p className="mt-2 font-bold text-heading">
                    {title}
                </p>

                {description && (
                    <p className="mt-1 max-w-xs text-sm text-slate-500">
                        {description}
                    </p>
                )}

                {action && <div className="mt-4">{action}</div>}
            </div>
        </div>
    )
}
