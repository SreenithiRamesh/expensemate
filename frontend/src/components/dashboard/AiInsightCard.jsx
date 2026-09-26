import { ArrowRight } from 'lucide-react'

import { DashboardMascot } from './DashboardMascot'

export function AiInsightCard({
                                  insight = 'Dining expenses are higher than your recent average. Review before changing your budget.',
                                  onViewInsight,
                                  unavailable = false,
                              }) {
    if (unavailable) {
        return (
            <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
                <div className="flex items-center gap-3">
                    <span className="grid h-10 w-10 place-items-center rounded-2xl bg-slate-100 text-slate-400">
                        <DashboardMascot variant="sparkle" className="h-6 w-6 opacity-50" />
                    </span>
                    <div>
                        <p className="font-bold text-heading">
                            AI insight unavailable
                        </p>
                        <p className="text-sm text-slate-500">
                            Check back once you&rsquo;ve logged a few more expenses.
                        </p>
                    </div>
                </div>
            </article>
        )
    }

    return (
        <article className="relative overflow-hidden rounded-3xl border border-white/80 bg-linear-to-br from-brand-600 via-brand-500 to-sky-500 p-6 text-white shadow-lg">
            <div
                aria-hidden="true"
                className="absolute -top-10 -right-10 h-32 w-32 animate-pulse rounded-full bg-white/10 blur-2xl motion-reduce:animate-none"
            />

            <div className="relative flex items-start gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-white/15">
                    <DashboardMascot variant="sparkle" className="h-6 w-6" />
                </span>

                <div className="min-w-0">
                    <span className="inline-flex items-center rounded-full bg-white/15 px-2.5 py-1 text-xs font-bold">
                        AI-generated suggestion
                    </span>

                    <p className="mt-3 text-sm leading-relaxed text-white/95">
                        {insight}
                    </p>

                    <button
                        type="button"
                        onClick={onViewInsight}
                        className="mt-4 inline-flex items-center gap-1.5 rounded-xl bg-white/15 px-3.5 py-2 text-sm font-bold transition hover:bg-white/25 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                    >
                        View insight
                        <ArrowRight size={15} aria-hidden="true" />
                    </button>

                    <p className="mt-3 text-xs text-white/70">
                        Advisory only — not financial advice.
                    </p>
                </div>
            </div>
        </article>
    )
}
