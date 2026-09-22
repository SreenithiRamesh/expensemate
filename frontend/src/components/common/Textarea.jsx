import {
    forwardRef,
    useId,
} from 'react'

import { cn } from '../../utils/cn'

export const Textarea = forwardRef(
    function Textarea(
        {
            label,
            error,
            helperText,
            id,
            className,
            containerClassName,
            required = false,
            rows = 4,
            ...props
        },
        ref,
    ) {
        const generatedId = useId()

        const textareaId =
            id || generatedId

        const errorId =
            `${textareaId}-error`

        const helperId =
            `${textareaId}-helper`

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
                        htmlFor={textareaId}
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

                <textarea
                    ref={ref}
                    id={textareaId}
                    rows={rows}
                    required={required}
                    aria-invalid={
                        error ? 'true' : undefined
                    }
                    aria-describedby={describedBy}
                    className={cn(
                        'w-full resize-y rounded-2xl border bg-white/85 px-4 py-3 text-heading shadow-sm outline-none transition placeholder:text-slate-400',
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