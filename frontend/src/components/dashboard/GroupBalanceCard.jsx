import { Users2 } from 'lucide-react'

function formatCurrency(value) {
    return `₹${Math.abs(value).toLocaleString('en-IN')}`
}

function AvatarStack({ members = [] }) {
    return (
        <div className="flex -space-x-2" aria-hidden="true">
            {members.slice(0, 4).map((member, index) => (
                <span
                    key={member}
                    className="grid h-8 w-8 place-items-center rounded-full border-2 border-white bg-linear-to-br from-brand-600 to-sky-500 text-xs font-black text-white"
                    style={{ zIndex: 10 - index }}
                >
                    {member}
                </span>
            ))}
        </div>
    )
}

export function GroupBalanceCard({
                                     groupName = 'Goa Trip',
                                     memberInitials = ['S', 'A', 'R', 'K'],
                                     memberCount = 4,
                                     balance = 1250,
                                     direction = 'owed',
                                     onSettleUp,
                                 }) {
    const isOwed = direction === 'owed'

    return (
        <article className="rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
            <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                    <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-linear-to-br from-violet-600 to-indigo-500 text-white shadow-lg">
                        <Users2 size={20} aria-hidden="true" />
                    </span>

                    <div>
                        <p className="font-bold text-heading">{groupName}</p>
                        <p className="text-xs text-slate-500">
                            {memberCount} members
                        </p>
                    </div>
                </div>

                <AvatarStack members={memberInitials} />
            </div>

            <div className="mt-5 flex items-end justify-between gap-3">
                <div>
                    <p className="text-xs font-semibold text-slate-500">
                        {isOwed ? 'You are owed' : 'You owe'}
                    </p>
                    <p
                        className={`mt-1 flex items-center gap-1.5 text-2xl font-black ${
                            isOwed ? 'text-emerald-600' : 'text-rose-600'
                        }`}
                    >
                        <span
                            aria-hidden="true"
                            className={`inline-block h-2.5 w-2.5 rounded-full ${
                                isOwed ? 'bg-emerald-500' : 'bg-rose-500'
                            }`}
                        />
                        {formatCurrency(balance)}
                    </p>
                </div>

                <button
                    type="button"
                    onClick={onSettleUp}
                    className="inline-flex min-h-11 items-center justify-center rounded-xl bg-heading px-4 py-2.5 text-sm font-bold text-white shadow-sm transition hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 motion-reduce:hover:translate-y-0"
                >
                    Settle up
                </button>
            </div>
        </article>
    )
}
