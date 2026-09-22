import { HandCoins, PlusCircle, Target, UserPlus } from 'lucide-react'

const defaultActions = [
    { label: 'Add expense', icon: PlusCircle, key: 'add-expense' },
    { label: 'Create group', icon: UserPlus, key: 'create-group' },
    { label: 'Settle balance', icon: HandCoins, key: 'settle-balance' },
    { label: 'Add budget', icon: Target, key: 'add-budget' },
]

export function QuickActions({ onAction }) {
    return (
        <section aria-label="Quick actions" className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <h2 className="text-xl font-bold text-heading">Quick actions</h2>

            <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {defaultActions.map((action) => {
                    const Icon = action.icon

                    return (
                        <button
                            key={action.key}
                            type="button"
                            onClick={() => onAction?.(action.key)}
                            aria-label={action.label}
                            className="flex min-h-20 flex-col items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-white/70 p-4 text-center transition hover:-translate-y-0.5 hover:border-brand-200 hover:bg-brand-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 motion-reduce:hover:translate-y-0"
                        >
                            <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-50 text-brand-700">
                                <Icon size={18} aria-hidden="true" />
                            </span>
                            <span className="text-xs font-bold text-heading sm:text-sm">
                                {action.label}
                            </span>
                        </button>
                    )
                })}
            </div>
        </section>
    )
}
