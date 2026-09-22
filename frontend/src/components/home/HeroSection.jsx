import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ShieldCheck, Sparkles, TrendingUp, Users2 } from 'lucide-react'
import WalletMascot from './WalletMascot.jsx'

const trustItems = [
    { icon: ShieldCheck, label: 'Secure authentication' },
    { icon: Users2, label: 'Accurate shared balances' },
    { icon: Sparkles, label: 'AI-assisted insights' },
]

function HeroSection() {
    const [heroImageFailed, setHeroImageFailed] = useState(false)
    const stageRef = useRef(null)

    // Lightweight cursor parallax on the illustration only — desktop, and
    // skipped entirely when the user prefers reduced motion.
    useEffect(() => {
        const stage = stageRef.current
        if (!stage) return undefined

        const prefersReducedMotion = window.matchMedia(
            '(prefers-reduced-motion: reduce)',
        ).matches
        const isNarrowViewport = window.matchMedia('(max-width: 1024px)').matches

        if (prefersReducedMotion || isNarrowViewport) {
            return undefined
        }

        function handlePointerMove(event) {
            const bounds = stage.getBoundingClientRect()
            const relativeX = (event.clientX - bounds.left) / bounds.width - 0.5
            const relativeY = (event.clientY - bounds.top) / bounds.height - 0.5
            stage.style.setProperty('--tilt-x', `${relativeY * -4}deg`)
            stage.style.setProperty('--tilt-y', `${relativeX * 6}deg`)
        }

        function resetTilt() {
            stage.style.setProperty('--tilt-x', '0deg')
            stage.style.setProperty('--tilt-y', '0deg')
        }

        stage.addEventListener('pointermove', handlePointerMove)
        stage.addEventListener('pointerleave', resetTilt)
        return () => {
            stage.removeEventListener('pointermove', handlePointerMove)
            stage.removeEventListener('pointerleave', resetTilt)
        }
    }, [])

    return (
        <section className="relative isolate overflow-hidden">
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -top-44 -left-40 h-96 w-96 rounded-full bg-brand-300/30 blur-3xl"
            />
            <div
                aria-hidden="true"
                className="pointer-events-none absolute right-[-8rem] bottom-[-10rem] h-[30rem] w-[30rem] rounded-full bg-sky-300/20 blur-3xl"
            />

            <div className="relative mx-auto grid max-w-7xl items-center gap-14 px-5 py-16 sm:px-8 lg:grid-cols-[1.05fr_0.95fr] lg:px-12 lg:py-24">
                <div className="max-w-2xl">
                    <span className="inline-flex items-center gap-2 rounded-full border border-brand-200/80 bg-white/70 px-4 py-2 text-sm font-semibold text-brand-800 shadow-sm backdrop-blur-xl">
                        <Sparkles size={16} className="text-brand-600" />
                        Smarter money. Better together.
                    </span>

                    <h1 className="mt-6 font-display text-4xl leading-[1.1] font-extrabold tracking-tight text-heading sm:text-5xl lg:text-6xl">
                        Money management that finally{' '}
                        <span className="text-gradient-animated">feels simple.</span>
                    </h1>

                    <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600">
                        Track your own spending, split bills with roommates and
                        friends, follow your monthly budget, and settle up in a
                        couple of taps — with AI quietly doing the categorizing
                        for you.
                    </p>

                    <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                        <Link
                            to="/register"
                            className="group inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-6 py-3 font-bold text-white shadow-button transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-600/25"
                        >
                            Start managing expenses
                            <ArrowRight
                                size={19}
                                className="transition-transform duration-300 group-hover:translate-x-1"
                            />
                        </Link>
                        <a
                            href="#features"
                            className="inline-flex min-h-12 items-center justify-center rounded-2xl border border-white/90 bg-white/70 px-6 py-3 font-semibold text-heading shadow-sm backdrop-blur-xl transition-all duration-300 hover:-translate-y-0.5 hover:bg-white hover:shadow-md"
                        >
                            Explore features
                        </a>
                    </div>

                    <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-sm font-medium text-slate-600">
                        {trustItems.map((item) => {
                            const Icon = item.icon
                            return (
                                <li key={item.label} className="inline-flex items-center gap-2">
                                    <span className="grid h-5 w-5 place-items-center rounded-full bg-brand-100 text-brand-700">
                                        <Icon size={13} strokeWidth={2.5} />
                                    </span>
                                    {item.label}
                                </li>
                            )
                        })}
                    </ul>
                </div>

                <div
                    ref={stageRef}
                    className="relative mx-auto w-full max-w-xl"
                    style={{ perspective: '1000px' }}
                >
                    <div
                        aria-hidden="true"
                        className="absolute inset-8 rounded-[3rem] bg-linear-to-br from-brand-400/35 to-sky-400/25 blur-3xl"
                    />

                    <div
                        className="relative rounded-[2.5rem] border border-white/80 bg-white/70 p-8 shadow-card backdrop-blur-2xl transition-transform duration-200 ease-out sm:p-10"
                        style={{ transform: 'rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg))' }}
                    >
                        <div className="relative mx-auto flex h-56 w-56 items-center justify-center sm:h-64 sm:w-64">
                            {heroImageFailed ? (
                                <WalletMascot className="h-full w-full animate-mascot-breathe" />
                            ) : (
                                <img
                                    src="/animations/expensemate-hero.webp"
                                    alt="Animated illustration of a person happily reviewing expenses on ExpenseMate, with a wallet mascot and floating coins"
                                    loading="eager"
                                    width="256"
                                    height="256"
                                    className="h-full w-full object-contain"
                                    onError={() => setHeroImageFailed(true)}
                                />
                            )}
                        </div>

                        <div className="absolute -top-6 -left-6 w-44 animate-float-a rounded-2xl border border-white/80 bg-white/90 p-3.5 shadow-lg shadow-brand-900/10 backdrop-blur-xl sm:-left-10">
                            <p className="text-[11px] font-semibold text-slate-500">Monthly budget</p>
                            <p className="mt-1 text-lg font-bold text-heading">
                                ₹18,400 <span className="text-xs font-semibold text-slate-400">/ ₹25,000</span>
                            </p>
                            <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-brand-100">
                                <div className="h-full w-[74%] rounded-full bg-linear-to-r from-brand-500 to-brand-400" />
                            </div>
                        </div>

                        <div className="absolute top-8 -right-6 w-40 animate-float-b rounded-2xl border border-white/80 bg-white/90 p-3.5 shadow-lg shadow-brand-900/10 backdrop-blur-xl sm:-right-10">
                            <p className="text-[11px] font-semibold text-slate-500">Roommates owe you</p>
                            <p className="mt-1 text-lg font-bold text-[#2F9E6F]">₹1,250</p>
                        </div>

                        <div className="absolute -bottom-7 left-1/2 flex w-48 -translate-x-1/2 animate-float-c items-center gap-2 rounded-2xl border border-white/80 bg-white/90 px-4 py-3 shadow-lg shadow-brand-900/10 backdrop-blur-xl">
                            <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-[#2F9E6F]/15 text-[#2F9E6F]">
                                <TrendingUp size={15} />
                            </span>
                            <p className="text-xs font-semibold text-heading">Expense settled with Aanya</p>
                        </div>

                        <span
                            aria-hidden="true"
                            className="absolute top-2 right-10 h-3 w-3 animate-coin-spin rounded-full bg-linear-to-br from-[#F0B94D] to-[#E9B949]"
                        />
                    </div>
                </div>
            </div>
        </section>
    )
}

export default HeroSection