import { ArrowUpRight, ReceiptText, Users } from 'lucide-react'
import { useInViewReveal } from './useInViewReveal.js'

const recentTransaction = {
    merchant: 'Swiggy',
    category: 'Food & dining',
    amount: '₹320',
    time: 'Today, 8:42 PM',
}

function ProductPreview() {
    const [ref, isVisible] = useInViewReveal()

    return (
        <section id="security" className="mx-auto max-w-7xl px-5 py-20 sm:px-8 lg:px-12">
            <div className="grid items-center gap-14 lg:grid-cols-[0.9fr_1.1fr]">
                <div ref={ref} className={`reveal ${isVisible ? 'is-visible' : ''}`}>
                    <h2 className="font-display text-3xl font-extrabold tracking-tight text-heading sm:text-4xl">
                        A dashboard that shows exactly where you stand
                    </h2>
                    <p className="mt-4 max-w-md text-lg text-slate-600">
                        Personal spending, shared balances, and budget progress sit
                        side by side — updated the moment an expense or settlement
                        happens.
                    </p>
                    <ul className="mt-6 space-y-3 text-sm font-medium text-slate-600">
                        <li className="flex items-center gap-2">
                            <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
                            Encrypted authentication with JWT-backed sessions
                        </li>
                        <li className="flex items-center gap-2">
                            <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
                            Balances calculated live, never stored stale
                        </li>
                        <li className="flex items-center gap-2">
                            <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
                            Every settlement recorded in a shared activity timeline
                        </li>
                    </ul>
                </div>

                <div className="relative overflow-hidden rounded-[2rem] border border-white/80 bg-white/80 p-6 shadow-card backdrop-blur-2xl sm:p-8">
                    <div
                        aria-hidden="true"
                        className="absolute inset-x-0 top-0 h-1 bg-linear-to-r from-brand-500 via-cyan-400 to-sky-400"
                    />

                    <div className="grid gap-4 sm:grid-cols-2">
                        <div className="rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 p-4 text-white shadow-lg shadow-brand-700/20">
                            <p className="text-xs font-medium text-white/80">This month</p>
                            <p className="mt-1 text-2xl font-bold">₹18,400</p>
                            <p className="mt-1 text-xs text-white/75">of ₹25,000 budget</p>
                            <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-white/25">
                                <div className="h-full w-[74%] rounded-full bg-white" />
                            </div>
                        </div>

                        <div className="rounded-2xl border border-slate-100 bg-white/90 p-4 shadow-sm">
                            <p className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
                                <Users size={13} /> Weekend trip group
                            </p>
                            <p className="mt-1 text-2xl font-bold text-[#2F9E6F]">+₹1,250</p>
                            <p className="mt-1 text-xs text-slate-400">You are owed overall</p>
                        </div>
                    </div>

                    <div className="mt-4 rounded-2xl border border-slate-100 bg-white/90 p-4 shadow-sm">
                        <div className="flex items-center justify-between">
                            <p className="text-sm font-semibold text-heading">Spending trend</p>
                            <ArrowUpRight size={16} className="text-brand-600" />
                        </div>
                        <svg viewBox="0 0 280 70" className="mt-3 h-16 w-full" aria-hidden="true">
                            <defs>
                                <linearGradient id="previewChartFill" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#27B8B5" stopOpacity="0.25" />
                                    <stop offset="100%" stopColor="#27B8B5" stopOpacity="0" />
                                </linearGradient>
                            </defs>
                            <polyline
                                points="0,55 40,45 80,50 120,30 160,38 200,18 240,26 280,10 280,70 0,70"
                                fill="url(#previewChartFill)"
                                stroke="none"
                            />
                            <polyline
                                points="0,55 40,45 80,50 120,30 160,38 200,18 240,26 280,10"
                                fill="none"
                                stroke="#27B8B5"
                                strokeWidth="3"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                        </svg>
                    </div>

                    <div className="mt-4 flex items-center gap-3 rounded-2xl border border-slate-100 bg-white/90 p-4 shadow-sm">
                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
                            <ReceiptText size={18} />
                        </span>
                        <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-semibold text-heading">
                                {recentTransaction.merchant} · {recentTransaction.category}
                            </p>
                            <p className="text-xs text-slate-400">{recentTransaction.time}</p>
                        </div>
                        <p className="text-sm font-bold text-heading">{recentTransaction.amount}</p>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default ProductPreview