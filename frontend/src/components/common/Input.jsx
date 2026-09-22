import {
    forwardRef,
    useId,
} from 'react'

import { cn } from '../../utils/cn'

export const Input = forwardRef(
    function Input(
        {
            label,
            error,
            helperText,
            id,
            className,
            containerClassName,
            required = false,
            ...props
        },
        ref,
    ) {
        const generatedId = useId()

        const inputId =
            id || generatedId

        const errorId =
            `${inputId}-error`

        const helperId =
            `${inputId}-helper`

        const describedBy = [
            error ? errorId : null,
            helperText ? helperId : null,
        ]
            .filter(Boolean)
            .join(' ') || undefined

        return (
            <div
                className={cn(
                    'w-full',
                    containerClassName,
                )}
            >
                {label && (
                    <label
                        htmlFor={inputId}
                        className="mb-2 block text-sm font-bold text-heading"
                    >
                        {label}

                        {required && (
                            <span
                                aria-hidden="true"
                                className="ml-1 text-rose-500"
                            >
                                *
                            </span>
                        )}
                    </label>
                )}

                <input
                    ref={ref}
                    id={inputId}
                    required={required}
                    aria-invalid={
                        error ? 'true' : undefined
                    }
                    aria-describedby={describedBy}
                    className={cn(
                        'min-h-12 w-full rounded-2xl border bg-white/85 px-4 text-heading shadow-sm outline-none transition placeholder:text-slate-400',
                        'focus:border-brand-400 focus:ring-4 focus:ring-brand-100',
                        'disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500',
                        error
                            ? 'border-rose-400 focus:border-rose-400 focus:ring-rose-100'
                            : 'border-slate-200',
                        className,
                    )}
                    {...props}
                />

                {helperText && (
                    <p
                        id={helperId}
                        className="mt-2 text-sm text-slate-500"
                    >
                        {helperText}
                    </p>
                )}

                {error && (
                    <p
                        id={errorId}
                        role="alert"
                        className="mt-2 text-sm font-semibold text-rose-600"
                    >
                        {error}
                    </p>
                )}
            </div>
        )
    },
)