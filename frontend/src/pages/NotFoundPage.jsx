import { ArrowLeft, SearchX } from 'lucide-react'
import { Link } from 'react-router-dom'

function NotFoundPage() {
    return (
        <main className="grid min-h-screen place-items-center px-5">
            <section className="w-full max-w-lg rounded-[2rem] border border-white/80 bg-white/80 p-8 text-center shadow-card backdrop-blur-xl">
                <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br from-brand-500 to-sky-500 text-white">
                    <SearchX size={30} />
                </span>

                <p className="mt-6 text-sm font-black tracking-[0.2em] text-brand-700 uppercase">
                    Error 404
                </p>

                <h1 className="mt-2 text-3xl font-black text-heading">
                    Page not found
                </h1>

                <p className="mt-3 text-slate-500">
                    The requested ExpenseMate page does not exist.
                </p>

                <Link
                    to="/"
                    className="mt-7 inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-6 py-3 font-bold text-white shadow-button"
                >
                    <ArrowLeft size={18} />
                    Return home
                </Link>
            </section>
        </main>
    )
}

export default NotFoundPage