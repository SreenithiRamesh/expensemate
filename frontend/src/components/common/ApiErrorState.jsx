import {
    AlertCircle,
    RefreshCw,
} from 'lucide-react'

import { Button } from './Button'

export function ApiErrorState({
                                  title = 'Unable to load this information',
                                  description = 'ExpenseMate could not complete the request. Please try again.',
                                  onRetry,
                              }) {
    return (
        <div
            role="alert"
            className="grid min-h-64 place-items-center rounded-3xl border border-rose-100 bg-linear-to-br from-white/80 to-rose-50/80 px-6 py-10 text-center"
        >
            <div className="max-w-sm">
                <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-linear-to-br from-rose-500 to-red-500 text-white shadow-lg shadow-rose-500/20">
                    <AlertCircle size={26} />
                </span>

                <h2 className="mt-5 text-xl font-bold text-heading">
                    {title}
                </h2>

                <p className="mt-2 leading-6 text-slate-500">
                    {description}
                </p>

                {onRetry && (
                    <Button
                        variant="secondary"
                        onClick={onRetry}
                        className="mt-6"
                    >
                        <RefreshCw size={17} />
                        Try again
                    </Button>
                )}
            </div>
        </div>
    )
}