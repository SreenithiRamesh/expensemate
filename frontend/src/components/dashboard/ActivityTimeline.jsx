import { CheckCircle2, HandCoins, ReceiptText, UserPlus } from 'lucide-react'

const iconByType = {
    expense: ReceiptText,
    member: UserPlus,
    settlement: HandCoins,
    budget: CheckCircle2,
}

const tintByType = {
    expense: 'bg-brand-50 text-brand-700',
    member: 'bg-violet-50 text-violet-700',
    settlement: 'bg-emerald-50 text-emerald-700',
    budget: 'bg-amber-50 text-amber-700',
}

export function ActivityTimeline({ items = [] }) {
    if (items.length === 0) return null

    return (
        <section className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <h2 className="text-xl font-bold text-heading">Recent activity</h2>

            <ol className="mt-5 space-y-5">
                {items.map((item, index) => {
                    const Icon = iconByType[item.type] ?? ReceiptText

                    return (
                        <li key={item.id} className="relative flex gap-4 pl-1">
                            {index !== items.length - 1 && (
                                <span
                                    aria-hidden="true"
                                    className="absolute top-9 left-[19px] h-[calc(100%-4px)] w-px bg-slate-100"
                                />
                            )}

                            <span
                                className={`grid h-9 w-9 shrink-0 place-items-center rounded-full ${tintByType[item.type] ?? 'bg-slate-100 text-slate-600'}`}
                            >
                                <Icon size={16} aria-hidden="true" />
                            </span>

                            <div className="min-w-0 pb-1">
                                <p className="text-sm font-semibold text-heading">
                                    {item.title}
                                </p>
                                <p className="text-xs text-slate-500">
                                    {item.timestamp}
                                </p>
                            </div>
                        </li>
                    )
                })}
            </ol>
        </section>
    )
}
