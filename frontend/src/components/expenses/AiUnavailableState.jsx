import {
    AlertTriangle,
    RefreshCw,
} from 'lucide-react'

export function AiUnavailableState({
                                       message = 'AI suggestions are temporarily unavailable.',
                                       onRetry,
                                   }) {
    return (
        <div
            role="status"
            className="rounded-2xl border border-amber-200 bg-amber-50 p-4"
        >
            <div className="flex items-start gap-3">
                <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-amber-100 text-amber-700">
                    <AlertTriangle
                        size={18}
                        aria-hidden="true"
                    />
                </span>

                <div className="min-w-0 flex-1">
                    <p className="font-bold text-amber-900">
                        AI unavailable
                    </p>

                    <p className="mt-1 text-sm leading-6 text-amber-800">
                        {message} You can still select a
                        category manually and save the
                        expense.
                    </p>

                    {onRetry && (
                        <button
                            type="button"
                            onClick={onRetry}
                            className="mt-3 inline-flex items-center gap-2 text-sm font-bold text-amber-900 underline decoration-amber-400 underline-offset-4"
                        >
                            <RefreshCw
                                size={15}
                                aria-hidden="true"
                            />
                            Try suggestion again
                        </button>
                    )}
                </div>
            </div>
        </div>
    )
}