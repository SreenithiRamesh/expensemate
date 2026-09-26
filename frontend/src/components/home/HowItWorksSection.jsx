import { CircleDollarSign, SplitSquareHorizontal, Wallet } from 'lucide-react'
import { useInViewReveal } from './useInViewReveal.js'

const steps = [
    {
        icon: CircleDollarSign,
        title: 'Add an expense',
        description:
            'Type it in plain words or fill a quick form — AI fills the merchant, amount, and category for you to confirm.',
    },
    {
        icon: SplitSquareHorizontal,
        title: 'Split or categorize it',
        description:
            'Keep it personal, or split it across a group equally, by percentage, or by exact amounts.',
    },
    {
        icon: Wallet,
        title: 'Track balances and settle',
        description:
            'Watch who owes whom update live, then settle up with the fewest payments possible.',
    },
]

function HowItWorksSection() {
    const [ref, isVisible] = useInViewReveal()

    return (
        <section
            id="how-it-works"
            className="relative bg-linear-to-b from-transparent via-white/40 to-transparent py-20"
        >
            <div className="mx-auto max-w-7xl px-5 sm:px-8 lg:px-12">
                <div ref={ref} className={`reveal mx-auto max-w-2xl text-center ${isVisible ? 'is-visible' : ''}`}>
                    <h2 className="font-display text-3xl font-extrabold tracking-tight text-heading sm:text-4xl">
                        Three steps to a settled tab
                    </h2>
                    <p className="mt-4 text-lg text-slate-600">
                        No spreadsheets, no chasing people for money — ExpenseMate keeps
                        the whole cycle in one flow.
                    </p>
                </div>

                <ol className="relative mt-14 grid gap-10 sm:grid-cols-3">
                    <div
                        aria-hidden="true"
                        className="absolute top-8 right-[16.6%] left-[16.6%] hidden h-0.5 bg-linear-to-r from-brand-200 via-brand-400 to-brand-200 sm:block"
                    />
                    {steps.map((step, index) => {
                        const Icon = step.icon
                        return (
                            <li key={step.title} className="relative flex flex-col items-center text-center">
                                <div className="relative grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/25">
                                    <Icon size={26} />
                                    <span className="absolute -top-2 -right-2 grid h-6 w-6 place-items-center rounded-full bg-white text-xs font-bold text-brand-700 shadow-sm">
                                        {index + 1}
                                    </span>
                                </div>
                                <h3 className="mt-5 font-display text-lg font-bold text-heading">
                                    {step.title}
                                </h3>
                                <p className="mt-2 max-w-xs text-sm leading-6 text-slate-500">
                                    {step.description}
                                </p>
                            </li>
                        )
                    })}
                </ol>
            </div>
        </section>
    )
}

export default HowItWorksSection