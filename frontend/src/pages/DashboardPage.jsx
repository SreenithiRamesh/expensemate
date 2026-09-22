import {
    ArrowUpRight,
    IndianRupee,
    ReceiptText,
    Users,
    WalletCards,
} from 'lucide-react'

const summaryCards = [
    {
        title: 'Total balance',
        value: '₹0.00',
        note: 'Available across accounts',
        icon: IndianRupee,
        gradient:
            'from-brand-600 to-brand-500',
    },
    {
        title: 'Monthly expenses',
        value: '₹0.00',
        note: 'No expenses recorded',
        icon: ReceiptText,
        gradient:
            'from-cyan-600 to-sky-500',
    },
    {
        title: 'Active groups',
        value: '0',
        note: 'No shared groups yet',
        icon: Users,
        gradient:
            'from-violet-600 to-indigo-500',
    },
]

function DashboardPage() {
    return (
        <div>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <p className="text-sm font-bold tracking-[0.16em] text-brand-700 uppercase">
                        Financial command centre
                    </p>

                    <h1 className="mt-2 text-3xl font-black tracking-tight text-heading sm:text-4xl">
                        Welcome to ExpenseMate
                    </h1>

                    <p className="mt-2 text-slate-500">
                        Your financial overview will appear here.
                    </p>
                </div>

                <button
                    type="button"
                    className="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition hover:-translate-y-0.5"
                >
                    <WalletCards size={18} />
                    Add expense
                </button>
            </div>

            <section className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                {summaryCards.map((card) => {
                    const Icon = card.icon

                    return (
                        <article
                            key={card.title}
                            className="relative overflow-hidden rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl transition hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-800/10"
                        >
                            <div
                                aria-hidden="true"
                                className={`absolute -top-12 -right-12 h-32 w-32 rounded-full bg-linear-to-br ${card.gradient} opacity-10 blur-2xl`}
                            />

                            <div className="relative flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-semibold text-slate-500">
                                        {card.title}
                                    </p>

                                    <p className="mt-3 text-3xl font-black text-heading">
                                        {card.value}
                                    </p>

                                    <p className="mt-2 text-sm text-slate-500">
                                        {card.note}
                                    </p>
                                </div>

                                <span
                                    className={`grid h-12 w-12 place-items-center rounded-2xl bg-linear-to-br ${card.gradient} text-white shadow-lg`}
                                >
                                    <Icon size={22} />
                                </span>
                            </div>
                        </article>
                    )
                })}
            </section>

            <section className="mt-6 grid gap-6 xl:grid-cols-[1.5fr_1fr]">
                <article className="min-h-80 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
                    <div className="flex items-center justify-between">
                        <div>
                            <h2 className="text-xl font-bold text-heading">
                                Spending overview
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                Monthly trends will appear here.
                            </p>
                        </div>

                        <ArrowUpRight className="text-brand-600" />
                    </div>

                    <div className="mt-8 grid min-h-52 place-items-center rounded-2xl bg-linear-to-br from-brand-50 to-sky-50">
                        <p className="text-sm font-semibold text-slate-500">
                            Chart foundation ready
                        </p>
                    </div>
                </article>

                <article className="min-h-80 rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl">
                    <h2 className="text-xl font-bold text-heading">
                        Recent activity
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        Your latest financial actions.
                    </p>

                    <div className="mt-8 grid min-h-52 place-items-center rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 px-5 text-center">
                        <div>
                            <ReceiptText
                                size={30}
                                className="mx-auto text-brand-500"
                            />

                            <p className="mt-3 font-bold text-heading">
                                No activity yet
                            </p>

                            <p className="mt-1 text-sm text-slate-500">
                                New transactions will appear here.
                            </p>
                        </div>
                    </div>
                </article>
            </section>
        </div>
    )
}

export default DashboardPage