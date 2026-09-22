import {
    ArrowRight,
    BarChart3,
    Check,
    ShieldCheck,
    Sparkles,
    Users,
    WalletCards,
} from 'lucide-react'

const features = [
    {
        icon: WalletCards,
        title: 'Track expenses',
        description:
            'Manage personal spending, budgets, and recurring payments.',
    },
    {
        icon: Users,
        title: 'Split together',
        description:
            'Create groups, share expenses, and settle balances clearly.',
    },
    {
        icon: BarChart3,
        title: 'Understand money',
        description:
            'See useful insights and make confident financial decisions.',
    },
]

function App() {
    return (
        <main className="relative isolate min-h-screen overflow-hidden">
            {/* Decorative gradient glow */}
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -top-44 -left-40 h-96 w-96 rounded-full bg-brand-300/30 blur-3xl"
            />

            <div
                aria-hidden="true"
                className="pointer-events-none absolute right-[-8rem] bottom-[-10rem] h-[30rem] w-[30rem] rounded-full bg-sky-300/20 blur-3xl"
            />

            <div className="relative mx-auto flex min-h-screen w-full max-w-7xl flex-col px-5 py-6 sm:px-8 lg:px-12">
                {/* Header */}
                <header className="flex items-center justify-between">
                    <a
                        href="/"
                        aria-label="ExpenseMate home"
                        className="group flex items-center gap-3"
                    >
                        <span className="grid h-11 w-11 place-items-center rounded-2xl bg-linear-to-br from-brand-500 via-brand-600 to-brand-800 text-white shadow-lg shadow-brand-600/25 transition-transform duration-300 group-hover:-rotate-3 group-hover:scale-105">
                            <WalletCards
                                size={22}
                                strokeWidth={2.2}
                            />
                        </span>

                        <span className="text-xl font-bold tracking-tight text-heading">
                            Expense
                            <span className="bg-linear-to-r from-brand-600 to-sky-500 bg-clip-text text-transparent">
                                Mate
                            </span>
                        </span>
                    </a>

                    <div className="hidden items-center gap-2 rounded-full border border-white/80 bg-white/65 px-4 py-2 text-sm font-medium text-brand-800 shadow-sm backdrop-blur-xl sm:flex">
                        <ShieldCheck size={16} />
                        Backend V1 secured
                    </div>
                </header>

                {/* Hero */}
                <section className="flex flex-1 items-center py-14 lg:py-20">
                    <div className="grid w-full items-center gap-14 lg:grid-cols-[1.05fr_0.95fr]">
                        <div className="max-w-2xl">
                            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-brand-200/80 bg-white/70 px-4 py-2 text-sm font-semibold text-brand-800 shadow-sm backdrop-blur-xl">
                                <Sparkles
                                    size={16}
                                    className="text-brand-600"
                                />
                                Smart personal and shared finance
                            </div>

                            <h1 className="text-5xl leading-[1.05] font-black tracking-[-0.045em] text-heading sm:text-6xl lg:text-7xl">
                                Your money,{' '}
                                <span className="mt-2 block bg-linear-to-r from-brand-600 via-brand-500 to-sky-500 bg-clip-text text-transparent">
                                    beautifully organized.
                                </span>
                            </h1>

                            <p className="mt-7 max-w-xl text-lg leading-8 text-slate-600 sm:text-xl">
                                Track personal spending, manage shared expenses,
                                settle balances, and receive intelligent insights
                                from one secure workspace.
                            </p>

                            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                                <button
                                    type="button"
                                    className="group inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-6 py-3 font-bold text-white shadow-button transition-all duration-300 hover:-translate-y-0.5 hover:from-brand-700 hover:to-brand-600 hover:shadow-xl hover:shadow-brand-600/25 active:translate-y-0"
                                >
                                    Foundation ready

                                    <ArrowRight
                                        size={19}
                                        className="transition-transform duration-300 group-hover:translate-x-1"
                                    />
                                </button>

                                <button
                                    type="button"
                                    className="inline-flex min-h-12 items-center justify-center rounded-2xl border border-white/90 bg-white/70 px-6 py-3 font-semibold text-heading shadow-sm backdrop-blur-xl transition-all duration-300 hover:-translate-y-0.5 hover:bg-white hover:shadow-md"
                                >
                                    Explore architecture
                                </button>
                            </div>

                            <div className="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-sm font-medium text-slate-600">
                                {[
                                    'Secure authentication',
                                    'Shared settlements',
                                    'AI-assisted insights',
                                ].map((item) => (
                                    <span
                                        key={item}
                                        className="inline-flex items-center gap-2"
                                    >
                                        <span className="grid h-5 w-5 place-items-center rounded-full bg-brand-100 text-brand-700">
                                            <Check
                                                size={13}
                                                strokeWidth={3}
                                            />
                                        </span>

                                        {item}
                                    </span>
                                ))}
                            </div>
                        </div>

                        {/* Feature overview card */}
                        <div className="relative mx-auto w-full max-w-xl">
                            <div
                                aria-hidden="true"
                                className="absolute inset-8 rounded-[3rem] bg-linear-to-br from-brand-400/35 to-sky-400/25 blur-3xl"
                            />

                            <div className="relative overflow-hidden rounded-[2rem] border border-white/80 bg-white/75 p-5 shadow-card backdrop-blur-2xl sm:p-7">
                                <div
                                    aria-hidden="true"
                                    className="absolute inset-x-0 top-0 h-1 bg-linear-to-r from-brand-500 via-cyan-400 to-sky-400"
                                />

                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <p className="text-sm font-semibold text-slate-500">
                                            Financial overview
                                        </p>

                                        <h2 className="mt-1 text-2xl font-bold text-heading">
                                            Everything in one place
                                        </h2>
                                    </div>

                                    <span className="rounded-xl bg-linear-to-br from-brand-100 to-cyan-50 p-3 text-brand-700">
                                        <BarChart3 size={22} />
                                    </span>
                                </div>

                                <div className="mt-7 grid gap-4">
                                    {features.map(
                                        (feature, index) => {
                                            const Icon = feature.icon

                                            let iconStyle =
                                                'bg-linear-to-br from-sky-100 to-cyan-100 text-sky-700'

                                            if (index === 0) {
                                                iconStyle =
                                                    'bg-linear-to-br from-brand-500 to-brand-700 text-white'
                                            }

                                            if (index === 1) {
                                                iconStyle =
                                                    'bg-linear-to-br from-cyan-100 to-brand-100 text-brand-700'
                                            }

                                            return (
                                                <article
                                                    key={feature.title}
                                                    className="group flex items-start gap-4 rounded-2xl border border-slate-100 bg-white/80 p-4 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg hover:shadow-brand-700/10"
                                                >
                                                    <span
                                                        className={`grid h-11 w-11 shrink-0 place-items-center rounded-xl ${iconStyle}`}
                                                    >
                                                        <Icon size={21} />
                                                    </span>

                                                    <div>
                                                        <h3 className="font-bold text-heading">
                                                            {feature.title}
                                                        </h3>

                                                        <p className="mt-1 text-sm leading-6 text-slate-500">
                                                            {
                                                                feature.description
                                                            }
                                                        </p>
                                                    </div>
                                                </article>
                                            )
                                        },
                                    )}
                                </div>

                                <div className="mt-5 rounded-2xl bg-linear-to-r from-brand-700 via-brand-600 to-cyan-600 p-5 text-white shadow-lg shadow-brand-700/20">
                                    <p className="text-sm font-medium text-white/75">
                                        ExpenseMate foundation
                                    </p>

                                    <div className="mt-2 flex items-end justify-between gap-4">
                                        <p className="text-2xl font-bold">
                                            Ready to build
                                        </p>

                                        <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold backdrop-blur-md">
                                            M30
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Footer */}
                <footer className="flex flex-col gap-2 border-t border-white/70 py-5 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
                    <p>
                        ExpenseMate — smarter money management.
                    </p>

                    <p>
                        React · Spring Boot · PostgreSQL
                    </p>
                </footer>
            </div>
        </main>
    )
}

export default App