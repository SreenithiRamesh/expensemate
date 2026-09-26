import {
    ArrowLeft,
    LayoutDashboard,
    ShieldX,
} from 'lucide-react'
import { Link } from 'react-router-dom'

export default function UnauthorizedPage() {
    return (
        <main className="flex min-h-screen items-center justify-center px-5 py-12">
            <section className="w-full max-w-lg rounded-3xl border border-white/80 bg-white/90 p-8 text-center shadow-card backdrop-blur-xl sm:p-10">
                <div className="mx-auto grid h-20 w-20 place-items-center rounded-3xl bg-rose-50 text-rose-600">
                    <ShieldX
                        size={38}
                        aria-hidden="true"
                    />
                </div>

                <p className="mt-6 text-sm font-bold uppercase tracking-[0.18em] text-rose-600">
                    Access denied
                </p>

                <h1 className="mt-3 font-display text-3xl font-extrabold text-heading">
                    You cannot access this page
                </h1>

                <p className="mx-auto mt-4 max-w-md text-sm leading-6 text-slate-600">
                    Your account is signed in, but it does not have
                    permission to view the requested resource.
                </p>

                <div className="mt-7 flex flex-col justify-center gap-3 sm:flex-row">
                    <Link
                        to="/app/dashboard"
                        className="inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button"
                    >
                        <LayoutDashboard
                            size={18}
                            aria-hidden="true"
                        />

                        Dashboard
                    </Link>

                    <Link
                        to="/"
                        className="inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl border border-brand-200 bg-white px-5 py-3 font-bold text-brand-700"
                    >
                        <ArrowLeft
                            size={18}
                            aria-hidden="true"
                        />

                        Home
                    </Link>
                </div>
            </section>
        </main>
    )
}