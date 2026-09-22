import { LoaderCircle } from 'lucide-react'

export function PageLoader({
                               message = 'Loading ExpenseMate...',
                           }) {
    return (
        <div
            role="status"
            aria-live="polite"
            className="grid min-h-[55vh] place-items-center px-5"
        >
            <div className="text-center">
                <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br from-brand-600 to-brand-500 text-white shadow-button">
                    <LoaderCircle
                        size={29}
                        className="animate-spin"
                    />
                </span>

                <p className="mt-5 font-bold text-heading">
                    {message}
                </p>

                <p className="mt-1 text-sm text-slate-500">
                    Please wait a moment.
                </p>
            </div>
        </div>
    )
}