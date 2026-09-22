import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import WalletMascot from './WalletMascot.jsx'

function FinalCtaSection() {
    return (
        <section className="mx-auto max-w-7xl px-5 pb-20 sm:px-8 lg:px-12">
            <div className="relative overflow-hidden rounded-[2.5rem] bg-linear-to-br from-brand-700 via-brand-600 to-[#239db7] px-8 py-14 text-white shadow-xl shadow-brand-700/25 sm:px-14 sm:py-16">
                <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -top-16 -right-10 h-64 w-64 rounded-full bg-white/10 blur-3xl"
                />

                <div className="relative flex flex-col items-center gap-8 text-center lg:flex-row lg:justify-between lg:text-left">
                    <div className="max-w-xl">
                        <h2 className="font-display text-3xl font-extrabold tracking-tight sm:text-4xl">
                            Ready to make money management easier?
                        </h2>
                        <p className="mt-4 text-lg text-white/85">
                            Set up personal budgets and your first shared group in
                            under five minutes — it&apos;s free to start.
                        </p>
                        <Link
                            to="/register"
                            className="group mt-8 inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-white px-6 py-3 font-bold text-brand-700 shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl"
                        >
                            Get started with ExpenseMate
                            <ArrowRight
                                size={19}
                                className="transition-transform duration-300 group-hover:translate-x-1"
                            />
                        </Link>
                    </div>

                    <WalletMascot
                        className="h-36 w-36 shrink-0 animate-mascot-breathe sm:h-44 sm:w-44"
                        animated
                    />
                </div>
            </div>
        </section>
    )
}

export default FinalCtaSection