import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Menu, WalletCards, X } from 'lucide-react'

const navLinks = [
    { label: 'Features', href: '#features' },
    { label: 'How it works', href: '#how-it-works' },
    { label: 'Security', href: '#security' },
]

function HomeHeader() {
    const [isMenuOpen, setIsMenuOpen] = useState(false)

    return (
        <header className="sticky top-0 z-40 border-b border-white/60 bg-white/70 backdrop-blur-xl">
            <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4 sm:px-8 lg:px-12">
                <Link
                    to="/"
                    aria-label="ExpenseMate home"
                    className="group flex items-center gap-3"
                >
                    <span className="grid h-10 w-10 place-items-center rounded-2xl bg-linear-to-br from-brand-500 via-brand-600 to-brand-800 text-white shadow-lg shadow-brand-600/25 transition-transform duration-300 group-hover:-rotate-6">
                        <WalletCards size={20} strokeWidth={2.2} />
                    </span>
                    <span className="font-display text-lg font-bold tracking-tight text-heading">
                        Expense<span className="text-brand-600">Mate</span>
                    </span>
                </Link>

                <nav aria-label="Primary" className="hidden items-center gap-8 md:flex">
                    {navLinks.map((link) => (
                        <a
                            key={link.href}
                            href={link.href}
                            className="text-sm font-semibold text-slate-600 transition hover:text-brand-700"
                        >
                            {link.label}
                        </a>
                    ))}
                </nav>

                <div className="hidden items-center gap-3 md:flex">
                    <Link
                        to="/login"
                        className="rounded-xl px-4 py-2 text-sm font-semibold text-heading transition hover:text-brand-700"
                    >
                        Log in
                    </Link>
                    <Link
                        to="/register"
                        className="inline-flex items-center justify-center rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-2.5 text-sm font-bold text-white shadow-button transition hover:-translate-y-0.5"
                    >
                        Get started
                    </Link>
                </div>

                <button
                    type="button"
                    onClick={() => setIsMenuOpen((open) => !open)}
                    aria-expanded={isMenuOpen}
                    aria-controls="mobile-nav"
                    aria-label={isMenuOpen ? 'Close menu' : 'Open menu'}
                    className="grid h-10 w-10 place-items-center rounded-xl border border-slate-200 bg-white/80 text-heading md:hidden"
                >
                    {isMenuOpen ? <X size={20} /> : <Menu size={20} />}
                </button>
            </div>

            {isMenuOpen ? (
                <nav
                    id="mobile-nav"
                    aria-label="Mobile"
                    className="flex flex-col gap-1 border-t border-white/60 bg-white/95 px-5 py-4 md:hidden"
                >
                    {navLinks.map((link) => (
                        <a
                            key={link.href}
                            href={link.href}
                            onClick={() => setIsMenuOpen(false)}
                            className="rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-600 hover:bg-brand-50 hover:text-brand-700"
                        >
                            {link.label}
                        </a>
                    ))}
                    <div className="mt-2 flex flex-col gap-2 border-t border-slate-100 pt-3">
                        <Link
                            to="/login"
                            onClick={() => setIsMenuOpen(false)}
                            className="rounded-xl px-3 py-2.5 text-center text-sm font-semibold text-heading hover:bg-brand-50"
                        >
                            Log in
                        </Link>
                        <Link
                            to="/register"
                            onClick={() => setIsMenuOpen(false)}
                            className="rounded-xl bg-linear-to-r from-brand-600 to-brand-500 px-3 py-2.5 text-center text-sm font-bold text-white shadow-button"
                        >
                            Get started
                        </Link>
                    </div>
                </nav>
            ) : null}
        </header>
    )
}

export default HomeHeader