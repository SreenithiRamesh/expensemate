import { Construction } from 'lucide-react'
import { useLocation } from 'react-router-dom'

function ComingSoonPage() {
    const location = useLocation()

    const title =
        location.pathname
            .split('/')
            .filter(Boolean)
            .at(-1)
            ?.replace('-', ' ') ||
        'Feature'

    return (
        <section className="grid min-h-[65vh] place-items-center">
            <div className="w-full max-w-lg rounded-3xl border border-white/80 bg-white/80 p-8 text-center shadow-sm backdrop-blur-xl">
                <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br from-brand-500 to-sky-500 text-white">
                    <Construction size={29} />
                </span>

                <h1 className="mt-6 text-3xl font-black capitalize text-heading">
                    {title}
                </h1>

                <p className="mt-3 text-slate-500">
                    This ExpenseMate module will be implemented in its scheduled frontend milestone.
                </p>
            </div>
        </section>
    )
}

export default ComingSoonPage