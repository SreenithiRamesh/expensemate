import { Component } from 'react'
import { AlertTriangle, Home, RefreshCw } from 'lucide-react'

class ErrorBoundary extends Component {
    constructor(props) {
        super(props)

        this.state = {
            hasError: false,
        }
    }

    static getDerivedStateFromError() {
        return {
            hasError: true,
        }
    }

    componentDidCatch(error, errorInfo) {
        if (import.meta.env.DEV) {
            console.error(
                'ExpenseMate render error:',
                error,
                errorInfo,
            )
        }
    }

    handleRetry = () => {
        this.setState({
            hasError: false,
        })
    }

    handleHome = () => {
        window.location.assign('/')
    }

    render() {
        if (!this.state.hasError) {
            return this.props.children
        }

        return (
            <main className="relative grid min-h-screen place-items-center overflow-hidden px-5 py-12">
                <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -top-32 -left-28 h-80 w-80 rounded-full bg-brand-300/30 blur-3xl"
                />

                <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -right-28 -bottom-32 h-96 w-96 rounded-full bg-sky-300/20 blur-3xl"
                />

                <section className="relative w-full max-w-lg rounded-[2rem] border border-white/80 bg-white/80 p-7 text-center shadow-card backdrop-blur-2xl sm:p-10">
                    <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br from-amber-400 to-orange-500 text-white shadow-lg shadow-orange-500/20">
                        <AlertTriangle size={30} />
                    </span>

                    <p className="mt-6 text-sm font-bold tracking-[0.2em] text-brand-700 uppercase">
                        Unexpected error
                    </p>

                    <h1 className="mt-2 text-3xl font-black tracking-tight text-heading">
                        ExpenseMate hit a temporary problem
                    </h1>

                    <p className="mt-4 leading-7 text-slate-600">
                        Your financial data has not been changed. Retry the
                        screen or return to the home page.
                    </p>

                    <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
                        <button
                            type="button"
                            onClick={this.handleRetry}
                            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl bg-linear-to-r from-brand-600 to-brand-500 px-5 py-3 font-bold text-white shadow-button transition hover:-translate-y-0.5"
                        >
                            <RefreshCw size={18} />
                            Try again
                        </button>

                        <button
                            type="button"
                            onClick={this.handleHome}
                            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-5 py-3 font-bold text-heading transition hover:bg-slate-50"
                        >
                            <Home size={18} />
                            Go home
                        </button>
                    </div>
                </section>
            </main>
        )
    }
}

export default ErrorBoundary