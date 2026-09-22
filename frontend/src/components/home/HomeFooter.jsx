import { Link } from 'react-router-dom'
import { WalletCards } from 'lucide-react'

const productLinks = [
    { label: 'Features', href: '#features' },
    { label: 'How it works', href: '#how-it-works' },
    { label: 'Security', href: '#security' },
]

const devLinks = [
    { label: 'GitHub', href: 'https://github.com' },
    { label: 'API documentation', href: '#' },
]

function HomeFooter() {
    return (
        <footer className="border-t border-white/70 bg-white/50">
            <div className="mx-auto max-w-7xl px-5 py-12 sm:px-8 lg:px-12">
                <div className="grid gap-10 sm:grid-cols-3">
                    <div>
                        <Link to="/" className="flex items-center gap-2.5">
                            <span className="grid h-9 w-9 place-items-center rounded-xl bg-linear-to-br from-brand-500 to-brand-700 text-white">
                                <WalletCards size={18} />
                            </span>
                            <span className="font-display text-lg font-bold text-heading">
                                Expense<span className="text-brand-600">Mate</span>
                            </span>
                        </Link>
                        <p className="mt-3 max-w-xs text-sm text-slate-500">
                            Personal and shared expense management, with AI doing the
                            tedious parts.
                        </p>
                    </div>

                    <div>
                        <p className="text-sm font-bold text-heading">Product</p>
                        <ul className="mt-3 space-y-2.5">
                            {productLinks.map((link) => (
                                <li key={link.label}>
                                    <a
                                        href={link.href}
                                        className="text-sm text-slate-500 transition hover:text-brand-700"
                                    >
                                        {link.label}
                                    </a>
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div>
                        <p className="text-sm font-bold text-heading">Developers</p>
                        <ul className="mt-3 space-y-2.5">
                            {devLinks.map((link) => (
                                <li key={link.label}>
                                    <a
                                        href={link.href}
                                        className="text-sm text-slate-500 transition hover:text-brand-700"
                                    >
                                        {link.label}
                                    </a>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>

                <div className="mt-10 flex flex-col gap-2 border-t border-slate-100 pt-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
                    <p>&copy; {new Date().getFullYear()} ExpenseMate. All rights reserved.</p>
                    <p>React · Spring Boot · PostgreSQL</p>
                </div>
            </div>
        </footer>
    )
}

export default HomeFooter