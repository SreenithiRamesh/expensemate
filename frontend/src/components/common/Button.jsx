import { LoaderCircle } from 'lucide-react'

import { cn } from '../../utils/cn'

const variantClasses = {
    primary:
        'bg-linear-to-r from-brand-600 to-brand-500 text-white shadow-button hover:-translate-y-0.5 hover:from-brand-700 hover:to-brand-600',

    secondary:
        'border border-slate-200 bg-white text-heading shadow-sm hover:border-brand-200 hover:bg-brand-50',

    danger:
        'bg-linear-to-r from-rose-600 to-red-500 text-white shadow-lg shadow-rose-600/20 hover:-translate-y-0.5',

    ghost:
        'bg-transparent text-slate-600 hover:bg-brand-50 hover:text-brand-800',
}

const sizeClasses = {
    small: 'min-h-9 rounded-xl px-3 py-2 text-sm',
    medium: 'min-h-11 rounded-2xl px-5 py-2.5',
    large: 'min-h-12 rounded-2xl px-6 py-3 text-base',
}

export function Button({
                           children,
                           type = 'button',
                           variant = 'primary',
                           size = 'medium',
                           loading = false,
                           disabled = false,
                           className,
                           ...props
                       }) {
    const isDisabled =
        disabled || loading

    return (
        <button
            type={type}
            disabled={isDisabled}
            className={cn(
                'inline-flex items-center justify-center gap-2 font-bold transition-all duration-300 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-200 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0',
                variantClasses[variant],
                sizeClasses[size],
                className,
            )}
            {...props}
        >
            {loading && (
                <LoaderCircle
                    aria-hidden="true"
                    size={18}
                    className="animate-spin"
                />
            )}

            {children}
        </button>
    )
}