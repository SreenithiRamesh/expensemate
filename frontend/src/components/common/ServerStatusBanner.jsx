import {
    LoaderCircle,
    RefreshCw,
    ServerOff,
} from 'lucide-react'

import { SERVER_HEALTH } from '../../features/server-health/serverHealth'

export function ServerStatusBanner({
                                       status,
                                       onRetry,
                                   }) {
    if (
        status === SERVER_HEALTH.AVAILABLE ||
        status === SERVER_HEALTH.CHECKING
    ) {
        return null
    }

    const isWaking =
        status === SERVER_HEALTH.WAKING

    return (
        <div
            role="status"
            className={`flex items-center justify-center gap-3 px-4 py-2.5 text-sm font-semibold ${
                isWaking
                    ? 'bg-linear-to-r from-amber-50 to-orange-50 text-amber-900'
                    : 'bg-linear-to-r from-rose-50 to-red-50 text-rose-800'
            }`}
        >
            {isWaking ? (
                <LoaderCircle
                    size={17}
                    className="animate-spin"
                />
            ) : (
                <ServerOff size={17} />
            )}

            <span>
                {isWaking
                    ? 'ExpenseMate is waking up. This can take a few seconds.'
                    : 'ExpenseMate backend is currently unavailable.'}
            </span>

            {!isWaking && (
                <button
                    type="button"
                    onClick={onRetry}
                    className="inline-flex items-center gap-1 underline underline-offset-4"
                >
                    <RefreshCw size={14} />
                    Retry
                </button>
            )}
        </div>
    )
}