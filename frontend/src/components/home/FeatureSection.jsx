import { BrainCircuit, PiggyBank, Scale, Users } from 'lucide-react'
import { useInViewReveal } from './useInViewReveal.js'

const features = [
    {
        icon: PiggyBank,
        title: 'Personal expenses',
        description:
            'Log day-to-day spending in seconds and see exactly where your money is going, category by category.',
    },
    {
        icon: Users,
        title: 'Shared groups',
        description:
            'Create a group for a trip, flat, or friend circle, add expenses once, and split them equally, by percentage, or by exact amounts.',
    },
    {
        icon: Scale,
        title: 'Smart settlements',
        description:
            'ExpenseMate nets everyone\u2019s balances down to the fewest payments needed, so settling up never takes more than a few transfers.',
    },
    {
        icon: BrainCircuit,
        title: 'AI insights',
        description:
            'Type an expense in plain words and let AI fill in the merchant, amount, and category — you just confirm and save.',
    },
]

function FeatureSection() {
    const [ref, isVisible] = useInViewReveal()

    return (
        <section id="features" className="relative mx-auto max-w-7xl px-5 py-20 sm:px-8 lg:px-12">
            <div ref={ref} className={`reveal mx-auto max-w-2xl text-center ${isVisible ? 'is-visible' : ''}`}>
                <h2 className="font-display text-3xl font-extrabold tracking-tight text-heading sm:text-4xl">
                    Everything your money needs, in one place
                </h2>
                <p className="mt-4 text-lg text-slate-600">
                    Personal budgets and shared bills usually live in different apps.
                    ExpenseMate keeps them together, with AI doing the busywork.
                </p>
            </div>

            <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                {features.map((feature) => {
                    const Icon = feature.icon
                    return (
                        <article
                            key={feature.title}
                            className="glow-border group rounded-3xl border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur-xl transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl hover:shadow-brand-800/10"
                        >
                            <span className="grid h-12 w-12 place-items-center rounded-2xl bg-linear-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/20 transition-transform duration-300 group-hover:-rotate-6">
                                <Icon size={22} />
                            </span>
                            <h3 className="mt-5 font-display text-lg font-bold text-heading">
                                {feature.title}
                            </h3>
                            <p className="mt-2 text-sm leading-6 text-slate-500">
                                {feature.description}
                            </p>
                        </article>
                    )
                })}
            </div>
        </section>
    )
}

export default FeatureSection